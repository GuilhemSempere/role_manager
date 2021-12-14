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
package fr.cirad.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import fr.cirad.security.base.IModuleManager;
import fr.cirad.security.base.IRoleDefinition;
import fr.cirad.web.controller.security.UserPermissionController;

@Component
public class ReloadableInMemoryDaoImpl implements UserDetailsService {

    private static final Logger LOG = Logger.getLogger(ReloadableInMemoryDaoImpl.class);

    @Autowired
    private IModuleManager moduleManager;
    
    @Autowired
    private PasswordEncoder passwordEncoder;	// this may be either a CustomBCryptPasswordEncoder or a NoOpPasswordEncoder

    private File m_resourceFile;
    private Properties m_props = null;
    private HashMap<String, UserWithMethod> m_users;
    
    public ReloadableInMemoryDaoImpl() {
    	m_users = new HashMap<String, UserWithMethod>();
    }

    public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadUserByUsernameAndMethod(username, "");
    }
	
	/** Get a user by username from a specific authentication method
	 * Also load properties if they are not already loaded
	 * @param username Username to load
	 * @param method Authentication method to consider. Default is the empty string, null matches any method.
	 * @return The requested user
	 * @throws UsernameNotFoundException if the user is not found or does not correspond to the given authentication method
	 */
	public UserDetails loadUserByUsernameAndMethod(String username, String method) throws UsernameNotFoundException {
		try {
			loadProperties();
			
			UserWithMethod user = m_users.get(username);
			if (user == null)
				throw new UsernameNotFoundException("Username not found");
			else if (!user.getMethod().equals(method) && method != null)
				throw new UsernameNotFoundException("User/method couple not found");
			else
				return user;
		} catch (IOException exc) {
			throw new UsernameNotFoundException("Could not load the user data");
		}
	}

    public void setResource(Resource resource) throws Exception {
        m_resourceFile = resource.getFile();
        loadProperties();
        
		
		PasswordEncoder pe = getPasswordEncoder();
		boolean fBCryptEnabled = pe != null && pe instanceof CustomBCryptPasswordEncoder;
		if (fBCryptEnabled)
		{
			int nConvertedPasswordCount = 0;
			for (String username : listUsers(false))
			{
				UserDetails user = loadUserByUsernameAndMethod(username, null);
				String password = user.getPassword();
				if (nConvertedPasswordCount == 0 && ((CustomBCryptPasswordEncoder) pe).looksLikeBCrypt(password))
					break;	// all is fine, passwords are encoded

				saveOrUpdateUser(username, password, user.getAuthorities().stream().map(ga -> ga.getAuthority()).toArray(String[]::new), user.isEnabled());
				nConvertedPasswordCount++;
			}
			if (nConvertedPasswordCount > 0)
				LOG.warn("This is the first time the system starts in the encoded password mode: users.properties file was converted to BCrypt-encoded version. " + nConvertedPasswordCount + " passwords were converted. This is a non-reversible operation.");
		}
		else
			for (String username : listUsers(false))
			{
				UserDetails user = loadUserByUsernameAndMethod(username, null);
				String password = user.getPassword();
				if (new CustomBCryptPasswordEncoder().looksLikeBCrypt(password))
					throw new Exception("It looks like the system was reverted back from encoded to plain password mode. The users.properties file contains BCrypt-encoded passwords which cannot be decoded. This can only be fixed by editing it manually.");
				else
					break;
			}
    }

    private Properties loadProperties() throws IOException {
        if (m_resourceFile != null && m_props == null) {
            m_props = new Properties();
            m_props.load(new InputStreamReader(new FileInputStream(m_resourceFile), "UTF-8"));
            
            m_users.clear();
            for (String username: m_props.stringPropertyNames()) {
            	String[] tokens = m_props.getProperty(username).split(",");
            	String password = tokens[0];
            	boolean enabled = true;
            	String method = "";
            	List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            	
            	// Compatibility mode
            	if (!tokens[1].equals("enabled") && !tokens[1].equals("disabled")) {
	            	for (int i = 1; i < tokens.length; i++) {
	            		if (tokens[i].equals("enabled")) {
	            			enabled = true;
	            		} else if (tokens[i].equals("disabled")) {
	            			enabled = false;
	            		} else {
	            			authorities.add(new SimpleGrantedAuthority(tokens[i]));
	            		}
	            	}
	            	saveOrUpdateUser(username, password, authorities.toArray(new SimpleGrantedAuthority[authorities.size()]), enabled, method);
            	} else {
            		enabled = tokens[1].equals("enabled");
            		method = tokens[2];
            		for (String authority : tokens[3].split(";"))
            			authorities.add(new SimpleGrantedAuthority(authority));
            	}
            	m_users.put(username, new UserWithMethod(username, password, authorities, enabled, method));
            	
            }
        }
        return m_props;
    }

    public List<String> listUsers(boolean fExcludeAdministrators) throws IOException {
        List<String> result = new ArrayList<>();
        for (Object key : loadProperties().keySet()) {
            if (!fExcludeAdministrators || !loadUserByUsernameAndMethod((String) key, null).getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))) {
                result.add((String) key);
            }
        }
        return result;
    }

    synchronized public void saveOrUpdateUser(String username, String password, GrantedAuthority[] grantedAuthorities, boolean enabled, String method) throws IOException {
    	String[] stringAuthorities = null;
    	if (grantedAuthorities != null) {
    		stringAuthorities = new String[grantedAuthorities.length];
	    	for (int i = 0; i < grantedAuthorities.length; i++)
	    			stringAuthorities[i] = grantedAuthorities[i].getAuthority();
    	}
    	saveOrUpdateUser(username, password, stringAuthorities, enabled, method);
    }
    
    synchronized public void saveOrUpdateUser(String username, String password, String[] grantedAuthorities, boolean enabled, String method) throws IOException {
        // as long as we keep all write operations in a single synchronized method, we should be safe
        Properties props = loadProperties();
        if (password == null && grantedAuthorities == null && enabled == false)
        	props.remove(username);	// we actually want to delete it
        else
	    {
        	String sPropValue = (passwordEncoder instanceof CustomBCryptPasswordEncoder && !((CustomBCryptPasswordEncoder) passwordEncoder).looksLikeBCrypt(password)) ? passwordEncoder.encode(password) : password;
	        sPropValue += "," + (enabled ? "enabled" : "disabled");
	        sPropValue += "," + method;
	        sPropValue += "," + String.join(";", grantedAuthorities);
	        props.setProperty(username, sPropValue);
	    }
        props.store(new OutputStreamWriter(new FileOutputStream(m_resourceFile), "UTF-8"), "");
        reloadProperties();
    }
    
    synchronized public void saveOrUpdateUser(String username, String password, String[] grantedAuthorities, boolean enabled) throws IOException {
    	saveOrUpdateUser(username, password, grantedAuthorities, enabled, "");
    }

    public boolean deleteUser(String username) throws IOException {
        Properties props = loadProperties();
        if (!props.containsKey(username)) {
            return false;
        }
        
        saveOrUpdateUser(username, null, null, false);

//        props.remove(username);
//        try (FileOutputStream fos = new FileOutputStream(m_resourceFile)) {
//            props.store(new OutputStreamWriter(fos, "UTF-8"), "");
//        }
//        reloadProperties();
        return true;
    }

    public boolean canLoggedUserWriteToSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return false;
        }

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (auth.getAuthority().equals(IRoleDefinition.ROLE_ADMIN)) {
                return true;
            } else if (getWritableEntityTypesByModule(authentication.getAuthorities()).size() > 0 || getManagedEntitiesByModuleAndType(authentication.getAuthorities()).size() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public boolean doesLoggedUserOwnEntities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return false;
        }

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (auth.getAuthority().equals(IRoleDefinition.ROLE_ADMIN)) {
                return true;
            } else if (getManagedEntitiesByModuleAndType(authentication.getAuthorities()).size() > 0) {
                return true;
            }
        }
        return false;
    }

    public Map<String /*module*/, Collection<String /*entity-type*/>> getWritableEntityTypesByModule(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Collection<String>> result = new HashMap<>();
        if (authorities != null) {
            for (GrantedAuthority auth : authorities) {
                String[] splittedPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splittedPermission.length == 3) {
                    if (moduleManager.getModules(null).contains(splittedPermission[0]) && UserPermissionController.rolesByLevel1Type.keySet().contains(splittedPermission[1]) && splittedPermission[2].equals(IRoleDefinition.CREATOR_ROLE_SUFFIX)) {
                        Collection<String> entityTypes = result.get(splittedPermission[0]);
                        if (entityTypes == null) {
                            entityTypes = new HashSet<>();
                            result.put(splittedPermission[0], entityTypes);
                        }
                        entityTypes.add(splittedPermission[1]);
                    }
                }
            }
        }
        return result;
    }

    public Map<String /*module*/, Map<String /*entity-type*/, Collection<Comparable> /*entity-IDs*/>> getManagedEntitiesByModuleAndType(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Map<String, Collection<Comparable>>> result = new HashMap<>();
        if (authorities != null) {
            for (GrantedAuthority auth : authorities) {
                String[] splittedPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splittedPermission.length == 4) {
                    if (moduleManager.getModules(null).contains(splittedPermission[0]) && UserPermissionController.rolesByLevel1Type.keySet().contains(splittedPermission[1]) && splittedPermission[2].equals(IRoleDefinition.ENTITY_MANAGER_ROLE)) {
                        Map<String, Collection<Comparable>> entitiesByTypeForModule = result.get(splittedPermission[0]);
                        if (entitiesByTypeForModule == null) {
                            entitiesByTypeForModule = new HashMap<>();
                            result.put(splittedPermission[0], entitiesByTypeForModule);
                        }
                        Collection<Comparable> entities = entitiesByTypeForModule.get(splittedPermission[1]);
                        if (entities == null) {
                            entities = new HashSet<>();
                            entitiesByTypeForModule.put(splittedPermission[1], entities);
                        }
                        entities.add(new Scanner(splittedPermission[3]).hasNextInt() ? Integer.parseInt(splittedPermission[3]) : splittedPermission[3]);
                    }
                }
            }
        }
        return result;
    }

    public Map<String /*module*/, Map<String /*entity-type*/, Map<String /*role*/, Collection<Comparable> /*entity-IDs*/>>> getCustomRolesByModuleAndEntityType(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Map<String, Map<String, Collection<Comparable>>>> result = new HashMap<>();
        if (authorities != null) {
            for (GrantedAuthority auth : authorities) {
                String[] splittedPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splittedPermission.length == 4) {
                    if (moduleManager.getModules(null).contains(splittedPermission[0]) && UserPermissionController.rolesByLevel1Type.keySet().contains(splittedPermission[1]) && UserPermissionController.rolesByLevel1Type.get(splittedPermission[1]).contains(splittedPermission[2])) {
                        Map<String, Map<String, Collection<Comparable>>> rolesByEntityTypeForModule = result.get(splittedPermission[0]);
                        if (rolesByEntityTypeForModule == null) {
                            rolesByEntityTypeForModule = new HashMap<>();
                            result.put(splittedPermission[0], rolesByEntityTypeForModule);
                        }
                        Map<String, Collection<Comparable>> entityIDsByRoles = rolesByEntityTypeForModule.get(splittedPermission[1]);
                        if (entityIDsByRoles == null) {
                            entityIDsByRoles = new HashMap<>();
                            rolesByEntityTypeForModule.put(splittedPermission[1], entityIDsByRoles);
                        }
                        Collection<Comparable> entities = entityIDsByRoles.get(splittedPermission[2]);
                        if (entities == null) {
                            entities = new HashSet<>();
                            entityIDsByRoles.put(splittedPermission[2], entities);
                        }
                        entities.add(new Scanner(splittedPermission[3]).hasNextInt() ? Integer.parseInt(splittedPermission[3]) : splittedPermission[3]);
                    }
                }
            }
        }
        return result;
    }

    public void allowManagingEntity(String sModule, String entityType, Comparable entityId, String username) throws IOException {
        UserDetails owner = loadUserByUsernameAndMethod(username, null);
		if (owner.getAuthorities() != null && (owner.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))))
			return;	// no need to grant any role to administrators

        SimpleGrantedAuthority role = new SimpleGrantedAuthority(sModule + UserPermissionController.ROLE_STRING_SEPARATOR + entityType + UserPermissionController.ROLE_STRING_SEPARATOR + IRoleDefinition.ENTITY_MANAGER_ROLE + UserPermissionController.ROLE_STRING_SEPARATOR + entityId);
        if (!owner.getAuthorities().contains(role)) {
            HashSet<String> authoritiesToSave = new HashSet<>();
            authoritiesToSave.add(role.getAuthority());
            for (GrantedAuthority ga : owner.getAuthorities()) {
                authoritiesToSave.add(ga.getAuthority());
            }
            saveOrUpdateUser(username, owner.getPassword(), authoritiesToSave.toArray(new String[authoritiesToSave.size()]), owner.isEnabled());
        }
    }

    public int countByLoginLookup(String sLoginLookup) throws IOException {
        Authentication authorities = SecurityContextHolder.getContext().getAuthentication();
        boolean fLoggedUserIsAdmin = authorities.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
        if (sLoginLookup == null) {
            return listUsers(!fLoggedUserIsAdmin).size();
        }

        int nCount = 0;
        for (String sUserName : listUsers(!fLoggedUserIsAdmin)) {
            if (sUserName.startsWith(sLoginLookup)) {
                nCount++;
            }
        }
        return nCount;
    }

    public List<UserDetails> listByLoginLookup(String sLoginLookup, int max, int size) throws IOException {
        Authentication authorities = SecurityContextHolder.getContext().getAuthentication();
        boolean fLoggedUserIsAdmin = authorities.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
        List<UserDetails> result = new ArrayList<>();
        List<String> userList = listUsers(!fLoggedUserIsAdmin);
        String[] userArray = userList.toArray(new String[userList.size()]);
        Arrays.sort(userArray);
        for (String sUserName : userArray) {
            if (sLoginLookup == null || sUserName.startsWith(sLoginLookup)) {
                try {
                    result.add(loadUserByUsernameAndMethod(sUserName, null));
                } catch (UsernameNotFoundException unfe) {
                    LOG.error("Unable to load user by username", unfe);
                }
            }
        }
        return result;
    }

    /**
     * Reload properties.
     */
    public void reloadProperties()
    {
    	m_props = null;
    }
    
    public int getUserCount() {
    	return m_users.size();
    }
}
