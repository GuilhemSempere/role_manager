package fr.cirad.manager.dump;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import fr.cirad.manager.AbstractProcess;
import fr.cirad.manager.IModuleManager;
import fr.cirad.manager.ProcessStatus;

public class DumpProcess extends AbstractProcess {
    
    private static final Logger LOG = Logger.getLogger(DumpProcess.class);
    
    public static final String dumpManagementPath = "WEB-INF" + File.separator + "dump_management";
    public static String operatingSystem = System.getProperty("os.name").toLowerCase();
    private static String scriptExtension = operatingSystem.startsWith("win") ? "bat" : (operatingSystem.startsWith("mac") ? "command" : "sh");
    private static final String dumpCommand = dumpManagementPath + File.separator + "dbDump." + scriptExtension;
    private static final String restoreCommand = dumpManagementPath + File.separator + "dbRestore." + scriptExtension;

	private String module;
	private String dbName;
	private List<String> hosts;
	private Process subprocess = null;
	private String basePath;
	private String outPath;
	private boolean abortable = false;
	private boolean deleteOnError;
	private String abortWarning;

	private StringBuilder log;
	private String logFile;
	private ProcessStatus status;
	private String statusMessage;
	private boolean aborted = false;
	
	private IModuleManager moduleManager;

	public DumpProcess(IModuleManager moduleManager, String module, String dbName, List<String> hosts, String basePath, String outPath) {
		this.moduleManager = moduleManager;
		this.module = module;
		this.hosts = hosts;
		this.dbName = dbName;
		this.basePath = basePath;
		this.outPath = outPath;

		new File(this.outPath).mkdirs();

		this.log = new StringBuilder();
		this.status = ProcessStatus.IDLE;
	}

	public void startDump(String fileName, String credentials) {
		abortable = true;
		deleteOnError = true;
		abortWarning = null;
		logFile = outPath + File.separator + fileName + "__dump.log";
		(new Thread() {
			public void run() {
				File scriptFile = new File(basePath + dumpCommand);
				scriptFile.setReadable(true);
				scriptFile.setExecutable(true);

				String hostString = String.join(",", hosts);
				List<String> args = new ArrayList<String>(Arrays.asList(
					basePath + dumpCommand,
					"--host", hostString,
					"--output", outPath,
					"--database", dbName,
					"--name", fileName,
					"--log", logFile
				));

				String password = null;
				if (credentials != null) {
					int nLastAtSignPos = credentials.lastIndexOf("@");
					String[] userAndPass = credentials.substring(0, nLastAtSignPos).split(":");
					password = userAndPass[1];
					args.add("--username");
					args.add(userAndPass[0]);
					args.add("--authenticationDatabase");
					args.add(credentials.substring(1 + nLastAtSignPos));
                    args.add("--passwordPrompt");
				}

				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);

				runProcess(builder, password, null);
			}
		}).start();
	}

	public void startRestore(String dumpFile, boolean drop, String credentials) {
		abortable = true;
		deleteOnError = false;
		abortWarning = "This database may be left in an unstable state if you proceed.";
		logFile = dumpFile.substring(0, dumpFile.indexOf(".gz")) + "__restore-" + DateTimeFormatter.ofPattern("uuuuMMddHHmmss").format(LocalDateTime.now()) + ".log";
		(new Thread() {
			public void run() {
				File scriptFile = new File(basePath + restoreCommand);
				scriptFile.setReadable(true);
				scriptFile.setExecutable(true);

				String hostString = String.join(",", hosts);
				List<String> args = new ArrayList<String>(Arrays.asList(
					basePath + restoreCommand,
                    "--nsFrom", "\"" + dumpFile.substring(dumpFile.lastIndexOf(File.separator) + 1).split("__")[0] + ".*\"",
                    "--nsTo", "\"" + dbName + ".*\"",
					"--host", hostString,
					"--input", dumpFile,
					"--log", logFile
				));

				String password = null;
				if (drop) args.add("--drop");
				if (credentials != null) {
					int nLastAtSignPos = credentials.lastIndexOf("@");
					String[] userAndPass = credentials.substring(0, nLastAtSignPos).split(":");
					args.add("--username");
					args.add(userAndPass[0]);
					args.add("--authenticationDatabase");
					args.add(credentials.substring(1 + nLastAtSignPos));
					args.add("--passwordPrompt");
					password = userAndPass[1];
				}

				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);

				runProcess(builder, password, dumpFile);
			}
		}).start();
	}
	
	
	public static InputStream getLogInputStream(String logFile) throws IOException {
        File plainLogFile = new File(logFile);
        File gzippedLogFile = new File(logFile + ".gz");
        InputStream logInputStream = null;
        if (gzippedLogFile.exists())
            logInputStream = new GZIPInputStream(new FileInputStream(gzippedLogFile));
        else if (plainLogFile.exists()) {   // Windows script does not gzip logfiles: let's do it now
        	
        	// Windows logfile encoding is (at least sometimes) UTF-16 and we need to specify that in order to be able to read the stream correctly
        	Charset charset = detectLogCharset(plainLogFile);
        	LOG.debug("Compressing logfile by reading it as " + charset.displayName());
        	
            // re-encode as UTF-8 since that is what getLog() decodes with
            try (	Reader reader = new InputStreamReader(new FileInputStream(plainLogFile), charset);
            		Writer writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(gzippedLogFile)), StandardCharsets.UTF_8)) {
                char[] chars = new char[8192];
                int n;
                while ((n = reader.read(chars)) != -1)
                	writer.write(chars, 0, n);
            }
        	
        	if (gzippedLogFile.exists() && gzippedLogFile.length() > 0)
        		plainLogFile.delete();
            logInputStream = new GZIPInputStream(new FileInputStream(gzippedLogFile));
        }
        else
            throw new FileNotFoundException("Could find neither gzipped nor plain log file: " + logFile);
        return logInputStream;
	}

	/**
	 * Works out a plain logfile's encoding from its first bytes: a BOM if there is one,
	 * otherwise the position of NUL bytes, which only show up in UTF-16 text (logs being mostly ASCII).
	 */
	private static Charset detectLogCharset(File logFile) throws IOException {
		byte[] start;
		try (InputStream is = new FileInputStream(logFile)) {
			start = is.readNBytes(512);
		}

		if (start.length >= 2 && (((start[0] & 0xFF) == 0xFF && (start[1] & 0xFF) == 0xFE) || ((start[0] & 0xFF) == 0xFE && (start[1] & 0xFF) == 0xFF)))
			return StandardCharsets.UTF_16;  // this one reads the BOM to get the byte order, and leaves it out of the decoded text

		int nulsAtEvenPositions = 0, nulsAtOddPositions = 0;
		for (int i = 0; i < start.length; i++)
			if (start[i] == 0) {
				if (i % 2 == 0)
					nulsAtEvenPositions++;
				else
					nulsAtOddPositions++;
			}

		if (nulsAtOddPositions > nulsAtEvenPositions)
			return StandardCharsets.UTF_16LE;
		if (nulsAtEvenPositions > nulsAtOddPositions)
			return StandardCharsets.UTF_16BE;
		return StandardCharsets.UTF_8;
	}


	public String getLog(){
		if (this.log != null) {
			this.updateLog();
			return this.log.toString();
		} else {
			InputStream logIS = null;
            try {
			    logIS = getLogInputStream(logFile);
				int readLength;
				ByteArrayOutputStream logBuilder = new ByteArrayOutputStream();
				byte[] buffer = new byte[65536];  // FIXME ?
				while ((readLength = logIS.read(buffer)) != -1)
					logBuilder.write(buffer, 0, readLength);

				return logBuilder.toString("UTF-8");
			} catch (FileNotFoundException e) {
				return "Log file not found";
			} catch (IOException e) {
				return "Error reading the log file";
			} finally {
				try {
					if (logIS != null)
						logIS.close();
				} catch (Throwable t) {
					LOG.error(t);
				}
			}
		}
	}

	private void updateLog() {
		if (this.log != null && this.subprocess != null) {
			try {
				InputStream stream = this.subprocess.getInputStream();
				int length = stream.available();
				for (int i = 0; i < length; i++) {
					this.log.append((char)stream.read());
				}
			} catch (IOException e) {
				// Most likely, stream closed. Just return the last known state.
			}
		}
	}

	public String getModule() {
		return module;
	}

	public ProcessStatus getStatus() {
		return this.status;
	}

	public String getStatusMessage() {
		return this.statusMessage;
	}

	public boolean isAbortable() {
		return this.abortable;
	}

	public String getAbortWarning() {
		return this.abortWarning;
	}

	public void abort() {
		if (this.abortable) {
			this.aborted = true;
			this.subprocess.destroy();
		}
	}


	private void runProcess(ProcessBuilder builder, String password, String dumpFile) {
		moduleManager.lockModuleForWriting(module);
		try {
			status = ProcessStatus.RUNNING;
			statusMessage = "Running " + String.join(" ", builder.command());
			subprocess = builder.start();
			if (password != null) {
				OutputStream stdin = subprocess.getOutputStream();
				stdin.write((password + "\n").getBytes("utf-8"));
				stdin.flush();
			}

			int exitcode = subprocess.waitFor();
			if (exitcode == 0) {
				status = ProcessStatus.SUCCESS;
				statusMessage = "Finished";
				if (dumpFile != null) {
					moduleManager.updateDatabaseLastModification(this.module,
							Date.from(Files.readAttributes(new File(dumpFile).toPath(), BasicFileAttributes.class).creationTime().toInstant()),
							true);
				}
				this.log = null;  // Eventually deletes the log, from now on it will be read from disk
			} else if (aborted) {
				status = ProcessStatus.INTERRUPTED;
				statusMessage = "Aborted by user";
				onError();
			} else {
				status = ProcessStatus.ERROR;
				statusMessage = "Process exited with code " + exitcode;
				onError();
			}
		} catch (IOException e) {
			status = ProcessStatus.ERROR;
			statusMessage = "Error : " + e.toString();
		} catch (SecurityException e) {
			status = ProcessStatus.ERROR;
			statusMessage = "Security error : " + e.toString();
		} catch (InterruptedException e) {
			status = ProcessStatus.INTERRUPTED;
			statusMessage = "Interrupted : " + e.toString();
		} finally {
			moduleManager.unlockModuleForWriting(module);
		}
	}

	private void onError() {
		if (deleteOnError) {
			updateLog();
			for (String line : this.log.toString().split("\n")) {
				if (line.startsWith("Name : ")) {
					String dumpName = line.split(":")[1].trim();

					for (File file : new File(this.outPath).listFiles()) {
						String filename = file.getName();
						if (filename.startsWith(dumpName) && (filename.endsWith(".log") || filename.endsWith(".gz") || filename.endsWith(".txt")))
							file.delete();
					}
				}
			}
		}
	}
}
