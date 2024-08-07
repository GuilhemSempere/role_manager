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
package fr.cirad.web.controller.security;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import fr.cirad.manager.IModuleManager;
import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.UserWithMethod;
import fr.cirad.security.base.IRoleDefinition;

@Controller
public class UserPermissionController
{
	private static final Logger LOG = Logger.getLogger(UserPermissionController.class);

	static final public String FRONTEND_URL = "private/roleManager";

	static final public String userListPageURL = "/" + FRONTEND_URL + "/UserList.do_";
	static final public String userListDataURL = "/" + FRONTEND_URL + "/listUsers.json_";
	static final public String userListCountURL = "/" + FRONTEND_URL + "/countUsers.json_";
	static final public String userRemovalURL = "/" + FRONTEND_URL + "/removeUser.json_";
	static final public String userDetailsURL = "/" + FRONTEND_URL + "/UserDetails.do_";
	static final public String userPermissionURL = "/" + FRONTEND_URL + "/UserPermissions.do_";

	public static final String LEVEL1 = "LEVEL1";
	public static final String LEVEL1_ROLES = "LEVEL1_ROLES";
	public static final String LEVEL2 = "LEVEL2";
	public static final String LEVEL2_ROLES = "LEVEL2_ROLES";
	public static final String ROLE_STRING_SEPARATOR = "$";

	public static final HashMap<String, LinkedHashSet<String>> rolesByLevel1Type = new HashMap<>();
	public static final HashMap<String, HashMap<String, LinkedHashSet<String>>> rolesByLevel2Type = new HashMap<>();

	@Autowired private ReloadableInMemoryDaoImpl userDao;
	private static IModuleManager moduleManager;

	@Autowired void setModuleManager(IModuleManager modMgr) {
		moduleManager = modMgr;
		
		ResourceBundle bundle = ResourceBundle.getBundle("roles", resourceControl);
		String level1EntityTypes = "";
		try {
			level1EntityTypes = bundle.getString(LEVEL1);
		}
		catch (Exception e) {
			LOG.warn("No entity types found to manage permissions for in roles.properties (you may specify some by adding a LEVEL1 property with comma-separated strings as a value)");
		}
        Set<String> level1Types = Stream.of(level1EntityTypes.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
        for (String level1Type : level1Types) {
    		LinkedHashSet<String> levelRoles = new LinkedHashSet<String>();
        	try {
        		levelRoles.add(IRoleDefinition.ENTITY_MANAGER_ROLE);	// this one must exist even if not declared in roles.properties
        		levelRoles.addAll(moduleManager.getLevel1Roles(level1Type, bundle));
        	}
			catch (Exception e) {
				levelRoles = null;	// we don't want to support the manager role if no orthers have been defined
				LOG.warn("No roles to manage " + level1Type + " entities in roles.properties (you may specify some by adding a LEVEL1_ROLES_" + level1Type + " property with comma-separated strings as a value)");
			}
    		rolesByLevel1Type.put(level1Type, levelRoles);
        }
        
		String level2EntityTypes = "";
		try {
			level2EntityTypes = bundle.getString(LEVEL2);
		}
		catch (Exception ignored) {}
        String[] level2Types = StringUtils.tokenizeToStringArray(level2EntityTypes, ",");
        for (String level2Type : level2Types) {
        	String[] splitLevel2Type = level2Type.split("\\.");
        	if (splitLevel2Type.length != 2) { 
        		LOG.warn("Ignoring invalid level 2 entity type defined in roles.properties: '" + level2Type + "' (levels should be dot-separated)");
        		continue;
        	}
        	if (!level1Types.contains(splitLevel2Type[0])) {
        		LOG.warn("Ignoring invalid level 2 entity type defined in roles.properties: '" + level2Type + "', unknown parent level '" + splitLevel2Type[0] + "'");
        		continue;
        	}
    		LinkedHashSet<String> levelRoles = new LinkedHashSet<String>();
    		HashMap<String, LinkedHashSet<String>> roleMapForSubType = rolesByLevel2Type.get(splitLevel2Type[0]);
    		if (roleMapForSubType == null) {
    			roleMapForSubType = new HashMap<>();
    			rolesByLevel2Type.put(splitLevel2Type[0], roleMapForSubType);
    		}
    		try {
        		levelRoles.add(IRoleDefinition.ENTITY_MANAGER_ROLE);	// this one must exist even if not declared in roles.properties
        		levelRoles.addAll(Arrays.asList(StringUtils.tokenizeToStringArray(bundle.getString(LEVEL2_ROLES + "_" + level2Type), ",")));
        	}
			catch (Exception e) {
				levelRoles = null;	// we don't want to support the manager role if no orthers have been defined 
				LOG.warn("No roles to manage " + level2Type + " entities in roles.properties (you may specify some by adding a LEVEL2_ROLES_" + level2Type + " property with comma-separated strings as a value)");
			}
    		roleMapForSubType.put(splitLevel2Type[1], levelRoles);
        }
	}

    private static Control resourceControl = new ResourceBundle.Control() {
        @Override
        public boolean needsReload(String baseName, java.util.Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
            return true;
        }

        @Override
        public long getTimeToLive(String baseName, java.util.Locale locale) {
            return 0;
        }
    };

	@GetMapping(userListPageURL)
	protected ModelAndView setupList() throws Exception
	{
		return new ModelAndView();
    }

	@GetMapping(userListCountURL)
	protected @ResponseBody int countUsersByLoginLookup(@RequestParam("loginLookup") String sLoginLookup) throws Exception
	{
		return userDao.countByLoginLookup(sLoginLookup);
	}

	@GetMapping(userListDataURL)
	protected @ResponseBody Comparable[][] listUsersByLoginLookup(@RequestParam("loginLookup") String sLoginLookup, @RequestParam("page") int page, @RequestParam("size") int size) throws Exception
	{
		List<UserDetails> users = userDao.listByLoginLookup(sLoginLookup, Math.max(0, page), size);
		Comparable[][] result = new Comparable[users.size()][3];
		for (int i=0; i<users.size(); i++)
		{
			UserWithMethod ud = (UserWithMethod) users.get(i);
			String sAuthoritySummary;
			if (ud.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
				sAuthoritySummary = "(ADMINISTRATOR)";
			else
			{
				HashSet<String> modules = new HashSet<>(userDao.getSupervisedModules(ud.getAuthorities()));
				modules.addAll(userDao.getManagedEntitiesByModuleAndType(ud.getAuthorities()).keySet());
				modules.addAll(userDao.getCustomRolesByModuleAndEntityType(ud.getAuthorities()).keySet());
				sAuthoritySummary = modules.stream().collect(Collectors.joining(", "));
			}
			result[i][0] = ud.getUsername();
			result[i][1] = sAuthoritySummary;
			result[i][2] = ud.getMethod().isEmpty() ? "Local" : ud.getMethod();
		}
		return result;
	}

	@GetMapping(value = userDetailsURL)
	protected void setupForm(Model model, @RequestParam(value="user", required=false) String username)
	{
		model.addAttribute("rolesByLevel1Type", rolesByLevel1Type);

		UserDetails user = username != null ? userDao.loadUserByUsernameAndMethod(username, null) : new UserWithMethod(" ", "", new ArrayList<GrantedAuthority>(), true, "", null);
		model.addAttribute("user", user);
		Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		boolean fIsLoggedUserAdmin = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
		Map<String /*module*/, Map<String /*entity-type*/, Collection<Comparable> /*entity-IDs*/>> managedEntitiesByModuleAndType = userDao.getManagedEntitiesByModuleAndType(loggedUserAuthorities);
		Collection<String> supervisedModules = userDao.getSupervisedModules(loggedUserAuthorities);
		
		Collection<String> publicModules = new ArrayList<String>(moduleManager.getModules(true)), publicModulesWithoutOwnedEntities = new ArrayList();
		if (!fIsLoggedUserAdmin)
			for (String sModule : publicModules)	// only show modules containing entities managed by logged user, or that he is supervisor on
				if (!supervisedModules.contains(sModule) && managedEntitiesByModuleAndType.get(sModule) == null)
					publicModulesWithoutOwnedEntities.add(sModule);
		model.addAttribute("publicModules", CollectionUtils.disjunction(publicModules, publicModulesWithoutOwnedEntities));
		
		Collection<String> privateModules = new ArrayList<String>(moduleManager.getModules(false)), privateModulesWithoutOwnedEntities = new ArrayList();
		if (!fIsLoggedUserAdmin)
			for (String sModule : privateModules)	// only show modules containing entities managed by logged user
				if (!supervisedModules.contains(sModule) && managedEntitiesByModuleAndType.get(sModule) == null)
					privateModulesWithoutOwnedEntities.add(sModule);
		model.addAttribute("privateModules", CollectionUtils.disjunction(privateModules, privateModulesWithoutOwnedEntities));
	}

	@PostMapping(value = userDetailsURL)
	protected String processForm(Model model, HttpServletRequest request) throws Exception
	{
		String sUserName = request.getParameter("username"), sPassword = request.getParameter("password"), sEmail = request.getParameter("email");
		boolean fGotUserName = sUserName != null && sUserName.length() > 0;
		boolean fGotPassword = sPassword != null && sPassword.length() > 0;
		boolean fCloning = "true".equals(request.getParameter("cloning"));

		ArrayList<String> errors = new ArrayList<String>();

		UserWithMethod user = null;
		if (fGotUserName)
			try
			{	
				user = (UserWithMethod) userDao.loadUserByUsernameAndMethod(sUserName, null);
			}
			catch (UsernameNotFoundException unfe)
			{	// it's a new user, so make sure we have a password
				if (!fGotPassword && !fCloning)
					errors.add("You must specify a password");
			}

		if (!fGotPassword && user != null)
			sPassword = user.getPassword();		// password remains the same

		HashSet<String> entitiesOnWhichPermissionsWereExplicitlyApplied = new HashSet<>();
		HashSet<String> grantedAuthorityLabels = new HashSet<>();	// ensures unicity 
		
		Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		if (user != null && user.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			grantedAuthorityLabels.add(IRoleDefinition.ROLE_ADMIN);
		else
		{
		    Collection<String> modules = moduleManager.getModules(null);
		    for (String sEntityType : rolesByLevel1Type.keySet())
			{
		        Map<String, Map<Comparable, String[]>> entitiesByModule = moduleManager.getEntitiesByModule(sEntityType, null, modules, false);
		        for (String sModule : modules)
				{
					LinkedHashMap<Comparable, String[]> moduleEntities = (LinkedHashMap<Comparable, String[]>) entitiesByModule.get(sModule);
					if (moduleEntities != null)
    					for (Comparable entityId : moduleEntities.keySet()) {
    						LinkedHashSet<String> rolesForEntityType = rolesByLevel1Type.get(sEntityType);
    						if (rolesForEntityType != null)
    						for (String anEntityRole : rolesForEntityType) {
    							String sRole = urlEncode(sModule + ROLE_STRING_SEPARATOR + sEntityType + ROLE_STRING_SEPARATOR + anEntityRole + ROLE_STRING_SEPARATOR + entityId);
    							if (request.getParameter(urlEncode(sRole)) != null)
    								grantedAuthorityLabels.add(sRole);
    						}
    					}
					
					Enumeration<String> it = request.getParameterNames();
					while (it.hasMoreElements())
					{
						String param = it.nextElement();
						if (param.equals(urlEncode(sEntityType + "Permission_" + sModule)))
						{
							String val = request.getParameter(param);
							if (val.length() > 0)
							{
								for (String sRole : urlDecode(val).split(","))
								{
									String[] splitPermission = sRole.split(Pattern.quote(ROLE_STRING_SEPARATOR));
									if (moduleManager.doesEntityExistInModule(splitPermission[0], splitPermission[1], splitPermission[3]))
									{
										entitiesOnWhichPermissionsWereExplicitlyApplied.add(sModule + ROLE_STRING_SEPARATOR + sEntityType + ROLE_STRING_SEPARATOR + splitPermission[3]);
										grantedAuthorityLabels.add(sRole);
									}
									else
										LOG.debug("skipping " + sRole);
								}
							}
						}
						else
						{ // we should not be doing this for each entity type but since calling getEntitiesByModule() is expensive it's still a better option than looping first on modules, then on entities 
							String sRole = sModule + ROLE_STRING_SEPARATOR + IRoleDefinition.ROLE_DB_SUPERVISOR;							
							if (param.equals(urlEncode(sRole)))
								grantedAuthorityLabels.add(sRole);
						}
					}
				}
			}
		}

		if (user == null && !fGotUserName)
			errors.add("Username must not be empty");

		if (user != null)
		{	// make sure we don't loose permissions that are not set via this interface (i.e. roles on entities managed by other users than the connected one)
//			Map<String, Map<String, Collection<Comparable>>> managedEntitiesByModuleAndType = userDao.getOwnedEntitiesByModuleAndType(user.getAuthorities());
//			for (String sModule : managedEntitiesByModuleAndType.keySet())
//			{
//				Map<String, Collection<Comparable>> managedEntitiesByType = managedEntitiesByModuleAndType.get(sModule);
//				for (String sEntityType : managedEntitiesByType.keySet())
//					for (Comparable entityID : managedEntitiesByType.get(sEntityType))
//						grantedAuthorityLabels.add(sModule + ROLE_STRING_SEPARATOR + sEntityType + ROLE_STRING_SEPARATOR + IRoleDefinition.ENTITY_MANAGER_ROLE + ROLE_STRING_SEPARATOR + entityID);
//			}
			
			Map<String, Map<String, Map<String, Collection<Comparable>>>> customRolesByModuleAndEntityType = userDao.getCustomRolesByModuleAndEntityType(user.getAuthorities());
			for (String sModule : customRolesByModuleAndEntityType.keySet())
			{
				Map<String, Map<String, Collection<Comparable>>> rolesByEntityType = customRolesByModuleAndEntityType.get(sModule);
				for (String sEntityType : rolesByEntityType.keySet())
				{
					Map<String, Collection<Comparable>> entityIDsByRoles = rolesByEntityType.get(sEntityType);
					for (String role : entityIDsByRoles.keySet())
						for (Comparable entityId : entityIDsByRoles.get(role))
							if (!(loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))) && !(loggedUserAuthorities.contains(new SimpleGrantedAuthority(sModule + ROLE_STRING_SEPARATOR + sEntityType + ROLE_STRING_SEPARATOR + IRoleDefinition.ENTITY_MANAGER_ROLE + ROLE_STRING_SEPARATOR + entityId))) && !entitiesOnWhichPermissionsWereExplicitlyApplied.contains(sModule + ROLE_STRING_SEPARATOR + sEntityType + ROLE_STRING_SEPARATOR + entityId))
								grantedAuthorityLabels.add(sModule + ROLE_STRING_SEPARATOR + sEntityType + ROLE_STRING_SEPARATOR + role + ROLE_STRING_SEPARATOR + entityId);
				}
			}
		}
		
		if (grantedAuthorityLabels.isEmpty())
			grantedAuthorityLabels.add(IRoleDefinition.DUMMY_EMPTY_ROLE);

		if (errors.size() > 0 || (!fGotPassword && fCloning))
		{
			ArrayList<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
			for (final String sGA : grantedAuthorityLabels)
				grantedAuthorities.add(new SimpleGrantedAuthority(sGA));
			user = new UserWithMethod(fGotUserName ? sUserName : " ", "", grantedAuthorities, true, "", sEmail);
			model.addAttribute("errors", errors);
			setupForm(model, null);
			model.addAttribute("user", user);
			return userDetailsURL.substring(0, userDetailsURL.lastIndexOf("."));
		}

		userDao.saveOrUpdateUser(sUserName, sPassword, grantedAuthorityLabels.toArray(new String[grantedAuthorityLabels.size()]), true, (user == null) ? "" : user.getMethod(), sEmail);
		return "redirect:" + userListPageURL;
	}

	@GetMapping(value = userPermissionURL)
	protected void setupPermissionForm(Model model, @RequestParam("user") String username, @RequestParam("module") String module, @RequestParam("entityType") String entityType) throws Exception
	{
		model.addAttribute(module);
		model.addAttribute("roles", rolesByLevel1Type.get(entityType));
		UserDetails user = null;	// we need to support the case where the user does not exist yet
		try
		{
			user = userDao.loadUserByUsernameAndMethod(username, null);
		}
		catch (UsernameNotFoundException ignored)
		{}
		model.addAttribute("user", user);
		boolean fVisibilitySupported = moduleManager.doesEntityTypeSupportVisibility(module, entityType);
		model.addAttribute("publicEntities", moduleManager.getEntitiesByModule(entityType, fVisibilitySupported ? true : null, Arrays.asList(module), false).get(module));
		if (fVisibilitySupported)
			model.addAttribute("privateEntities", moduleManager.getEntitiesByModule(entityType, false, Arrays.asList(module), false).get(module));
	}

	@DeleteMapping(userRemovalURL)
	protected @ResponseBody boolean removeUser(@RequestParam("user") String sUserName) throws Exception
	{
		UserDetails user = userDao.loadUserByUsernameAndMethod(sUserName, null);
		if (user != null && user.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("Admin user cannot be deleted!");

		return userDao.deleteUser(sUserName);
	}

	// just a wrapper, for convenient use in JSPs
	public String urlEncode(String s) throws UnsupportedEncodingException
	{
		return URLEncoder.encode(s, "UTF-8");
	}
	
	// just a wrapper, for convenient use in JSPs
	public String urlDecode(String s) throws UnsupportedEncodingException
	{
		return URLDecoder.decode(s, "UTF-8");
	}
}
