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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.base.IModuleManager;
import fr.cirad.security.base.IRoleDefinition;
import fr.cirad.security.dump.DumpManager;
import fr.cirad.security.dump.DumpMetadata;
import fr.cirad.security.dump.DumpValidity;
import fr.cirad.security.dump.IBackgroundProcess;
import fr.cirad.security.dump.ProcessStatus;
import fr.cirad.web.controller.security.UserPermissionController;

@Controller
public class BackOfficeController {

	private static final Logger LOG = Logger.getLogger(BackOfficeController.class);
	
	static final public String FRONTEND_URL = "private";

	static final public String DTO_FIELDNAME_HOST = "host";
	static final public String DTO_FIELDNAME_SIZE = "size";
	static final public String DTO_FIELDNAME_PUBLIC = "public";
	static final public String DTO_FIELDNAME_HIDDEN = "hidden";
	static final public String DTO_FIELDNAME_DUMPSTATUS = "dumpStatus";
	
	static final public String mainPageURL = "/" + FRONTEND_URL + "/main.do_";
	static final public String homePageURL = "/" + FRONTEND_URL + "/home.do_";
	static final public String topFrameURL = "/" + FRONTEND_URL + "/topBanner.do_";
	static final public String adminMenuURL = "/" + FRONTEND_URL + "/AdminMenu.do_";
	static final public String moduleListPageURL = "/" + FRONTEND_URL + "/ModuleList.do_";
	static final public String moduleListDataURL = "/" + FRONTEND_URL + "/listModules.json_";
	static final public String moduleRemovalURL = "/" + FRONTEND_URL + "/removeModule.json_";
	static final public String moduleCreationURL = "/" + FRONTEND_URL + "/createModule.json_";
	static final public String moduleVisibilityURL = "/" + FRONTEND_URL + "/moduleVisibility.json_";
	static final public String moduleContentPageURL = "/" + FRONTEND_URL + "/ModuleContents.do_";
	static final public String moduleEntityRemovalURL = "/" + FRONTEND_URL + "/removeModuleEntity.json_";
	static final public String moduleEntityVisibilityURL = "/" + FRONTEND_URL + "/entityVisibility.json_";
    static final public String hostListURL = "/" + FRONTEND_URL + "/hosts.json_";
    
    static final public String moduleDumpInfoURL = "/" + FRONTEND_URL + "/moduleDumpInfo.json_";
    static final public String newDumpURL = "/" + FRONTEND_URL + "/newDump.do_";
    static final public String restoreDumpURL = "/" + FRONTEND_URL + "/restoreDump.do_";
    static final public String dumpStatusPageURL = "/" + FRONTEND_URL + "/dumpStatus.do_";
    static final public String dumpStatusQueryURL = "/" + FRONTEND_URL + "/dumpProgress.json_";
    static final public String processListPageURL = "/" + FRONTEND_URL + "/processList.do_";
    static final public String processListStatusURL = "/" + FRONTEND_URL + "/processListStatus.json_";
    static final public String abortProcessURL = "/" + FRONTEND_URL + "/abortProcess.json_";
    static final public String deleteDumpURL = "/" + FRONTEND_URL + "/deleteDump.json_";

	@Autowired private IModuleManager moduleManager;
	@Autowired private ReloadableInMemoryDaoImpl userDao;
	@Autowired private DumpManager dumpManager;
	
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
		mav.addObject("loggedUser", authToken != null && !authToken.getAuthorities().contains(new SimpleGrantedAuthority ("ROLE_ANONYMOUS")) ? authToken.getPrincipal() : null);
		return mav;
	}
	
	@RequestMapping(adminMenuURL)
	protected ModelAndView adminMenu()
	{
		ModelAndView mav = new ModelAndView();
		mav.addObject("actionRequiredToEnableDumps", moduleManager.getActionRequiredToEnableDumps());
		return mav;
	}
	
	@RequestMapping(moduleListPageURL)
	public ModelAndView setupList()
	{
		ModelAndView mav = new ModelAndView(); 
		mav.addObject("rolesByLevel1Type", UserPermissionController.rolesByLevel1Type);
		mav.addObject("actionRequiredToEnableDumps", moduleManager.getActionRequiredToEnableDumps());
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
			user = userDao.loadUserByUsernameAndMethod(username, null);
		}
		catch (UsernameNotFoundException ignored)
		{}
		model.addAttribute("user", user);
		
		boolean fVisibilitySupported = moduleManager.doesEntityTypeSupportVisibility(module, entityType);
		model.addAttribute("visibilitySupported", fVisibilitySupported);
		Map<Comparable, String> publicEntities = moduleManager.getEntitiesByModule(entityType, fVisibilitySupported ? true : null).get(module);
		Map<Comparable, String> privateEntities = fVisibilitySupported ? moduleManager.getEntitiesByModule(entityType, false).get(module) : new HashMap<>();

		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<Comparable> allowedEntities = (authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).get(module).get(entityType));
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
	protected @ResponseBody Map<String, Map<String, Comparable>> listModules() throws Exception
	{
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<String> modulesToManage;
		if (authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			modulesToManage = moduleManager.getModules(null);
		else
			modulesToManage = userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).keySet();

		Map<String, Map<String, Comparable>> result = new ConcurrentSkipListMap<>();
		
		Collection<String> publicModules = moduleManager.getModules(true);
		
		int nNumProc = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = new ThreadPoolExecutor(1, nNumProc * 3, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(nNumProc * 6), new ThreadPoolExecutor.CallerRunsPolicy());
		for (String module : modulesToManage)
		    executor.execute(new Thread() {
		        public void run() {
        			Map<String, Comparable> aModuleEntry = new HashMap<>();
        			aModuleEntry.put(DTO_FIELDNAME_SIZE, (Comparable) moduleManager.getModuleSizeMb(module));
        			aModuleEntry.put(DTO_FIELDNAME_HOST, moduleManager.getModuleHost(module));
        			aModuleEntry.put(DTO_FIELDNAME_PUBLIC, publicModules.contains(module));
        			aModuleEntry.put(DTO_FIELDNAME_HIDDEN, moduleManager.isModuleHidden(module));
        			if (moduleManager.getActionRequiredToEnableDumps().isEmpty())
        				aModuleEntry.put(DTO_FIELDNAME_DUMPSTATUS, moduleManager.getDumpStatus(module));
        			result.put(module, aModuleEntry);
		        }
            });
		executor.shutdown();
		executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
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
		Collection<Comparable> allowedEntities = authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).get(sModule).get(sEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(sEntityId))
			throw new Exception("You are not allowed to remove this " + sEntityType);
		
		return moduleManager.removeManagedEntity(sModule, sEntityType, sEntityId);
	}
	
	@RequestMapping(moduleEntityVisibilityURL)
	protected @ResponseBody boolean modifyModuleEntityVisibility(@RequestParam("module") String sModule, @RequestParam("entityType") String sEntityType, @RequestParam("entityId") String sEntityId, @RequestParam("public") boolean fPublic) throws Exception
	{
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		Collection<Comparable> allowedEntities = authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getManagedEntitiesByModuleAndType(authToken.getAuthorities()).get(sModule).get(sEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(sEntityId))
			throw new Exception("You are not allowed to modify this " + sEntityType);
		
		return moduleManager.setManagedEntityVisibility(sModule, sEntityType, sEntityId, fPublic);
	}
	
	@GetMapping(moduleDumpInfoURL)
	protected @ResponseBody Map<String, Object> getModuleDumpInfo(@RequestParam("module") String sModule) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to access dump data");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		List<DumpMetadata> dumps = moduleManager.getDumps(sModule);
		dumps.sort((dump1, dump2) -> dump2.getCreationDate().compareTo(dump1.getCreationDate()));
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("dumps", dumps);
		result.put("locked", !moduleManager.isModuleAvailableForDump(sModule));
		return result;
	}

	@GetMapping(newDumpURL)
	protected String startDumpProcess(@RequestParam("module") String sModule, @RequestParam("name") String sName, @RequestParam("description") String sDescription) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to create new dumps");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");
		
		if (!moduleManager.isModuleAvailableForDump(sModule))
			throw new Exception("The module is already busy, dump operation impossible");
		
		String dumpName = sName.replaceAll("(_|[^\\w-])+", "_");
		if (dumpName.matches("_*"))
			dumpName = "dump_" + sModule + "_" + DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss").format(LocalDateTime.now());
		String processID = dumpManager.startDumpProcess(sModule, dumpName, sDescription, authToken);
		return "redirect:" + dumpStatusPageURL + "?processID=" + processID;
	}
	
	@GetMapping(restoreDumpURL)
	protected String startRestoreProcess(@RequestParam("module") String sModule, @RequestParam("dump") String sDump, @RequestParam("drop") boolean drop) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to restore dumps");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		if (!moduleManager.isModuleAvailableForDump(sModule))
			throw new Exception("The module is already busy, dump operation impossible");
		
		String processID = dumpManager.startRestoreProcess(sModule, sDump, drop, authToken);
		return "redirect:" + dumpStatusPageURL + "?processID=" + processID;
	}
	
	@GetMapping(dumpStatusPageURL)
	protected ModelAndView dumpStatusPage(@RequestParam("processID") String processID) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to access dump status");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		IBackgroundProcess process = dumpManager.getProcess(processID);
		
		ModelAndView mav = new ModelAndView("private/dumpStatus");
		mav.addObject("processID", processID);
		mav.addObject("abortable", process == null ? false : process.isAbortable());
		mav.addObject("module", process.getModule());
		mav.addObject("abortWarning", process.getAbortWarning());
		return mav;
	}
	
	@GetMapping(dumpStatusQueryURL)
	protected @ResponseBody Map<String, Object> dumpStatusQuery(@RequestParam("processID") String processID, @RequestParam(name="logStart", required=false) Integer logStart) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to access dump status");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		IBackgroundProcess process = dumpManager.getProcess(processID);
		
		Map<String, Object> result = new HashMap<String, Object>();
		if (process == null) {
			result.put("processID", processID);
			result.put("status", ProcessStatus.INEXISTANT.label);
			result.put("message", "The requested process was not found. Either the supplied processID is wrong, or the process has finished and has been deleted");
			result.put("log", "");
		} else {
			String log = process.getLog();
			if (logStart != null && logStart < log.length())
				log = log.substring(logStart);
			else
				result.put("resetLog", true);
			result.put("processID", processID);
			result.put("status", process.getStatus().label);
			result.put("message", process.getStatusMessage());
			result.put("log", log);
			result.put("warning", null);
			if (process.getStatus() == ProcessStatus.SUCCESS && processID.contains("restore")) {
				List<DumpMetadata> dumps = moduleManager.getDumps(process.getModule());
				for (DumpMetadata dump : dumps) {
					if (dump.getValidity() == DumpValidity.DIVERGED) {
						result.put("warning", "Some dumps in this module are more recent than the one that has been restored. Remember to delete them if they are undesirable.");
						break;
					}
				}
			}
		}
		
		return result;
	}
	
	@GetMapping(processListPageURL)
	protected ModelAndView processListPage() throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to access dump status");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		ModelAndView mav = new ModelAndView();
		return mav;
	}
	
	@GetMapping(processListStatusURL)
	protected @ResponseBody List<Map<String, String>> processListStatus() throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to access dump status");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		Map<String, IBackgroundProcess> processes = dumpManager.getProcesses();
		List<String> orderedIds = new ArrayList<String>(processes.keySet());
		Collections.sort(orderedIds);
		
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		for (String processID : orderedIds) {
			IBackgroundProcess process = processes.get(processID);
			Map<String, String> item = new HashMap<String, String>();
			item.put("processID", processID);
			item.put("status", process.getStatus().label);
			item.put("message", process.getStatusMessage());
			result.add(item);
		}
		
		return result;
	}
	
	@GetMapping(abortProcessURL)
	protected @ResponseBody Map<String, Boolean> abortProcess(@RequestParam("processID") String processID) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to abort processes");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		result.put("done", dumpManager.abortProcess(processID));
		return result;
	}
	
	@DeleteMapping(deleteDumpURL)
	protected @ResponseBody Map<String, Boolean> deleteDump(@RequestParam("module") String module, @RequestParam("dump") String dump) throws Exception {
		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
		if (!authToken.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			throw new Exception("You are not allowed to abort processes");
		
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		result.put("done", moduleManager.deleteDump(module, dump));
		return result;
	}
	
//	protected ArrayList<String> listAuthorisedModules()
//	{
//		Authentication authToken = SecurityContextHolder.getContext().getAuthentication();
//		Collection<? extends GrantedAuthority> authorities = authToken == null ? null : authToken.getAuthorities();
//
//		ArrayList<String> authorisedModules = new ArrayList<String>();
//		for (String sAModule : moduleManager.getModules())
//			if (authorities == null || authorities.contains(new SimpleGrantedAuthority(IRoleDefinition.TOPLEVEL_ROLE_PREFIX + UserPermissionController.ROLE_STRING_SEPARATOR + sAModule)) || authorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
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