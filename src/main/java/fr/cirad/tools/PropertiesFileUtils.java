/*******************************************************************************
 * Role Manager - Generic web tool for managing user roles using Spring Security
 * Copyright (C) 2018, <CIRAD>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License, version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * See <http://www.gnu.org/licenses/agpl.html> for details about GNU General
 * Public License V3.
 *******************************************************************************/
package fr.cirad.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Writes properties files in a way that never leaves the target truncated or partially written,
 * e.g. when the disk is full: contents are first written and synced to a temporary file in the same
 * directory, which is then atomically renamed onto the target. A copy of the previous version is kept
 * as <target>.bak.
 */
public class PropertiesFileUtils {

    private static final Logger LOG = Logger.getLogger(PropertiesFileUtils.class);

    public static final String BACKUP_SUFFIX = ".bak";

    /**
     * @param props the properties to write
     * @param target the file to write
     * @param comment passed to Properties.store()
     * @param charset if null, Properties.store(OutputStream) is used (ISO-8859-1 with unicode escapes), otherwise Properties.store(Writer) with this charset
     */
    public static void storeAtomically(Properties props, File target, String comment, Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (charset == null)
            props.store(baos, comment);
        else {
            OutputStreamWriter writer = new OutputStreamWriter(baos, charset);
            props.store(writer, comment);
            writer.flush();
        }
        writeAtomically(target, baos.toByteArray());
    }

    public static void writeAtomically(File target, byte[] contents) throws IOException {
        Path dest = target.toPath();
        if (Files.exists(dest))
            dest = dest.toRealPath();	// follow symlinks so that we replace the actual file rather than the link

        Path tmp = Files.createTempFile(dest.getParent(), dest.getFileName().toString() + ".", ".tmp");
        try {
            try (FileOutputStream fos = new FileOutputStream(tmp.toFile())) {
                fos.write(contents);
                fos.flush();
                fos.getFD().sync();	// any lack of disk space is reported at this point at the latest, before the target is touched
            }

            if (Files.exists(dest)) {
                copyPermissions(dest, tmp);
                backup(dest);
            }

            try {
                Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException moveException) {	// e.g. target is a single-file bind mount (Docker), which cannot be replaced by renaming
                LOG.warn("Unable to atomically replace " + dest + " (" + moveException + "), overwriting it in place instead");
                Files.deleteIfExists(tmp);	// free up its disk space first
                Files.write(dest, contents, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        }
        finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Keeps a copy of the current file as <file>.bak, making sure an existing backup is never replaced by an incomplete one */
    private static void backup(Path file) {
        Path backup = file.resolveSibling(file.getFileName().toString() + BACKUP_SUFFIX);
        Path tmpBackup = null;
        try {
            if (Files.size(file) == 0)
                return;	// we don't want to replace a possibly valid backup with an empty file

            tmpBackup = Files.createTempFile(file.getParent(), backup.getFileName().toString() + ".", ".tmp");
            Files.copy(file, tmpBackup, StandardCopyOption.REPLACE_EXISTING);
            copyPermissions(file, tmpBackup);
            Files.move(tmpBackup, backup, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            LOG.warn("Unable to back up " + file + " to " + backup, e);
        }
        finally {
            if (tmpBackup != null)
                try {
                    Files.deleteIfExists(tmpBackup);
                }
                catch (IOException ignored) {}
        }
    }

    /** Temp files are created with restrictive permissions, so we give them those of the file they are replacing */
    private static void copyPermissions(Path from, Path to) {
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(from);
            Files.setPosixFilePermissions(to, perms);
        }
        catch (UnsupportedOperationException | IOException ignored) {}	// non-POSIX file system
    }
}
