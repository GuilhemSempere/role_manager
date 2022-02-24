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
import java.util.stream.Collectors;

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

import fr.cirad.security.base.IModuleManager;
import fr.cirad.security.base.IRoleDefinition;
import fr.cirad.web.controller.security.UserPermissionController;

public class ReloadableInMemoryDaoImpl implements UserDetailsService {

    private static final Logger LOG = Logger.getLogger(ReloadableInMemoryDaoImpl.class);

    @Autowired
    private IModuleManager moduleManager;
    
    @Autowired
    private PasswordEncoder passwordEncoder;	// this may be either a CustomBCryptPasswordEncoder or a NoOpPasswordEncoder

    private File m_resourceFile;
    private HashMap<String, UserWithMethod> m_users;
    
    public ReloadableInMemoryDaoImpl() {
    	m_users = null;
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
			UserWithMethod user = m_users.get(username);
			if (user == null)
				throw new UsernameNotFoundException("Username not found");
			else if (!user.getMethod().equals(method) && method != null)
				throw new UsernameNotFoundException("User/method couple not found");
			else
				return user;
		} catch (NullPointerException exc) {  // Resource probably not set
			throw new UsernameNotFoundException("No data loaded", exc);
		}
	}

    public void setResource(Resource resource) throws Exception {
        m_resourceFile = resource.getFile();
        loadProperties();
        
		
		PasswordEncoder pe = getPasswordEncoder();
		boolean fBCryptEnabled = pe != null && pe instanceof CustomBCryptPasswordEncoder;
		if (fBCryptEnabled) {
			int nConvertedPasswordCount = 0;
			for (String username : m_users.keySet()) {
				UserWithMethod user = m_users.get(username);
				String password = user.getPassword();
				if (nConvertedPasswordCount == 0 && ((CustomBCryptPasswordEncoder) pe).looksLikeBCrypt(password))
					break;	// all is fine, passwords are encoded

				bufferSaveOrUpdateUser(username, password, user.getAuthorities(), user.isEnabled(), user.getMethod());
				nConvertedPasswordCount++;
			}
			
			if (nConvertedPasswordCount > 0) {
				saveUsers();
				LOG.warn("This is the first time the system starts in the encoded password mode: users.properties file was converted to BCrypt-encoded version. " + nConvertedPasswordCount + " passwords were converted. This is a non-reversible operation.");
			}
		}
		else
			for (String username : m_users.keySet()) {
				UserDetails user = m_users.get(username);
				String password = user.getPassword();
				if (new CustomBCryptPasswordEncoder().looksLikeBCrypt(password))
					throw new Exception("It looks like the system was reverted back from encoded to plain password mode. The users.properties file contains BCrypt-encoded passwords which cannot be decoded. This can only be fixed by editing it manually.");
				else
					break;
			}
    }

    private void loadProperties() throws IOException {
    	try {
        if (m_resourceFile != null && m_users == null) {
        	m_users = new HashMap<String, UserWithMethod>();
            Properties props = new Properties();
            props.load(new InputStreamReader(new FileInputStream(m_resourceFile), "UTF-8"));
            
            m_users.clear();
            for (String username: props.stringPropertyNames()) {
            	String[] tokens = props.getProperty(username).split(",", -1);  // Negative limit to keep trailing empty strings
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
	            	bufferSaveOrUpdateUser(username, password, authorities, enabled, method);
					LOG.info("Updated user info from obsolete to current structure for " + username);
            	} else {
            		enabled = tokens[1].equals("enabled");
            		method = tokens[2];
            		for (String authority : tokens[3].split(";")) {
            			if (!authority.isEmpty())
            				authorities.add(new SimpleGrantedAuthority(authority));
            		}
            	}
            	m_users.put(username, new UserWithMethod(username, password, authorities, enabled, method));
            }
            saveUsers();
        }
    	} catch (Throwable t) {
    		t.printStackTrace();
    		throw t;
    	}
    }

    public List<String> listUsers(boolean fExcludeAdministrators) throws IOException {
        List<String> result = new ArrayList<>();
        for (String key : m_users.keySet()) {
            if (!fExcludeAdministrators || !loadUserByUsernameAndMethod(key, null).getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))) {
                result.add(key);
            }
        }
        return result;
    }
    
    /** Update a user in memory, without saving it immediately to disk */
    synchronized public void bufferSaveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled, String method) throws IOException {
        if (password == null && grantedAuthorities == null && enabled == false) {
        	m_users.remove(username);	// we actually want to delete it
        } else {
        	UserWithMethod user = m_users.get(username);
        	
        	password = (passwordEncoder instanceof CustomBCryptPasswordEncoder && !((CustomBCryptPasswordEncoder) passwordEncoder).looksLikeBCrypt(password)) ? passwordEncoder.encode(password) : password;
        	if (user == null) {
        		user = new UserWithMethod(username, password, grantedAuthorities, enabled, method);
        		m_users.put(username, user);
        	} else {
        		user.setUsername(username);
        		user.setPassword(password);
        		user.setAuthorities(grantedAuthorities);
        		user.setEnabled(enabled);
        		user.setMethod(method);
        	}
	    }
    }
    
    /** Update a user in memory, without saving it immediately to disk */
    public void bufferSaveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled) throws IOException {
    	bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled, "");
    }
    
    public void bufferSaveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled, String method) throws IOException {
    	List<GrantedAuthority> grantedAuthorities = Arrays.stream(stringAuthorities).map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList());
    	bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled, method);
    }
    
    public void bufferSaveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled) throws IOException {
    	bufferSaveOrUpdateUser(username, password, stringAuthorities, enabled, "");
    }
    
    /** Update a user, save it to disk and reload users */
    public void saveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled, String method) throws IOException {
	    bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled, method);
	    saveUsers();
    }
    
    /** Update a user, save it to disk and reload users */
    public void saveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled) throws IOException {
	    bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled);
	    saveUsers();
    }
    
    /** Update a user, save it to disk and reload users */
    public void saveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled, String method) throws IOException {
	    bufferSaveOrUpdateUser(username, password, stringAuthorities, enabled, method);
	    saveUsers();
    }
    
    /** Update a user, save it to disk and reload users */
    public void saveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled) throws IOException {
	    bufferSaveOrUpdateUser(username, password, stringAuthorities, enabled);
	    saveUsers();
    }
    
    /** Save and reload users stored in memory */
    synchronized public void saveUsers() throws IOException {
    	Properties props = new Properties();
    	for (String username : m_users.keySet()) {
    		UserWithMethod user = m_users.get(username);
    		String sPropValue = user.getPassword();
            sPropValue += "," + (user.isEnabled() ? "enabled" : "disabled");
            sPropValue += "," + user.getMethod();
            sPropValue += "," + String.join(";", user.getAuthorities().stream().map(authority -> authority.toString()).collect(Collectors.toList()));
            props.put(username, sPropValue);
    	}
    	
	    props.store(new OutputStreamWriter(new FileOutputStream(m_resourceFile), "UTF-8"), "");
    }

    synchronized public boolean deleteUser(String username) throws IOException {
        if (!m_users.containsKey(username)) {
            return false;
        }
        
        m_users.remove(username);
        saveUsers();
        return true;
    }

    public boolean canLoggedUserWriteToSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return false;
        }

        Collection<? extends GrantedAuthority> loggedUserAuthorities = getLoggedUserAuthorities();
        for (GrantedAuthority auth : loggedUserAuthorities) {
            if (auth.getAuthority().equals(IRoleDefinition.ROLE_ADMIN)) {
                return true;
            } else if (getSupervisedModules(loggedUserAuthorities).size() > 0 || getManagedEntitiesByModuleAndType(loggedUserAuthorities).size() > 0) {
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

        Collection<? extends GrantedAuthority> loggedUserAuthorities = getLoggedUserAuthorities();
        for (GrantedAuthority auth : loggedUserAuthorities) {
            if (auth.getAuthority().equals(IRoleDefinition.ROLE_ADMIN)) {
                return true;
            } else if (getManagedEntitiesByModuleAndType(loggedUserAuthorities).size() > 0) {
                return true;
            }
        }
        return false;
    }

    public HashSet<String /*module*/> getSupervisedModules(Collection<? extends GrantedAuthority> authorities) {
        HashSet<String>result = new HashSet<>();
        if (authorities != null) {
            Collection<String> modules = moduleManager.getModules(null);
            for (GrantedAuthority auth : authorities) {
                String[] splitPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splitPermission.length == 2 && modules.contains(splitPermission[0]) && splitPermission[1].equals(IRoleDefinition.ROLE_DB_SUPERVISOR))
                    result.add(splitPermission[0]);
            }
        }
        return result;
    }

    public Map<String /*module*/, Map<String /*entity-type*/, Collection<Comparable> /*entity-IDs*/>> getManagedEntitiesByModuleAndType(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Map<String, Collection<Comparable>>> result = new HashMap<>();
        if (authorities != null) {
            Collection<String> modules = moduleManager.getModules(null);
            for (GrantedAuthority auth : authorities) {
                String[] splitPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splitPermission.length == 4) {
                    if (modules.contains(splitPermission[0]) && UserPermissionController.rolesByLevel1Type.keySet().contains(splitPermission[1]) && splitPermission[2].equals(IRoleDefinition.ENTITY_MANAGER_ROLE)) {
                        Map<String, Collection<Comparable>> entitiesByTypeForModule = result.get(splitPermission[0]);
                        if (entitiesByTypeForModule == null) {
                            entitiesByTypeForModule = new HashMap<>();
                            result.put(splitPermission[0], entitiesByTypeForModule);
                        }
                        Collection<Comparable> entities = entitiesByTypeForModule.get(splitPermission[1]);
                        if (entities == null) {
                            entities = new HashSet<>();
                            entitiesByTypeForModule.put(splitPermission[1], entities);
                        }
                        entities.add(new Scanner(splitPermission[3]).hasNextInt() ? Integer.parseInt(splitPermission[3]) : splitPermission[3]);
                    }
                }
            }
        }
        return result;
    }

    public Map<String /*module*/, Map<String /*entity-type*/, Map<String /*role*/, Collection<Comparable> /*entity-IDs*/>>> getCustomRolesByModuleAndEntityType(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Map<String, Map<String, Collection<Comparable>>>> result = new HashMap<>();
        if (authorities != null) {
            Collection<String> modules = moduleManager.getModules(null);
            for (GrantedAuthority auth : authorities) {
                String[] splitPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splitPermission.length == 4) {
                    if (modules.contains(splitPermission[0]) && UserPermissionController.rolesByLevel1Type.keySet().contains(splitPermission[1]) && UserPermissionController.rolesByLevel1Type.get(splitPermission[1]).contains(splitPermission[2])) {
                        Map<String, Map<String, Collection<Comparable>>> rolesByEntityTypeForModule = result.get(splitPermission[0]);
                        if (rolesByEntityTypeForModule == null) {
                            rolesByEntityTypeForModule = new HashMap<>();
                            result.put(splitPermission[0], rolesByEntityTypeForModule);
                        }
                        Map<String, Collection<Comparable>> entityIDsByRoles = rolesByEntityTypeForModule.get(splitPermission[1]);
                        if (entityIDsByRoles == null) {
                            entityIDsByRoles = new HashMap<>();
                            rolesByEntityTypeForModule.put(splitPermission[1], entityIDsByRoles);
                        }
                        Collection<Comparable> entities = entityIDsByRoles.get(splitPermission[2]);
                        if (entities == null) {
                            entities = new HashSet<>();
                            entityIDsByRoles.put(splitPermission[2], entities);
                        }
                        entities.add(new Scanner(splitPermission[3]).hasNextInt() ? Integer.parseInt(splitPermission[3]) : splitPermission[3]);
                    }
                }
            }
        }
        return result;
    }

    public void allowManagingEntity(String sModule, String entityType, Comparable entityId, String username) throws IOException {
        UserWithMethod owner = (UserWithMethod)loadUserByUsernameAndMethod(username, null);
		if (owner.getAuthorities() != null && (owner.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))))
			return;	// no need to grant any role to administrators

        SimpleGrantedAuthority role = new SimpleGrantedAuthority(sModule + UserPermissionController.ROLE_STRING_SEPARATOR + entityType + UserPermissionController.ROLE_STRING_SEPARATOR + IRoleDefinition.ENTITY_MANAGER_ROLE + UserPermissionController.ROLE_STRING_SEPARATOR + entityId);
        if (!owner.getAuthorities().contains(role)) {
            HashSet<GrantedAuthority> authoritiesToSave = new HashSet<>();
            authoritiesToSave.add(role);
            for (GrantedAuthority authority : owner.getAuthorities()) {
                authoritiesToSave.add(authority);
            }
            saveOrUpdateUser(username, owner.getPassword(), authoritiesToSave, owner.isEnabled(), owner.getMethod());
        }
    }

    public int countByLoginLookup(String sLoginLookup) throws IOException {
        boolean fLoggedUserIsAdmin = getLoggedUserAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
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
        boolean fLoggedUserIsAdmin = getLoggedUserAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
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
     * Reload users properties file. The users currently in memory are not saved beforehand.
     */
    public void reloadProperties() throws IOException {
    	m_users = null;
    	loadProperties();
    }
    
    public int getUserCount() {
    	return m_users.size();
    }

    /**
     * @return always up-to-date authorities, contrarily to those obtained via SecurityContextHolder.getContext().getAuthentication() which requires re-authentication to account for changes
     */
    public Collection<? extends GrantedAuthority> getLoggedUserAuthorities()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        if ("anonymousUser".equals(username))
            return auth.getAuthorities();
        
        return loadUserByUsernameAndMethod(username, null).getAuthorities();
    }

    public void updateUser(String username, UserWithMethod user) {
        m_users.put(username, user);
    }
}
