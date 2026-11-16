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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import fr.cirad.manager.IModuleManager;
import fr.cirad.security.base.IRoleDefinition;
import fr.cirad.tools.PropertiesFileUtils;
import fr.cirad.web.controller.security.UserPermissionController;

public class ReloadableInMemoryDaoImpl implements UserDetailsService {

    private static final Logger LOG = Logger.getLogger(ReloadableInMemoryDaoImpl.class);

    @Autowired
    private IModuleManager moduleManager;
    
    @Autowired
    private PasswordEncoder passwordEncoder;	// this may be either a CustomBCryptPasswordEncoder or a NoOpPasswordEncoder

    private File m_resourceFile;
    private HashMap<String, UserWithMethod> m_users;
    private boolean m_fAdminExistedAtLoadTime = false;	// used to prevent persisting the removal of the last administrator

    public ReloadableInMemoryDaoImpl() {
    	m_users = null;
    }

    public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			UserWithMethod user = findByUsername(username);
			if (user == null)
				throw new UsernameNotFoundException("Username not found");
			else
				return user;
		} catch (NullPointerException exc) {  // Resource probably not set
			throw new UsernameNotFoundException("No data loaded", exc);
		}
    }

    public UserWithMethod loadUserByUserId(String userId) throws UsernameNotFoundException {
		try {
			UserWithMethod user = m_users.get(userId);
			if (user == null)
				throw new UsernameNotFoundException("User ID not found");
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
			for (UserWithMethod user : new ArrayList<>(m_users.values())) {
				String password = user.getPassword();
				if (nConvertedPasswordCount == 0 && ((CustomBCryptPasswordEncoder) pe).looksLikeBCrypt(password))
					break;	// all is fine, passwords are encoded

				bufferSaveOrUpdateUser(user.getUsername(), password, user.getAuthorities(), user.isEnabled(), user.getMethod(), user.getEmail(), user.getSub());
				nConvertedPasswordCount++;
			}
			
			if (nConvertedPasswordCount > 0)
				try {
					saveUsers();
					LOG.warn("This is the first time the system starts in the encoded password mode: users.properties file was converted to BCrypt-encoded version. " + nConvertedPasswordCount + " passwords were converted. This is a non-reversible operation.");
				}
				catch (IOException ioe) {
					LOG.error("Unable to save BCrypt-encoded passwords to " + m_resourceFile + ", they will be saved next time users are", ioe);
				}
		}
		else
			for (UserWithMethod user : m_users.values()) {
				String password = user.getPassword();
				if (new CustomBCryptPasswordEncoder().looksLikeBCrypt(password))
					throw new Exception("It looks like the system was reverted back from encoded to plain password mode. The users.properties file contains BCrypt-encoded passwords which cannot be decoded. This can only be fixed by editing it manually.");
				else
					break;
			}
    }

    private static Properties readProperties(File file) throws IOException {
    	Properties props = new Properties();
    	try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
    		props.load(reader);
    	}
    	return props;
    }

	private void loadProperties() throws IOException {
    	try {
	        if (m_resourceFile != null && m_users == null) {
	        	m_users = new HashMap<>();
	        	List<UserWithMethod> usersForWhichToCopyEmailFromUsername = new ArrayList<>();
	            Properties props = readProperties(m_resourceFile);
	            boolean fNeedToSave = false;

	            if (props.isEmpty()) {	// may result from an interrupted write (e.g. disk full) in a previous version
	            	File backupFile = new File(m_resourceFile.getPath() + PropertiesFileUtils.BACKUP_SUFFIX);
	            	Properties backupProps = backupFile.exists() ? readProperties(backupFile) : new Properties();
	            	if (backupProps.isEmpty())
	            		LOG.error(m_resourceFile + " contains no users and no usable backup was found at " + backupFile + ": nobody will be able to log in until it is fixed manually");
	            	else {
	            		LOG.error(m_resourceFile + " contains no users, restoring " + backupProps.size() + " users from " + backupFile);
	            		props = backupProps;
	            		fNeedToSave = true;
	            	}
	            }

	            m_users.clear();
	            for (String username : props.stringPropertyNames()) {
	            	String[] tokens = props.getProperty(username).split(",", -1);  // Negative limit to keep trailing empty strings
	            	String password = tokens[0];
	            	String email = null;
	            	boolean enabled = true;
	            	String method = "";
	            	String sub = null;
	            	List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
	            	
	            	// Compatibility mode
	            	if (!tokens[1].equals("enabled") && !tokens[1].equals("disabled")) {	// old format looking like: gui=tou,IAVAO_Sorgho$project$SNPCLUST_EDITOR$2,IAVAO_Sorgho$project$READER$1,enabled
		            	for (int i = 1; i < tokens.length; i++) {
		            		if (tokens[i].equals("disabled"))
		            			enabled = false;
		            		else
		            			authorities.add(new SimpleGrantedAuthority(tokens[i]));
		            	}
		            	bufferSaveOrUpdateUser(username, password, authorities, enabled, method, email);
						LOG.info("Updated user info from obsolete to current structure for " + username);
						fNeedToSave = true;
	            	} else {
	            		enabled = tokens[1].equals("enabled");
	            		method = tokens[2];
	            		for (String authority : tokens[3].split(";")) {
	            			if (!authority.isEmpty())
	            				authorities.add(new SimpleGrantedAuthority(authority));
	            		}
	            		if (tokens.length >= 5 && !tokens[4].trim().isEmpty())
	            			email = tokens[4].trim();
	            		if (tokens.length >= 6 && !tokens[5].trim().isEmpty())
	            			sub = tokens[5].trim();
	            	}

	            	UserWithMethod userWM = new UserWithMethod(username, password, authorities, enabled, method, email, sub);
	            	m_users.put(userWM.getUserId(), userWM);
        			if (email == null) {
        				if (UserWithMethod.isEmailAddress(username))
        					usersForWhichToCopyEmailFromUsername.add(userWM);	// we will try and set it from the username (which appears to be an email address) once all users have been loaded
            			else
            				LOG.debug("No e-mail address available for user " + username);
        			}
	            }
	            
	            for (UserWithMethod user : usersForWhichToCopyEmailFromUsername)
    				try {
        				if (getUserWithMethodByEmailAddress(user.getUsername()) != null)
        					throw new IllegalArgumentException("A different user already has this e-mail address");

        				user.setEmail(user.getUsername().toLowerCase());
        				LOG.info("Set e-mail address from username for user " + user.getUsername());
        				fNeedToSave = true;
    				}
    				catch (IllegalArgumentException iae) {
    					LOG.warn("Unable to set e-mail address from username for user " + user.getUsername() + ", because this address is already used", iae);
    				}

	            m_fAdminExistedAtLoadTime = containsAdministrator();
	            if (!m_users.isEmpty() && !m_fAdminExistedAtLoadTime)
	            	LOG.error(m_resourceFile + " defines no user with " + IRoleDefinition.ROLE_ADMIN + " role");

	            if (fNeedToSave)
	            	try {
	            		saveUsers();
	            	}
	            	catch (IOException ioe) {
	            		LOG.error("Unable to save updated user info to " + m_resourceFile + ", it will be saved next time users are", ioe);
	            	}
	        }
    	} catch (Throwable t) {
    		LOG.error(t);
    		throw t;
    	}
    }

    public List<String> listUsers(boolean fExcludeAdministrators) throws IOException {
        List<String> result = new ArrayList<>();
        for (UserWithMethod user : m_users.values()) {
            if (!fExcludeAdministrators || !user.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))) {
                result.add(user.getUsername());
            }
        }
        return result;
    }
    
    /** Update a user in memory, without saving it immediately to disk */
    synchronized public void bufferSaveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled, String method, String email) throws IOException {
    	bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled, method, email, null);
    }

    /** Update a user in memory, without saving it immediately to disk. A null/blank {@code sub} leaves the existing value untouched. */
    synchronized public void bufferSaveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled, String method, String email, String sub) throws IOException {
    	String normalizedSub = normalizeSub(sub);
    	UserWithMethod user = findUser(username, method, normalizedSub);

        if (password == null && grantedAuthorities == null && enabled == false) {
        	m_users.remove(username);	// we actually want to delete it
        } else {
        	password = (passwordEncoder instanceof CustomBCryptPasswordEncoder && !((CustomBCryptPasswordEncoder) passwordEncoder).looksLikeBCrypt(password)) ? passwordEncoder.encode(password) : password;
        	boolean fValidEmailPassed = email != null && !email.trim().isEmpty();
        	if (user == null) {	// it's a new one
        		if (findByUsername(username) != null)
        			throw new IOException("A user with this username already exists: " + username);
        		if (fValidEmailPassed && getUserWithMethodByEmailAddress(email) != null)
        			throw new IOException("A user with this e-mail address already exists: " + email);
        		else {
	        		user = new UserWithMethod(username, password, grantedAuthorities, enabled, method, email, normalizedSub);
	        		m_users.put(user.getUserId(), user);
        		}
        	} else {
        		if (fValidEmailPassed && !email.trim().equalsIgnoreCase(user.getEmail()) && getUserWithMethodByEmailAddress(email) != null)
        			throw new IOException("A user with this e-mail address already exists: " + email);

        		user.setUsername(username);
        		user.setPassword(method.isEmpty() ? password : "" /* if using a remote auth system then we don't need to store a password */);
        		user.setAuthorities(grantedAuthorities);
        		user.setEnabled(enabled);
        		user.setMethod(method);
        		user.setEmail(email == null ? user.getEmail() : email.trim());
        		if (normalizedSub != null)
        			user.setSub(normalizedSub);
        	}
	    }
    }
    
    public void bufferSaveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled, String method, String email) throws IOException {
    	List<GrantedAuthority> grantedAuthorities = Arrays.stream(stringAuthorities).map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList());
    	bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled, method, email);
    }

    /** Update a user and save it to disk. If saving fails, the user is restored to its previous state in memory */
    synchronized public void saveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled, String method, String email) throws IOException {
    	saveOrUpdateUser(username, password, grantedAuthorities, enabled, method, email, null);
    }

    /** Update a user and save it to disk. If saving fails, the user is restored to its previous state in memory. A null/blank {@code sub} leaves the existing value untouched. */
    synchronized public void saveOrUpdateUser(String username, String password, Collection<? extends GrantedAuthority> grantedAuthorities, boolean enabled, String method, String email, String sub) throws IOException {
    	String normalizedSub = normalizeSub(sub);
    	UserWithMethod existing = findUser(username, method, normalizedSub);
    	UserWithMethod previousState = copyOf(existing);
    	try {
    		bufferSaveOrUpdateUser(username, password, grantedAuthorities, enabled, method, email, normalizedSub);
    		saveUsers();
    	}
    	catch (IOException | RuntimeException e) {
    		String userId = previousState != null ? previousState.getUserId() : UserWithMethod.buildUserId(username, method, normalizedSub);
    		UserWithMethod insertedUser = m_users.get(userId);
    		if (previousState == null) {
    			if (insertedUser != null && username.equals(insertedUser.getUsername()) && Objects.equals(method, insertedUser.getMethod()) && Objects.equals(normalizedSub, insertedUser.getSub()))
    				m_users.remove(userId);
    		}
    		else
    			m_users.put(userId, previousState);
    		throw e;
    	}
    }

    /** Update a user and save it to disk. If saving fails, the user is restored to its previous state in memory */
    public void saveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled, String method, String email) throws IOException {
    	List<GrantedAuthority> grantedAuthorities = Arrays.stream(stringAuthorities).map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList());
	    saveOrUpdateUser(username, password, grantedAuthorities, enabled, method, email);
    }

    /** Update a user and save it to disk. If saving fails, the user is restored to its previous state in memory */
    public void saveOrUpdateUser(String username, String password, String[] stringAuthorities, boolean enabled, String method, String email, String oauthSub) throws IOException {
    	List<GrantedAuthority> grantedAuthorities = Arrays.stream(stringAuthorities).map(authority -> new SimpleGrantedAuthority(authority)).collect(Collectors.toList());
	    saveOrUpdateUser(username, password, grantedAuthorities, enabled, method, email, oauthSub);
    }

    private static UserWithMethod copyOf(UserWithMethod user) {
    	return user == null ? null : new UserWithMethod(user.getUsername(), user.getPassword(), user.getAuthorities(), user.isEnabled(), user.getMethod(), user.getEmail(), user.getSub());
    }

    private boolean containsAdministrator() {
    	SimpleGrantedAuthority adminRole = new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN);
    	return m_users.values().stream().anyMatch(user -> user.getAuthorities().contains(adminRole));
    }

    /** Save users stored in memory to disk. The file is replaced atomically so that a failure (e.g. disk full) leaves it untouched */
    synchronized public void saveUsers() throws IOException {
    	if (m_users.isEmpty())
    		throw new IOException("Refusing to save an empty user list to " + m_resourceFile);
    	if (m_fAdminExistedAtLoadTime && !containsAdministrator())
    		throw new IOException("Refusing to save a user list containing no administrator to " + m_resourceFile);

    	Properties props = new Properties();
    	for (UserWithMethod user : m_users.values()) {
    		String sPropValue = user.getPassword();
            sPropValue += "," + (user.isEnabled() ? "enabled" : "disabled");
            sPropValue += "," + user.getMethod();
            sPropValue += "," + String.join(";", user.getAuthorities().stream().map(authority -> authority.toString()).collect(Collectors.toList()));
            sPropValue += (user.getEmail() == null ? "" : ("," + user.getEmail()));
            sPropValue += (user.getSub() == null ? "" : ("," + user.getSub()));
            props.put(user.getUsername(), sPropValue);
    	}
    	
    	PropertiesFileUtils.storeAtomically(props, m_resourceFile, "", StandardCharsets.UTF_8);
    }

    synchronized public boolean deleteUser(String username) throws IOException {
    	UserWithMethod user = findByUsername(username);
        if (user == null) {
            return false;
        }

        String userId = user.getUserId();
        UserWithMethod removedUser = m_users.remove(userId);
        try {
        	saveUsers();
        }
        catch (IOException ioe) {
        	m_users.put(userId, removedUser);
        	throw ioe;
        }
        return true;
    }

    public boolean canLoggedUserWriteToSystem() {
        Collection<? extends GrantedAuthority> loggedUserAuthorities = getLoggedUserAuthorities();
        for (GrantedAuthority auth : loggedUserAuthorities) {
            if (auth.getAuthority().equals(IRoleDefinition.ROLE_ADMIN) || auth.getAuthority().equals(IRoleDefinition.ROLE_DB_CREATOR)) {
                return true;
            } else if (getSupervisedModules(loggedUserAuthorities).size() > 0 || getManagedEntitiesByModuleAndType(loggedUserAuthorities).size() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public boolean doesLoggedUserOwnEntities() {
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

    public HashSet<String /*module*/> getBaseReaderModules(Collection<? extends GrantedAuthority> authorities) {
        HashSet<String>result = new HashSet<>();
        if (authorities != null) {
            Collection<String> modules = moduleManager.getModules(null);
            for (GrantedAuthority auth : authorities) {
                String[] splitPermission = auth.getAuthority().split(Pattern.quote(UserPermissionController.ROLE_STRING_SEPARATOR));
                if (splitPermission.length == 2 && modules.contains(splitPermission[0]) && splitPermission[1].equals(IRoleDefinition.ROLE_DB_READER))
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

    public int countByLoginLookup(String sLoginLookup) throws IOException {
    	Collection<? extends GrantedAuthority> loggedUserAuthorities = getLoggedUserAuthorities();
    	if (loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ANONYMOUS)))
   			return 0;
    	
    	if (!canLoggedUserWriteToSystem())
    		return 1;	// he may only see himself

        boolean fLoggedUserIsAdmin = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
        if (sLoginLookup == null)
            return listUsers(!fLoggedUserIsAdmin).size();

        int nCount = 0;
        for (String sUserName : listUsers(!fLoggedUserIsAdmin)) {
            if (sUserName.startsWith(sLoginLookup)) {
                nCount++;
            }
        }
        return nCount;
    }

    public List<UserDetails> listByLoginLookup(String sLoginLookup, int page, int size) throws IOException {
        List<UserDetails> result = new ArrayList<>();
    	Collection<? extends GrantedAuthority> loggedUserAuthorities = getLoggedUserAuthorities();
    	if (loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ANONYMOUS)))
   			return result;

        boolean fLoggedUserIsAdmin = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
        List<String> userList = !canLoggedUserWriteToSystem() ? Arrays.asList(SecurityContextHolder.getContext().getAuthentication().getName()) /* he may only see himself */: listUsers(!fLoggedUserIsAdmin);
        Collections.sort(userList);
        for (String sUserName : userList) {
            if (sLoginLookup == null || sUserName.startsWith(sLoginLookup)) {
                try {
                    result.add(loadUserByUsername(sUserName));
                } catch (UsernameNotFoundException unfe) {
                    LOG.error("Unable to load user by username", unfe);
                }
            }
        }
        result = result.subList(page * size, Math.min(result.size(), size < 1 ? result.size() : size * (page + 1)));
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
        return getUserAuthorities(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * @return always up-to-date authorities, contrarily to those obtained via SecurityContextHolder.getContext().getAuthentication() which requires re-authentication to account for changes
     */
    public Collection<? extends GrantedAuthority> getUserAuthorities(Authentication auth)
    {
        String username = auth.getName();
        if ("anonymousUser".equals(username))
            return auth.getAuthorities();
        
        return loadUserByUsername(username).getAuthorities();
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

    private static String normalizeSub(String sub) {
    	return sub == null || sub.isBlank() ? null : sub.trim();
    }

    /** Finds a user by its username (the map itself is keyed by userId). */
    private UserWithMethod findByUsername(String username) {
    	UserWithMethod direct = m_users.get(username);
    	if (direct != null && username.equals(direct.getUsername()))
    		return direct;
    	for (UserWithMethod candidate : m_users.values())
    		if (username.equals(candidate.getUsername()))
    			return candidate;
    	return null;
    }

    /** Resolves the user targeted by an update: preferably via its userId, then by username and method. */
    private UserWithMethod findUser(String username, String method, String sub) {
    	if (sub != null) {
    		UserWithMethod user = m_users.get(UserWithMethod.buildUserId(username, method, sub));
    		if (user != null)
    			return user;
    	}

    	UserWithMethod direct = m_users.get(username);
    	if (direct != null && (method == null || method.equals(direct.getMethod())))
    		return direct;

    	UserWithMethod match = null;
    	for (UserWithMethod candidate : m_users.values())
    		if (username.equals(candidate.getUsername()) && (method == null || method.equals(candidate.getMethod()))) {
    			if (match != null)
    				throw new IllegalArgumentException("Several users found with username '" + username + "' and method '" + method + "'");
    			match = candidate;
    		}
    	return match;
    }

    public UserWithMethod getUserWithMethodByEmailAddress(String email) throws IllegalArgumentException {
    	if (email != null)
    		email = email.trim().toLowerCase();

    	List<UserWithMethod> users = new ArrayList<>();
    	for (UserWithMethod user : m_users.values()) {
    		if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email))
    			users.add(user);
    	}
    	if (users.size() > 1)
    		throw new IllegalArgumentException(users.size() + " users found with e-mail address: " + email);

    	return users.isEmpty() ? null : users.get(0);
    }

    public UserWithMethod getUserWithMethodByMethodAndSubject(String method, String sub) throws IllegalArgumentException {
        if (method == null || method.isBlank() || sub == null || sub.isBlank())
            return null;

        List<UserWithMethod> users = new ArrayList<>();
        for (UserWithMethod user : m_users.values()) {
            if (method.equalsIgnoreCase(user.getMethod()) && sub.equals(user.getSub()))
                users.add(user);
        }
        if (users.size() > 1)
            throw new IllegalArgumentException(users.size() + " users found with method " + method + " and subject: " + sub);


        return users.isEmpty() ? null : users.get(0);
    }

}
