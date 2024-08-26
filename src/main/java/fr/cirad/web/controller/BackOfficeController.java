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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fr.cirad.manager.dump.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import fr.cirad.manager.AbstractProcess;
import fr.cirad.manager.IBackgroundProcess;
import fr.cirad.manager.IModuleManager;
import fr.cirad.manager.ProcessStatus;
import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.base.IRoleDefinition;
import fr.cirad.security.service.RoleService;
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
	static final public String moduleEntityInfoURL = "/" + FRONTEND_URL + "/moduleEntityInfo.json_";
	static final public String moduleEntityRemovalURL = "/" + FRONTEND_URL + "/removeModuleEntity.json_";
	static final public String moduleEntityVisibilityUpdateUrl = "/" + FRONTEND_URL + "/entityVisibility.json_";
	static final public String moduleEntityDescriptionUpdateUrl = "/" + FRONTEND_URL + "/entityDescUpdate.json_";
    static final public String hostListURL = "/" + FRONTEND_URL + "/hosts.json_";

    static final public String moduleDumpInfoURL = "/" + FRONTEND_URL + "/moduleDumpInfo.json_";
    static final public String newDumpURL = "/" + FRONTEND_URL + "/newDump.do_";
    static final public String restoreDumpURL = "/" + FRONTEND_URL + "/restoreDump.do_";
    static final public String dumpRestoreWarningURL = "/" + FRONTEND_URL + "/dumpRestoreWarning.do_";
    static final public String dumpStatusPageURL = "/" + FRONTEND_URL + "/dumpStatus.do_";
    static final public String dumpStatusQueryURL = "/" + FRONTEND_URL + "/dumpProgress.json_";
    static final public String processListPageURL = "/" + FRONTEND_URL + "/processList.do_";
    static final public String processListStatusURL = "/" + FRONTEND_URL + "/processListStatus.json_";
    static final public String abortProcessURL = "/" + FRONTEND_URL + "/abortProcess.json_";
    static final public String deleteDumpURL = "/" + FRONTEND_URL + "/deleteDump.json_";
    static final public String moduleDumpDownloadURL = "/" + FRONTEND_URL + "/moduleDumpDownload.json_";
    static final public String moduleDumpLogDownloadURL = "/" + FRONTEND_URL + "/moduleDumpLogDownload.json_";

	@Autowired private IModuleManager moduleManager;
	@Autowired private ReloadableInMemoryDaoImpl userDao;
	@Autowired private DumpManager dumpManager;
	@Autowired private RoleService roleService;

	@GetMapping(mainPageURL)
	protected ModelAndView mainPage(HttpSession session) throws Exception
	{
		ModelAndView mav = new ModelAndView();
		return mav;
	}

	@GetMapping(homePageURL)
	public ModelAndView homePage()
	{
		ModelAndView mav = new ModelAndView();
		return mav;
	}

	@GetMapping(topFrameURL)
	protected ModelAndView topFrame()
	{
		ModelAndView mav = new ModelAndView();
		return mav;
	}

	@GetMapping(adminMenuURL)
	protected ModelAndView adminMenu()
	{
		ModelAndView mav = new ModelAndView();
		mav.addObject("actionRequiredToEnableDumps", moduleManager.getActionRequiredToEnableDumps());
		return mav;
	}

	@GetMapping(moduleListPageURL)
	public ModelAndView setupList()
	{
		ModelAndView mav = new ModelAndView();
		mav.addObject("rolesByLevel1Type", UserPermissionController.rolesByLevel1Type);
		mav.addObject("actionRequiredToEnableDumps", moduleManager.getActionRequiredToEnableDumps());
		return mav;
	}

	@GetMapping(moduleContentPageURL)
	public void moduleContentPage(Model model, @RequestParam("user") String username, @RequestParam("module") String module, @RequestParam("entityType") String entityType) throws Exception
	{
		model.addAttribute(module);
		model.addAttribute("roles", UserPermissionController.rolesByLevel1Type.get(entityType));
		HashMap<String, LinkedHashSet<String>> subEntityTypeToRolesMap = UserPermissionController.rolesByLevel2Type.get(entityType);
		if (subEntityTypeToRolesMap == null)
			subEntityTypeToRolesMap = new HashMap<>();
		model.addAttribute("subEntityTypes", subEntityTypeToRolesMap.keySet());

		UserDetails user = null;	// we need to support the case where the user does not exist yet
		try
		{
			user = userDao.loadUserByUsername(username);
		}
		catch (UsernameNotFoundException ignored)
		{}
		model.addAttribute("user", user);

		boolean fVisibilitySupported = moduleManager.doesEntityTypeSupportVisibility(module, entityType), fDescriptionSupported = moduleManager.isInlineDescriptionUpdateSupportedForEntity(entityType);
		model.addAttribute("visibilitySupported", fVisibilitySupported);
		model.addAttribute("descriptionSupported", fDescriptionSupported);
		String entityEditionUrl = moduleManager.getEntityEditionURL(entityType), entityAdditionUrl = moduleManager.getEntityAdditionURL(entityType);
		if (entityEditionUrl != null)
			model.addAttribute("entityEditionUrl", entityEditionUrl);
		if (entityAdditionUrl != null)
			model.addAttribute("entityAdditionUrl", entityAdditionUrl);

		Map<Comparable, String[]> publicEntities = moduleManager.getEntitiesByModule(entityType, fVisibilitySupported ? true : null, Arrays.asList(module), fDescriptionSupported).get(module);
		Map<Comparable, String[]> privateEntities = fVisibilitySupported ? moduleManager.getEntitiesByModule(entityType, false, Arrays.asList(module), fDescriptionSupported).get(module) : new HashMap<>();

		Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		Collection<Comparable> allowedEntities = null;
		if (!loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) && !userDao.getSupervisedModules(loggedUserAuthorities).contains(module))
		    allowedEntities = userDao.getManagedEntitiesByModuleAndType(loggedUserAuthorities).get(module).get(entityType);
		
		Map<Comparable /*main entity ID */, Map<String /* sub entity type */, Map<Comparable /* sub entity ID */, String /* sub entity name */>>> subEntityMap = new HashMap<>();
		for (Map<Comparable, String[]> entityMap : Arrays.asList(publicEntities, privateEntities))
			if (entityMap != null) {
				Map<Comparable, String[]> allowedEntityMap = new TreeMap<>();
				
				for (Comparable key : entityMap.keySet()) {
					if (allowedEntities == null || allowedEntities.contains(key))
						allowedEntityMap.put(key, entityMap.get(key));
					
					for (String subEntityType : subEntityTypeToRolesMap.keySet()) {
						Map<Comparable, String> subEntities = moduleManager.getSubEntities(entityType + "." + subEntityType, module, new Comparable[] {key});
						if (subEntities != null && !subEntities.isEmpty()) {
							Map<String, Map<Comparable, String>> subEntitiesForCurrentMainEntity = subEntityMap.get(key);
							if (subEntitiesForCurrentMainEntity == null) {
								subEntitiesForCurrentMainEntity = new HashMap<>();
								subEntityMap.put(key, subEntitiesForCurrentMainEntity);
							}
							subEntitiesForCurrentMainEntity.put(subEntityType, subEntities);
						}
					}
				}
				model.addAttribute((publicEntities == entityMap ? "public" : "private") + "Entities", allowedEntityMap);
			}
		model.addAttribute("subEntities", subEntityMap);
	}

	@PreAuthorize("@roleService.hasAdminRole(authentication)")
	@GetMapping(hostListURL)
	protected @ResponseBody Collection<String> getHostList() throws IOException {
    	return moduleManager.getHosts();
    }

	@GetMapping(moduleListDataURL)
	protected @ResponseBody Map<String, Map<String, Comparable>> listModules() throws Exception
	{
		Collection<? extends GrantedAuthority> authorities = userDao.getLoggedUserAuthorities();
		Collection<String> modulesToManage;
		if (authorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)))
			modulesToManage = moduleManager.getModules(null);
		else
			modulesToManage = CollectionUtils.union(userDao.getSupervisedModules(authorities), userDao.getManagedEntitiesByModuleAndType(authorities).keySet());

		Map<String, Map<String, Comparable>> result = new ConcurrentSkipListMap<>();

		Collection<String> publicModules = moduleManager.getModules(true);

		int nNumProc = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = new ThreadPoolExecutor(1, nNumProc * 3, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(nNumProc * 6), new ThreadPoolExecutor.CallerRunsPolicy());
		for (String module : modulesToManage)
		    executor.execute(new Thread() {
		        public void run() {
        			Map<String, Comparable> aModuleEntry = new HashMap<>();
        			double storageSize = moduleManager.getModuleSize(module);
        			if (storageSize > 0)
        			    aModuleEntry.put(DTO_FIELDNAME_SIZE, (Comparable) storageSize);
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

	@GetMapping(moduleVisibilityURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected @ResponseBody boolean modifyModuleVisibility(HttpServletRequest request, @RequestParam String module, @RequestParam("public") boolean fPublic, @RequestParam("hidden") boolean fHidden) throws Exception
	{
		return moduleManager.updateDataSource(module, fPublic, fHidden, null);
	}

	@GetMapping(moduleCreationURL)
	@PreAuthorize("@roleService.hasAdminRole(authentication)")
	protected @ResponseBody boolean createModule(@RequestParam String module, @RequestParam("host") String sHost) throws Exception
	{
		return moduleManager.createDataSource(module, sHost, null, null);
	}

	@DeleteMapping(moduleRemovalURL)
	@PreAuthorize("@roleService.hasAdminRole(authentication)")
	protected @ResponseBody boolean removeModule(@RequestParam String module, @RequestParam(required=false, value="removeDumps") Boolean fRemoveDumps) throws Exception
	{
		return moduleManager.removeDataSource(module, true, Boolean.TRUE.equals(fRemoveDumps));
	}
	
	@PostMapping(moduleEntityInfoURL)
	protected @ResponseBody String moduleEntityInfo(@RequestBody Map<String, Object> body) throws Exception
	{	    
	    String sModule = (String) body.get("module"), sEntityType = (String) body.get("entityType");
	    Collection<Comparable> entityIDs = (Collection<Comparable>) body.get("allLevelEntityIDs");
	    
	    // we only support checking permissions on top level entities
	    String topLevelEntityType = sEntityType.split("\\.")[0];
	    Comparable topLevelEntityId = entityIDs.iterator().next();

	    Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
	    Map<String, Collection<Comparable>> rolesOnEntities = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) || userDao.getSupervisedModules(loggedUserAuthorities).contains(sModule) ? null : userDao.getCustomRolesByModuleAndEntityType(loggedUserAuthorities).get(sModule).get(topLevelEntityType);
	    if (rolesOnEntities != null)
		    for (Collection<Comparable> entities : rolesOnEntities.values())
				if (entities != null && entities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(topLevelEntityId)) {
					rolesOnEntities = null;	// if this is null at the end then we consider we can allow access
					break;
				}

	    if (rolesOnEntities != null)
	    	throw new Exception("You are not allowed to access information for this " + topLevelEntityType);

		return moduleManager.managedEntityInfo(sModule, sEntityType, entityIDs);
	}

	@DeleteMapping(moduleEntityRemovalURL)
	protected @ResponseBody boolean removeModuleEntity(@RequestBody Map<String, Object> body) throws Exception
	{
	    Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
	    
	    String sModule = (String) body.get("module"), sEntityType = (String) body.get("entityType");
	    Collection<Comparable> entityIDs = (Collection<Comparable>) body.get("allLevelEntityIDs");
	    
	    // we only support checking permissions on top level entities
	    String topLevelEntityType = sEntityType.split("\\.")[0];
	    Comparable topLevelEntityId = entityIDs.iterator().next();

		Collection<Comparable> allowedEntities = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) || userDao.getSupervisedModules(loggedUserAuthorities).contains(sModule) ? null : userDao.getManagedEntitiesByModuleAndType(loggedUserAuthorities).get(sModule).get(topLevelEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(topLevelEntityId))
			throw new Exception("You are not allowed to remove this " + sEntityType);

		return moduleManager.removeManagedEntity(sModule, sEntityType, entityIDs);
	}

	@PostMapping(moduleEntityVisibilityUpdateUrl)
	protected @ResponseBody boolean modifyModuleEntityVisibility(@RequestParam("module") String sModule, @RequestParam("entityType") String sEntityType, @RequestParam("entityId") String sEntityId, @RequestParam("public") boolean fPublic) throws Exception
	{
	    Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		Collection<Comparable> allowedEntities = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) || userDao.getSupervisedModules(loggedUserAuthorities).contains(sModule) ? null : userDao.getManagedEntitiesByModuleAndType(loggedUserAuthorities).get(sModule).get(sEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(sEntityId))
			throw new Exception("You are not allowed to modify this " + sEntityType);

		return moduleManager.setManagedEntityVisibility(sModule, sEntityType, sEntityId, fPublic);
	}
	
	@PostMapping(moduleEntityDescriptionUpdateUrl)
	protected @ResponseBody boolean modifyModuleEntityDescription(@RequestParam("module") String sModule, @RequestParam("entityType") String sEntityType, @RequestParam("entityId") String sEntityId, @RequestParam("desc") String desc) throws Exception
	{
	    Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		Collection<Comparable> allowedEntities = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) || userDao.getSupervisedModules(loggedUserAuthorities).contains(sModule) ? null : userDao.getManagedEntitiesByModuleAndType(loggedUserAuthorities).get(sModule).get(sEntityType);
		if (allowedEntities != null && !allowedEntities.stream().map(c -> c.toString()).collect(Collectors.toList()).contains(sEntityId))
			throw new Exception("You are not allowed to modify this " + sEntityType);

		return moduleManager.setManagedEntityDescription(sModule, sEntityType, sEntityId, desc);
	}

	@GetMapping(moduleDumpInfoURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected @ResponseBody Map<String, Object> getModuleDumpInfo(@RequestParam String module) throws Exception {
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		List<DumpMetadata> dumps = moduleManager.getDumps(module);
		dumps.sort((dump1, dump2) -> dump2.getCreationDate().compareTo(dump1.getCreationDate()));
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("dumps", dumps);
		result.put("locked", !moduleManager.isModuleAvailableForDump(module));
		return result;
	}
	
    @GetMapping(moduleDumpDownloadURL)
    @PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
    protected void downloadDump(HttpServletResponse response, @RequestParam String module, @RequestParam("dumpId") String sDumpId) throws Exception {
        if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
            throw new Exception("The dump feature is disabled");  // TODO : 404 ?
        
        try (InputStream is = moduleManager.getDumpInputStream(module, sDumpId)) {
            LOG.debug("Sending dump " + sDumpId + " from database " + module + " into response");
            response.setContentType("application/gzip");
            ((HttpServletResponse) response).setHeader("Content-disposition", "inline; filename=" + sDumpId + ".gz");
            int len;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) > 0)
                response.getOutputStream().write(buffer, 0, len);
            response.getOutputStream().close();
        }
    }

    @GetMapping(moduleDumpLogDownloadURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
    protected void downloadDumpLog(HttpServletResponse response, @RequestParam String module, @RequestParam("dumpId") String sDumpId) throws Exception {
        if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
            throw new Exception("The dump feature is disabled");  // TODO : 404 ?
        
        try (InputStream is = moduleManager.getDumpLogInputStream(module, sDumpId)) {
            LOG.debug("Sending dump " + sDumpId + " from database " + module + " into response");
            response.setContentType("text/plain");
            ((HttpServletResponse) response).setHeader("Content-disposition", "inline; filename=" + sDumpId + "__dump.log");
            int len;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) > 0)
                response.getOutputStream().write(buffer, 0, len);
            response.getOutputStream().close();
        }
    }
    
	@GetMapping(newDumpURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected String startDumpProcess(@RequestParam String module, @RequestParam("name") String sName, @RequestParam("description") String sDescription) throws Exception {
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");

		if (!moduleManager.isModuleAvailableForDump(module))
			throw new Exception("The module is already busy, dump operation impossible");
		
        String dumpName = sName.replaceAll("(_|[^\\w-])+", "_");
        if (dumpName.matches("_*"))
            dumpName = module + "_" + DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss").format(LocalDateTime.now());
		String processID = dumpManager.startDumpProcess(module, dumpName, sDescription, SecurityContextHolder.getContext().getAuthentication().getName());
		return "redirect:" + dumpStatusPageURL + "?processID=" + processID + "&module=" + module;
	}

	@GetMapping(restoreDumpURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected String startRestoreProcess(@RequestParam String module, @RequestParam("dump") String sDump, @RequestParam("drop") boolean drop) throws Exception {
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		if (!moduleManager.isModuleAvailableForDump(module))
			throw new Exception("The module is already busy, dump operation impossible");

		String processID = dumpManager.startRestoreProcess(module, sDump, drop, SecurityContextHolder.getContext().getAuthentication().getName());
		return "redirect:" + dumpStatusPageURL + "?processID=" + processID + "&module=" + module;
	}
	
	@GetMapping(dumpRestoreWarningURL)
	protected @ResponseBody String getRestoreWarning(@RequestParam("module") String sModule, @RequestParam("dump") String sDumpId) throws Exception {
		String sMessage = "";
		String dbName = null;
		boolean fFoundCompletionMessage = false;

		try (InputStream is = moduleManager.getDumpLogInputStream(sModule, sDumpId); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {			
			String line;			
			while ((line = reader.readLine()) != null) {
                if (dbName == null) {
                	int nDbNamePos = line.indexOf(" --db=");	
                	if (nDbNamePos > -1)
                		dbName = line.substring(6 + nDbNamePos, line.indexOf(" ", 6 + nDbNamePos));
                }

                LOG.debug(line);
                if (line.toLowerCase().contains("mux completed successfully"))
                	fFoundCompletionMessage = true;
            }

            if (!fFoundCompletionMessage)
            	sMessage += " Dump logfile does not mention succesful completion.";

            if (dbName != null && !(dbName + "_").contains("_" + sModule + "_"))
            	sMessage += " Mongodump database name (" + dbName + ") does not seem to match the target database (" + sModule + ").";
		}
		catch (FileNotFoundException fnfe) {
			return " No logfile found for this dump, unable to check how safe it would be to restore it";
		}
		return sMessage;
	}

	@GetMapping(dumpStatusPageURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected ModelAndView dumpStatusPage(@RequestParam String module, @RequestParam("processID") String processID) throws Exception {
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
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected @ResponseBody Map<String, Object> dumpStatusQuery(@RequestParam String module, @RequestParam("processID") String processID, @RequestParam(name="logStart", required=false) Integer logStart) throws Exception {
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		IBackgroundProcess process = dumpManager.getProcess(processID);

		Map<String, Object> result = new HashMap<String, Object>();
		if (process == null) {
			result.put("processID", processID);
			result.put("status", ProcessStatus.INEXISTENT.label);
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
			result.put("log", HtmlUtils.htmlEscape(log));
			result.put("warning", null);
			if (process.getStatus() == ProcessStatus.SUCCESS && processID.contains("restore")) {
				List<DumpMetadata> dumps = moduleManager.getDumps(process.getModule());
				for (DumpMetadata dump : dumps) {
					if (dump.getValidity() == DumpStatus.DIVERGED) {
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
	    Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		if (!loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) && userDao.getSupervisedModules(loggedUserAuthorities).isEmpty())
			throw new Exception("You are not allowed to access dump status");

		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		ModelAndView mav = new ModelAndView();
		return mav;
	}

	@GetMapping(processListStatusURL)
	protected @ResponseBody TreeSet<Map<String, String>> processListStatus() throws Exception {
		Collection<? extends GrantedAuthority> loggedUserAuthorities = userDao.getLoggedUserAuthorities();
		HashSet<String> supervisedModules = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) ? null : userDao.getSupervisedModules(loggedUserAuthorities);

		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty()) {
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?
		}

		Map<String, AbstractProcess> allProcesses = moduleManager.getImportProcesses();
		allProcesses.putAll(dumpManager.getProcesses());

		List<String> orderedIds = new ArrayList<>(allProcesses.keySet());
		Collections.sort(orderedIds);

		TreeSet<Map<String, String>> result = new TreeSet<>(new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> o1, Map<String, String> o2) {
				int result = o1.get("status").compareToIgnoreCase(o2.get("status"));
				if (result == 0) {
					result = o1.get("module").compareToIgnoreCase(o2.get("module"));
					if (result == 0)
						result = o1.get("processID").compareToIgnoreCase(o2.get("processID"));
				}
				return result;
			}
		});

		for (String processID : orderedIds) {
			IBackgroundProcess process = allProcesses.get(processID);
			if (supervisedModules != null && !supervisedModules.contains(process.getModule())) {
				continue;    // logged user is neither admin nor DB supervisor
			}

			Map<String, String> item = new HashMap<>();
			item.put("processID", process.getProcessID());
			try {
				item.put("type", process.getProcessID().split("::")[0].toUpperCase());
			}
			catch (Exception e) {
				LOG.error("Unable to determine process type", e);
			}
			item.put("status", process.getStatus().label);
			item.put("message", process.getStatusMessage());
			item.put("module", process.getModule());
			result.add(item);
		}

		return result;
	}

	@GetMapping(abortProcessURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected @ResponseBody Map<String, Boolean> abortProcess(@RequestParam String module, @RequestParam("processID") String processID) throws Exception {
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		Map<String, Boolean> result = new HashMap<String, Boolean>();
		result.put("done", dumpManager.abortProcess(processID));
		return result;
	}

	@DeleteMapping(deleteDumpURL)
	@PreAuthorize("@roleService.hasSupervisorOrAdminRole(authentication, #module)")
	protected @ResponseBody Map<String, Boolean> deleteDump(@RequestParam String module, @RequestParam("dump") String dump) throws Exception {
		if (!moduleManager.getActionRequiredToEnableDumps().isEmpty())
			throw new Exception("The dump feature is disabled");  // TODO : 404 ?

		Map<String, Boolean> result = new HashMap<String, Boolean>();
		result.put("done", moduleManager.deleteDump(module, dump));
		return result;
	}

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