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

	static final public String DTO_FIELDNAME_HOST = "host";
	static final public String DTO_FIELDNAME_PUBLIC = "public";
	static final public String DTO_FIELDNAME_HIDDEN = "hidden";
	
	static final public String mainPageURL = "/" + FRONTEND_URL + "/main.do_";
	static final public String homePageURL = "/" + FRONTEND_URL + "/home.do_";
	static final public String topFrameURL = "/" + FRONTEND_URL + "/topBanner.do_";
	static final public String moduleFieldsURL = "/" + FRONTEND_URL + "/moduleFields.json_";
	static final public String moduleListPageURL = "/" + FRONTEND_URL + "/ModuleList.do_";
	static final public String moduleListDataURL = "/" + FRONTEND_URL + "/listModules.json_";
	static final public String moduleRemovalURL = "/" + FRONTEND_URL + "/removeModule.json_";
	static final public String moduleCreationURL = "/" + FRONTEND_URL + "/createModule.json_";
	static final public String moduleFieldModificationURL = "/" + FRONTEND_URL + "/moduleFieldModification.json_";
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
		if (publicEntities == null)
			publicEntities = new HashMap<>();
		Map<Comparable, String> privateEntities = fVisibilitySupported ? moduleManager.getEntitiesByModule(entityType, false).get(module) : new HashMap<>();
		if (privateEntities == null)
			privateEntities = new HashMap<>();
		
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
	protected @ResponseBody Collection<Object> listModules() throws Exception
	{
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
//		Collection<Object> modulesToManage;
		if (authToken.getAuthorities().contains(new GrantedAuthorityImpl(IRoleDefinition.ROLE_ADMIN)))
			return moduleManager.getModulesByVisibility(null);
		else {
			Collection<String> moduleNamesToManage = userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).keySet();
			return moduleManager.getModulesByNames(moduleNamesToManage);
		}

//		Map<String, Map<String, Comparable>> result = new TreeMap<>();
//		
//		Collection<Object> publicModules = moduleManager.getModulesByVisibility(true);
//		for (String module : modulesToManage) {
//			Map<String, Comparable> aModuleEntry = new HashMap<>();
//			aModuleEntry.put(DTO_FIELDNAME_HOST, moduleManager.getModuleHost(module));
//			aModuleEntry.put(DTO_FIELDNAME_PUBLIC, publicModules.contains(module));
//			aModuleEntry.put(DTO_FIELDNAME_HIDDEN, moduleManager.isModuleHidden(module));
//			for (String field : getEditableModuleFields().keySet())
//				aModuleEntry.put(field, publicModules.contains(module));
//			result.put(module, aModuleEntry);
//		}
//		return result;
	}
	
	@RequestMapping(moduleFieldsURL)
	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
	protected @ResponseBody Map<String, String> getEditableModuleFields() throws Exception
	{
		return moduleManager.getEditableModuleFields();
	}

	@RequestMapping(moduleFieldModificationURL)
	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
	protected @ResponseBody boolean modifyModuleFields(HttpServletRequest request, @RequestParam("module") String sModule, @RequestParam("public") boolean fPublic, @RequestParam("hidden") boolean fHidden) throws Exception
	{
		HashMap<String, Object> editableFieldValues = new HashMap<>();
		for (String field : getEditableModuleFields().keySet())
			editableFieldValues.put(field, request.getParameter(field));
		return moduleManager.updateDataSource(sModule, fPublic, fHidden, editableFieldValues);
	}
	
	@RequestMapping(moduleCreationURL)
	@PreAuthorize("hasRole(IRoleDefinition.ROLE_ADMIN)")
	protected @ResponseBody Object createModule(@RequestParam("module") String sModule, @RequestParam("host") String sHost) throws Exception
	{
		return moduleManager.createDataSource(sModule, sHost, false, false, null, null);
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

	public static String determinePublicHostName(HttpServletRequest request) throws SocketException, UnknownHostException {
		int nPort = request.getServerPort();
		String sHostName = request.getHeader("X-Forwarded-Server"); // in case the app is running behind a proxy
		if (sHostName == null)
			sHostName = request.getServerName();
	
		// see if we can get this from the referer
		String sReferer = request.getHeader("referer");
		if (sReferer != null) {
			int nPos = sReferer.indexOf("://" + sHostName + request.getContextPath() + "/");
			if (nPos != -1) {
				sHostName = sReferer.substring(0, nPos) + "://" + sHostName;
				LOG.debug("From referer header, determinePublicHostName is returning " + sHostName);
				return sHostName;
			}
		}

		if ("localhost".equalsIgnoreCase(sHostName) || "127.0.0.1".equals(sHostName)) // we need a *real* address for remote applications to be able to reach us
			sHostName = tryAndFindVisibleIp(request);
		sHostName = "http" + (request.isSecure() ? "s" : "") + "://" + sHostName + (nPort != 80 ? ":" + nPort : "");
		LOG.debug("After scanning network interfaces, determinePublicHostName is returning " + sHostName);
		return sHostName;
	}

	private static String tryAndFindVisibleIp(HttpServletRequest request) throws SocketException, UnknownHostException {
		String sHostName = null;
		HashMap<InetAddress, String> inetAddressesWithInterfaceNames = getInetAddressesWithInterfaceNames();
        for (InetAddress addr : inetAddressesWithInterfaceNames.keySet()) {
            LOG.debug("address found for local machine: " + addr /*+ " / " + addr.isAnyLocalAddress() + " / " + addr.isLinkLocalAddress() + " / " + addr.isLoopbackAddress() + " / " + addr.isMCLinkLocal() + " / " + addr.isMCNodeLocal() + " / " + addr.isMCOrgLocal() + " / " + addr.isMCSiteLocal() + " / " + addr.isMulticastAddress() + " / " + addr.isSiteLocalAddress() + " / " + addr.isMCGlobal()*/);
            String hostAddress = addr.getHostAddress().replaceAll("/", "");
            if (!hostAddress.startsWith("127.0.") && hostAddress.split("\\.").length >= 4)
            {
            	sHostName = hostAddress;
            	if (!addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && !addr.isSiteLocalAddress() && !inetAddressesWithInterfaceNames.get(addr).toLowerCase().startsWith("wl"))
           			break;	// otherwise we will keep searching in case we find an ethernet network
            }
        }
        if (sHostName == null)
        	throw new UnknownHostException("Unable to convert local address to visible IP");
        return sHostName;
    }

	public static HashMap<InetAddress, String> getInetAddressesWithInterfaceNames() throws SocketException {
		HashMap<InetAddress, String> result = new HashMap<>();
		Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
        for (; niEnum.hasMoreElements();)
        {
            NetworkInterface ni = niEnum.nextElement();
            Enumeration<InetAddress> a = ni.getInetAddresses();
            for (; a.hasMoreElements();)
            	result.put(a.nextElement(), ni.getDisplayName());
        }
		return result;
	}
}