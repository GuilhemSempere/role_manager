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
package fr.cirad.manager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import fr.cirad.manager.dump.DumpMetadata;
import fr.cirad.manager.dump.DumpStatus;

/**
 * @author sempere
 * Interface to implement for a webapp to be able to use the role manager add-on
 */
public interface IModuleManager {
	/**
	 * @return collection of db host names available in the system
	 */
	Collection<String> getHosts();

	/**
	 * @param fTrueForPublicFalseForPrivateNullForBoth
	 * @return collection of modules (i.e. databases) declared in the system
	 */
	Collection<String> getModules(Boolean fTrueForPublicFalseForPrivateNullForBoth);

	/**
	 * @param entityType
	 * @param fTrueForPublicFalseForPrivateNullForAny
	 * @param modules collection of modules to limit the query to (all will be accounted for if null)
	 * @return for each module, a map containing entity id as key, and as value a String array, of size 1 (entity label) or 2 (entity desription)
	 * @throws Exception 
	 */
	Map<String, Map<Comparable, String[]>> getEntitiesByModule(String entityType, Boolean fTrueForPublicFalseForPrivateNullForAny, Collection<String> modules, boolean fIncludeEntityDescriptions) throws Exception;

	/**
	 * @param sModule
	 * @return whether or not the module is hidden (should not be listed by default)
	 */
	boolean isModuleHidden(String sModule);

	/**
	 * @param sModule
	 * @param fAlsoDropDatabase
	 * @param fRemoveDumps 
	 * @return whether or not module removal succeeded
	 * @throws IOException
	 */
	boolean removeDataSource(String sModule, boolean fAlsoDropDatabase, boolean fRemoveDumps) throws IOException;

	/**
	 * @param sModule
	 * @param fPublic
	 * @param fHidden
	 * @param datasourceCategory
	 * @return whether or not module update succeeded
	 * @throws Exception
	 */
	boolean updateDataSource(String sModule, boolean fPublic, boolean fHidden, String datasourceCategory) throws Exception;

	/**
	 * @param sModule
	 * @param sHost
	 * @param expiryDate
	 * @return whether or not module creation succeeded
	 * @throws Exception
	 */
	boolean createDataSource(String sModule, String sHost, Long expiryDate) throws Exception;

	/**
	 * A single entity is to be removed here. The collection of IDs is for dealing with nested sub-entities (provide ID of each parent entity, ending with the target)
	 * 
	 * @param sModule
	 * @param sEntityType
	 * @param entityIDs
	 * @return whether or not entity removal succeeded
	 * @throws Exception
	 */
	boolean removeManagedEntity(String sModule, String sEntityType, Collection<Comparable> entityIDs) throws Exception;

	/**
	 * @param sModule
	 * @param sEntityType
	 * @param entityId
	 * @return whether or not entity exists in module
	 */
	boolean doesEntityExistInModule(String sModule, String sEntityType, Comparable entityId);

	/**
	 * @param sModule
	 * @param sEntityType
	 * @return whether or not entities of the given type may be declared public/private in this module
	 */
	boolean doesEntityTypeSupportVisibility(String sModule, String sEntityType);

	/**
	 * @param sEntityType
	 * @return URL of entity management page (null if none)
	 */
	String getEntityAdditionURL(String sEntityType);
	
	/**
	 * @param sEntityType
	 * @return URL of entity addition page (null if none)
	 */
	String getEntityEditionURL(String sEntityType);
	
	/**
	 * @param sEntityType
	 * @return whether or not entities of the given type have a textual description field
	 */
	boolean isInlineDescriptionUpdateSupportedForEntity(String sEntityType);

	/**
	 * @param sModule
	 * @param sEntityType
	 * @param entityId
	 * @param fPublic
	 * @return whether or not setting entity visibility succeeded
	 * @throws Exception
	 */
	boolean setManagedEntityVisibility(String sModule, String sEntityType, Comparable entityId, boolean fPublic) throws Exception;

	/**
	 * @param sModule
	 * @return name of the host this module's data is stored on
	 */
	String getModuleHost(String sModule);
	
	/**
	 * @param sModule
	 * @return the module's category String
	 */
	String getModuleCategory(String module);

	/**
	 * @return A string giving instructions for enabling dumps (empty string if already enabled)
	 * @throws InterruptedException 
	 */
	String getActionRequiredToEnableDumps();

	/**
	 * @param sModule Module to get the dumps of
	 * @return List of existing dumps for the given module
	 */
	List<DumpMetadata> getDumps(String sModule);

	/**
	 * @param sModule Module to get the dump status of
	 * @return The dump status of the module (VALID = There is an up to date dump, OUTDATED = All dumps are outdated, BUSY = Can't do anything right now, NONE = No existing dump)
	 */
	DumpStatus getDumpStatus(String sModule);

	/**
	 * @param sModule Module to dump
	 * @param sName Name of the new dump
	 * @param sDescription Description of the new dump
	 * @return Started dump process
	 */
	AbstractProcess startDump(String sModule, String sName, String sDescription);

	/**
	 * @param sModule Module to dump
	 * @param sDump Dump to restore
	 * @param drop True to drop the database before restoring the dump
	 * @return Started restore process
	 */
	AbstractProcess startRestore(String sModule, String sDump, boolean drop);

	/**
	 * @param sModule Module to check
	 * @return Whether or not a dump process can be started on the module
	 */
	boolean isModuleAvailableForDump(String sModule);

	/**
	 * @param sModule Module the dump belongs to
	 * @param sDump Name of the dump to delete
	 * @return True if the deletion succeeded, false otherwise
	 */
	boolean deleteDump(String sModule, String sDump);

	/**
     * @param sModule Module the dump belongs to
     * @return DB's storage size in bytes
     */
	long getModuleSize(String sModule);

    /**
     * @param sModule Module the dump belongs to
     * @param sDumpName the dump name
     * @return an InputStream pointing to the dump file
     * @throws FileNotFoundException 
     */
    InputStream getDumpInputStream(String sModule, String sDumpName) throws FileNotFoundException;
    
	/**
	 * @param sModule Module the dump belongs to
	 * @param sDumpName the dump name
	 * @return an InputStream pointing to the dump's logfile
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 */
    InputStream getDumpLogInputStream(String sModule, String sDumpName) throws IOException;
    
    /**
	 * @param sModule Module to update modification date for
	 * @param lastModification the date to set
	 * @param restored flag telling whether this modification is a restore
     * @param sModule
     */
    void updateDatabaseLastModification(String sModule, Date lastModification, boolean restored);

	void lockModuleForWriting(String module);

	void unlockModuleForWriting(String module);

	Map<Comparable, String> getSubEntities(String entityType, String sModule, Comparable[] parentEntityIDs) throws Exception;

	/**
	 * @param sModule
	 * @param entityType (sub-entities must be prefixed with "<parentEntityType>.")
	 * @param entityIDs array with IDs leading to the targeted entity (number of cells must match the entity level)
	 * @return
	 * @throws Exception 
	 */
	String managedEntityInfo(String sModule, String entityType, Collection<Comparable> entityIDs) throws Exception;

	/**
	 * @param sModule
	 * @param sEntityType
	 * @param sEntityId
	 * @param desc
	 * @return whether setting entity description succeeded
	 * @throws Exception
	 */
	boolean setManagedEntityDescription(String sModule, String sEntityType, String sEntityId, String desc) throws Exception;

	Map<String, AbstractProcess> getImportProcesses();

	void cleanupDb(String sModule);

	Collection<? extends String> getLevel1Roles(String level1Type, ResourceBundle bundle);
}
