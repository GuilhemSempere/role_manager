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

public interface IRoleDefinition {

	static final public String ROLE_ADMIN = "ROLE_ADMIN";
	static final public String ROLE_DB_SUPERVISOR = "SUPERVISOR";
    @Deprecated static final public String CREATOR_ROLE_SUFFIX = "CREATOR"; // this is now deprecated in favor of ROLE_SUPERVISOR
	static final public String TOPLEVEL_ROLE_PREFIX = "USER";
	static final public String ENTITY_MANAGER_ROLE = "MANAGER";
	public static final String DUMMY_EMPTY_ROLE = "DUMMY_EMPTY_ROLE";	// Spring-security does not like users with no role
	
}