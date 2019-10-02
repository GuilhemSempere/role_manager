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
package fr.cirad.web.controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.base.IModuleManager;
import fr.cirad.security.base.IRoleDefinition;
import fr.cirad.web.controller.security.UserPermissionController;

@Controller
public class BackOfficeController {

	private static final Logger LOG = Logger.getLogger(BackOfficeController.class);
	
	static final public String FRONTEND_URL = "private";

	static final public String mainPageURL = "/" + FRONTEND_URL + "/main.do_";
	static final public String homePageURL = "/" + FRONTEND_URL + "/home.do_";
	static final public String topFrameURL = "/" + FRONTEND_URL + "/topBanner.do_";
	static final public String moduleListPageURL = "/" + FRONTEND_URL + "/ModuleList.do_";
	static final public String moduleListDataURL = "/" + FRONTEND_URL + "/listModules.json_";
	static final public String moduleRemovalURL = "/" + FRONTEND_URL + "/removeModule.json_";
	static final public String moduleCreationURL = "/" + FRONTEND_URL + "/createModule.json_";
	static final public String moduleVisibilityURL = "/" + FRONTEND_URL + "/moduleVisibility.json_";
	static final public String moduleContentPageURL = "/" + FRONTEND_URL + "/ModuleContents.do_";
	static final public String moduleEntityRemovalURL = "/" + FRONTEND_URL + "/removeModuleEntity.json_";
	static final public String moduleEntityVisibilityURL = "/" + FRONTEND_URL + "/entityVisibility.json_";
    static final public String hostListURL = "/" + FRONTEND_URL + "/hosts.json_";

	@Autowired private IModuleManager moduleManager;
	@Autowired private ReloadableInMemoryDaoImpl userDao;
	
	@RequestMapping(mainPageURL)
	protected ModelAndView mainPage(HttpSession session) throws Exception
	{
		ModelAndView mav = new ModelAndView();
		return mav;
	}

	@RequestMapping(homePageURL)
	public ModelAndView homePage()
	{
		ModelAndView mav = new ModelAndView();
		return mav;
	}

	@RequestMapping(topFrameURL)
	protected ModelAndView topFrame()
	{
		ModelAndView mav = new ModelAndView();
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		mav.addObject("loggedUser", authToken != null && !authToken.getAuthorities().contains(new GrantedAuthorityImpl ("ROLE_ANONYMOUS")) ? authToken.getPrincipal() : null);
		return mav;
	}
	
	@RequestMapping(moduleListPageURL)
	public ModelAndView setupList()
	{
		ModelAndView mav = new ModelAndView(); 
		mav.addObject("rolesByLevel1Type", UserPermissionController.rolesByLevel1Type);
		return mav;
	}
	
	@RequestMapping(moduleContentPageURL)
	public void moduleContentPage(Model model, @RequestParam("user") String username, @RequestParam("module") String module, @RequestParam("entityType") String entityType)
	{
		model.addAttribute(module);
		model.addAttribute("roles", UserPermissionController.rolesByLevel1Type.get(entityType));
		UserDetails user = null;	// we need to support the case where the user does not exist yet
		try
		{
			user = userDao.loadUserByUsername(username);
		}
		catch (UsernameNotFoundException ignored)
		{}
		model.addAttribute("user", user);
		
		boolean fVisibilitySupported = moduleManager.doesEntityTypeSupportVisibility(module, entityType);
		model.addAttribute("visibilitySupported", fVisibilitySupported);
		Map<Comparable, String> publicEntities = moduleManager.getEntitiesByModule(entityType, fVisibilitySupported ? true : null).get(module);
		Map<Comparable, String> privateEntities = fVisibilitySupported ? moduleManager.getEntitiesByModule(entityType, false).get(module) : new HashMap<>();

		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<Comparable> allowedEntities = authToken.getAuthorities().contains(new GrantedAuthorityImpl(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).get(module).get(entityType);
		for (Map<Comparable, String> entityMap : Arrays.asList(publicEntities, privateEntities))
		{
			Map<Comparable, String> allowedEntityMap = new TreeMap<Comparable, String>();
			for (Comparable key : entityMap.keySet())
				if (allowedEntities == null || allowedEntities.contains(key))
					allowedEntityMap.put(key, entityMap.get(key));
			model.addAttribute((publicEntities == entityMap ? "public" : "private") + "Entities", allowedEntityMap);
		}
	}

	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
    @RequestMapping(hostListURL)
	protected @ResponseBody Collection<String> getHostList() throws IOException {
    	return moduleManager.getHosts();
    }

	@RequestMapping(moduleListDataURL)
	protected @ResponseBody Map<String, Boolean[]> listModules() throws Exception
	{
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<String> modulesToManage;
		if (authToken.getAuthorities().contains(new GrantedAuthorityImpl(IRoleDefinition.ROLE_ADMIN)))
			modulesToManage = moduleManager.getModules(null);
		else
			modulesToManage = userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).keySet();

		Collection<String> publicModules = moduleManager.getModules(true);
		Map<String, Boolean[]> result = new TreeMap<>();
		for (String module : modulesToManage)
			result.put(module, new Boolean[] {publicModules.contains(module), moduleManager.isModuleHidden(module)});
		return result;
	}
	
	@RequestMapping(moduleVisibilityURL)
	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
	protected @ResponseBody boolean modifyModuleVisibility(@RequestParam("module") String sModule, @RequestParam("public") boolean fPublic, @RequestParam("hidden") boolean fHidden) throws Exception
	{
		return moduleManager.updateDataSource(sModule, fPublic, fHidden, null);
	}
	
	@RequestMapping(moduleCreationURL)
	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
	protected @ResponseBody boolean createModule(@RequestParam("module") String sModule, @RequestParam("host") String sHost) throws Exception
	{
		return moduleManager.createDataSource(sModule, sHost, null, null);
	}
	
	@RequestMapping(moduleRemovalURL)
	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
	protected @ResponseBody boolean removeModule(@RequestParam("module") String sModule) throws Exception
	{
		return moduleManager.removeDataSource(sModule, true);
	}

	@RequestMapping(moduleEntityRemovalURL)
	protected @ResponseBody boolean removeModuleEntity(@RequestParam("module") String sModule, @RequestParam("entityType") String sEntityType, @RequestParam("entityId") String sEntityId) throws Exception
	{
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<Comparable> allowedEntities = authToken.getAuthorities().contains(new GrantedAuthorityImpl(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).get(sModule).get(sEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(sEntityId))
			throw new Exception("You are not allowed to remove this " + sEntityType);
		
		return moduleManager.removeManagedEntity(sModule, sEntityType, sEntityId);
	}
	
	@RequestMapping(moduleEntityVisibilityURL)
	protected @ResponseBody boolean modifyModuleEntityVisibility(@RequestParam("module") String sModule, @RequestParam("entityType") String sEntityType, @RequestParam("entityId") String sEntityId, @RequestParam("public") boolean fPublic) throws Exception
	{
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<Comparable> allowedEntities = authToken.getAuthorities().contains(new GrantedAuthorityImpl(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).get(sModule).get(sEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(sEntityId))
			throw new Exception("You are not allowed to modify this " + sEntityType);
		
		return moduleManager.setManagedEntityVisibility(sModule, sEntityType, sEntityId, fPublic);
	}

//	protected ArrayList<String> listAuthorisedModules()
//	{
//		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
//		Collection<? extends GrantedAuthority> authorities = authToken == null ? null : authToken.getAuthorities();
//
//		ArrayList<String> authorisedModules = new ArrayList<String>();
//		for (String sAModule : moduleManager.getModules())
//			if (authorities == null || authorities.contains(new GrantedAuthorityImpl(IRoleDefinition.TOPLEVEL_ROLE_PREFIX + UserPermissionController.ROLE_STRING_SEPARATOR + sAModule)) || authorities.contains(new GrantedAuthorityImpl(IRoleDefinition.ROLE_ADMIN)))
//				authorisedModules.add(sAModule);
//
//		return authorisedModules;
//    }

	public static String determinePublicHostName(HttpServletRequest request) throws UnknownHostException, SocketException {
		int nPort = request.getServerPort();
		String sHostName = request.getHeader("X-Forwarded-Server"); // in case the app is running behind a proxy
		if (sHostName == null)
		{
			sHostName = request.getServerName();
			if ("localhost".equalsIgnoreCase(sHostName) || "127.0.0.1".equals(sHostName)) // we need a *real* address for the cluster to be able to pick up input files
			{
		        Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
		        mainLoop : for (; niEnum.hasMoreElements();)
		        {
	                NetworkInterface ni = niEnum.nextElement();
	                Enumeration<InetAddress> a = ni.getInetAddresses();
	                for (; a.hasMoreElements();)
	                {
                        InetAddress addr = a.nextElement();
//                        LOG.debug("address found for local machine: " + addr);
                        String hostAddress = addr.getHostAddress().replaceAll("/", "");
                        if (!hostAddress.startsWith("127.0.") && hostAddress.split("\\.").length >= 4)
                        {
                        	sHostName = hostAddress;
                        	if (!addr.isSiteLocalAddress() && !ni.getDisplayName().toLowerCase().startsWith("wlan"))
                        		break mainLoop;	// otherwise we will keep searching in case we find an ethernet network
                        }
	                }
		        }
		        if (sHostName == null)
		        	LOG.error("Unable to convert local address to internet IP");
		    }
			sHostName += nPort != 80 ? ":" + nPort : "";
		}
		LOG.debug("returning http" + (request.isSecure() ? "s" : "") + "://" + sHostName);
		return "http" + (request.isSecure() ? "s" : "") + "://" + sHostName;
	}
}
