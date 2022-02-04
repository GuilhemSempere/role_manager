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
package fr.cirad.security.base;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fr.cirad.manager.dump.DumpMetadata;
import fr.cirad.manager.dump.DumpValidity;
import fr.cirad.manager.dump.IBackgroundProcess;

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
	 * @return for each module, a map containing entity id as key, entity label as value
	 */
	Map<String, Map<Comparable, String>> getEntitiesByModule(String entityType, Boolean fTrueForPublicFalseForPrivateNullForAny);
	
	/**
	 * @param sModule
	 * @return whether or not the module is hidden (should not be listed by default)
	 */
	boolean isModuleHidden(String sModule);
	
	/**
	 * @param sModule
	 * @param fAlsoDropDatabase
	 * @return whether or not module removal succeeded
	 * @throws IOException 
	 */
	boolean removeDataSource(String sModule, boolean fAlsoDropDatabase) throws IOException;
	
	/**
	 * @param sModule
	 * @param fPublic
	 * @param fHidden
	 * @param ncbiTaxonIdNameAndSpecies
	 * @return whether or not module update succeeded
	 * @throws Exception
	 */
	boolean updateDataSource(String sModule, boolean fPublic, boolean fHidden, String ncbiTaxonIdNameAndSpecies) throws Exception;
	
	/**
	 * @param sModule
	 * @param sHost
	 * @param ncbiTaxonIdNameAndSpecies
	 * @param expiryDate
	 * @return whether or not module creation succeeded
	 * @throws Exception
	 */
	boolean createDataSource(String sModule, String sHost, String ncbiTaxonIdNameAndSpecies, Long expiryDate) throws Exception;
	
	/**
	 * @param sModule
	 * @param sEntityType
	 * @param entityId
	 * @return whether or not entity removal succeeded
	 * @throws Exception
	 */
	boolean removeManagedEntity(String sModule, String sEntityType, Comparable entityId) throws Exception;
	
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
	 * @return A string giving instructions for enabling dumps (empty string if already enabled)
	 */
	String getActionRequiredToEnableDumps();
	
	/**
	 * @param sModule Module to get the dumps of
	 * @return List of existing dumps for the given module
	 */
	List<DumpMetadata> getDumps(String sModule);
	
	/**
	 * @param sModule Module to get the dump status of
	 * @return The dump status of the module (VALID = There is an up to date dump, OUTDATED = All dumps are outdated, NONE = No existing dump)
	 */
	DumpValidity getDumpStatus(String sModule);
	
	/**
	 * @param sModule Module to dump
	 * @param sName Name of the new dump
	 * @param sDescription Description of the new dump
	 * @return Started dump process
	 */
	IBackgroundProcess startDump(String sModule, String sName, String sDescription);
	
	/**
	 * @param sModule Module to dump
	 * @param sDump Dump to restore
	 * @param drop True to drop the database before restoring the dump
	 * @return Started restore process
	 */
	IBackgroundProcess startRestore(String sModule, String sDump, boolean drop);
	
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
}
