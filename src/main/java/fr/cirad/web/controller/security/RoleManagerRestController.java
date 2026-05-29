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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cirad.manager.IModuleManager;
import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.UserWithMethod;
import fr.cirad.security.base.IRoleDefinition;

/**
 * REST API Controller for React-based Role Manager frontend.
 * Provides JSON endpoints to replace JSTL-based JSP data binding.
 */
@RestController
@RequestMapping("/private/roleManager/api")
@SuppressWarnings("rawtypes")
public class RoleManagerRestController {

    private static final Logger LOG = Logger.getLogger(RoleManagerRestController.class);

    @Autowired
    private ReloadableInMemoryDaoImpl userDao;

    @Autowired
    private IModuleManager moduleManager;

    // ============================================================
    // Role Configuration Endpoint - exposes roles.properties config
    // ============================================================

    @GetMapping({"/roleConfig", "/roleConfig.json_"})
    public Map<String, Object> getRoleConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Level 1 types and their roles
        config.put("level1Types", new ArrayList<>(UserPermissionController.rolesByLevel1Type.keySet()));
        config.put("level1Roles", UserPermissionController.rolesByLevel1Type);
        
        // Level 2 types and their roles (nested structure: parent -> subtype -> roles)
        config.put("level2Types", UserPermissionController.rolesByLevel2Type.keySet().stream()
            .flatMap(parent -> UserPermissionController.rolesByLevel2Type.get(parent).keySet().stream()
                .map(sub -> parent + "." + sub))
            .collect(Collectors.toList()));
        config.put("level2Roles", UserPermissionController.rolesByLevel2Type);
        
        // Role separator for building authority strings
        config.put("roleSeparator", UserPermissionController.ROLE_STRING_SEPARATOR);
        
        // Entity manager role constant
        config.put("entityManagerRole", IRoleDefinition.ENTITY_MANAGER_ROLE);
        
        // Admin and supervisor role constants
        config.put("roleAdmin", IRoleDefinition.ROLE_ADMIN);
        config.put("roleDbSupervisor", IRoleDefinition.ROLE_DB_SUPERVISOR);
        config.put("roleDbCreator", IRoleDefinition.ROLE_DB_CREATOR);
        
        return config;
    }

    // ============================================================
    // Current User Endpoint
    // ============================================================

    @GetMapping({"/currentUser", "/currentUser.json_"})
    public Map<String, Object> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("authenticated", false);
        }

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        boolean isAdmin = authorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
        
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("username", auth.getName());
        result.put("isAdmin", isAdmin);
        result.put("authorities", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        result.put("canWriteToSystem", userDao.canLoggedUserWriteToSystem());
        result.put("supervisedModules", userDao.getSupervisedModules(authorities));
        result.put("managedEntities", userDao.getManagedEntitiesByModuleAndType(authorities));
        
        return result;
    }

    // ============================================================
    // User Details Endpoint
    // ============================================================

    @GetMapping({"/user/{username}", "/user/{username}.json_"})
    public ResponseEntity<?> getUserDetails(@PathVariable String username, Authentication auth) {
        try {
            UserWithMethod user = (UserWithMethod) userDao.loadUserByUsername(username);
            return ResponseEntity.ok(buildUserResponse(user, auth));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> buildUserResponse(UserWithMethod user, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("enabled", user.isEnabled());
        result.put("method", user.getMethod().isEmpty() ? "Local" : user.getMethod());
        result.put("email", user.getEmail());
        
        // Parse authorities into structured format
        Collection<? extends GrantedAuthority> userAuthorities = user.getAuthorities();
        result.put("isAdmin", userAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)));
        result.put("isDbCreator", userAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_DB_CREATOR)));
        result.put("supervisedModules", userDao.getSupervisedModules(userAuthorities));
        result.put("managedEntities", userDao.getManagedEntitiesByModuleAndType(userAuthorities));
        result.put("customRoles", userDao.getCustomRolesByModuleAndEntityType(userAuthorities));
        result.put("authorities", userAuthorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        
        return result;
    }

    // ============================================================
    // User Create/Update Endpoint
    // ============================================================

    @PostMapping({"/user", "/user.json_"})
    public ResponseEntity<?> createOrUpdateUser(@RequestBody UserSaveRequest request, Authentication auth) {
        try {
            return saveUser(request, auth, true);
        } catch (Exception e) {
            LOG.error("Error saving user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping({"/user/{username}", "/user/{username}.json_"})
    public ResponseEntity<?> updateUser(@PathVariable String username, @RequestBody UserSaveRequest request, Authentication auth) {
        request.setUsername(username);
        try {
            return saveUser(request, auth, false);
        } catch (Exception e) {
            LOG.error("Error updating user", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> saveUser(UserSaveRequest request, Authentication auth, boolean isNew) throws Exception {
        List<String> errors = new ArrayList<>();
        
        String username = request.getUsername();
        String password = request.getPassword();
        String email = request.getEmail();
        
        if (username == null || username.trim().isEmpty()) {
            errors.add("Username must not be empty");
        }

        UserWithMethod existingUser = null;
        try {
            existingUser = (UserWithMethod) userDao.loadUserByUsername(username);
            if (isNew && existingUser != null) {
                errors.add("Username already exists");
            }
        } catch (UsernameNotFoundException e) {
            if (!isNew && password == null) {
                // Updating non-existent user without password
            }
            if (isNew && (password == null || password.isEmpty()) && !request.isCloning()) {
                errors.add("You must specify a password");
            }
        }

        Collection<? extends GrantedAuthority> loggedUserAuthorities = auth.getAuthorities();
        if (email == null || email.isEmpty()) {
            if (auth.getName().equals(username) && !loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))) {
                errors.add("You must specify an e-mail address");
            }
        } else if (!UserWithMethod.isEmailAddress(email)) {
            errors.add("Invalid e-mail address specified");
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }

        // Build granted authorities from request
        HashSet<String> grantedAuthorityLabels = buildGrantedAuthorities(request, existingUser, auth);
        
        if (grantedAuthorityLabels.isEmpty()) {
            grantedAuthorityLabels.add(IRoleDefinition.DUMMY_EMPTY_ROLE);
        }

        // Preserve password if not provided
        if (password == null && existingUser != null) {
            password = existingUser.getPassword();
        }

        String method = existingUser != null ? existingUser.getMethod() : "";
        
        userDao.saveOrUpdateUser(username, password, 
            grantedAuthorityLabels.toArray(new String[0]), 
            true, method, email);

        return ResponseEntity.ok(Map.of("success", true, "username", username));
    }

    private HashSet<String> buildGrantedAuthorities(UserSaveRequest request, UserWithMethod existingUser, Authentication auth) {
        HashSet<String> grantedAuthorityLabels = new HashSet<>();
        Collection<? extends GrantedAuthority> loggedUserAuthorities = auth.getAuthorities();
        boolean isLoggedUserAdmin = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));

        // Preserve admin role if user already has it
        if (existingUser != null && existingUser.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN))) {
            grantedAuthorityLabels.add(IRoleDefinition.ROLE_ADMIN);
        } else {
            // DB Creator role
            if (request.isDbCreator()) {
                grantedAuthorityLabels.add(IRoleDefinition.ROLE_DB_CREATOR);
            }

            // Supervisor roles
            if (request.getSupervisorModules() != null) {
                for (String module : request.getSupervisorModules()) {
                    grantedAuthorityLabels.add(module + UserPermissionController.ROLE_STRING_SEPARATOR + IRoleDefinition.ROLE_DB_SUPERVISOR);
                }
            }

            // Entity permissions
            if (request.getPermissions() != null) {
                for (PermissionEntry perm : request.getPermissions()) {
                    String authorityString = perm.getModule() + UserPermissionController.ROLE_STRING_SEPARATOR +
                        perm.getEntityType() + UserPermissionController.ROLE_STRING_SEPARATOR +
                        perm.getRole() + UserPermissionController.ROLE_STRING_SEPARATOR +
                        perm.getEntityId();
                    grantedAuthorityLabels.add(authorityString);
                }
            }
        }

        // Preserve permissions not manageable by current user
        if (existingUser != null && !isLoggedUserAdmin) {
            preserveUnmanageablePermissions(existingUser, auth, grantedAuthorityLabels, request);
        }

        return grantedAuthorityLabels;
    }

    private void preserveUnmanageablePermissions(UserWithMethod existingUser, Authentication auth, 
            HashSet<String> grantedAuthorityLabels, UserSaveRequest request) {
        Collection<? extends GrantedAuthority> loggedUserAuthorities = auth.getAuthorities();
        HashSet<String> modulesSupervisedBySubmittingUser = userDao.getSupervisedModules(loggedUserAuthorities);
        
        // Preserve supervisor roles that current user cannot manage
        for (String supervisedModule : userDao.getSupervisedModules(existingUser.getAuthorities())) {
            if (!modulesSupervisedBySubmittingUser.contains(supervisedModule)) {
                grantedAuthorityLabels.add(supervisedModule + UserPermissionController.ROLE_STRING_SEPARATOR + IRoleDefinition.ROLE_DB_SUPERVISOR);
            }
        }

        // Preserve entity permissions that current user cannot manage
        Map<String, Map<String, Map<String, Collection<Comparable>>>> customRoles = 
            userDao.getCustomRolesByModuleAndEntityType(existingUser.getAuthorities());
        
        for (String module : customRoles.keySet()) {
            Map<String, Map<String, Collection<Comparable>>> rolesByEntityType = customRoles.get(module);
            for (String entityType : rolesByEntityType.keySet()) {
                Map<String, Collection<Comparable>> entityIDsByRoles = rolesByEntityType.get(entityType);
                for (String role : entityIDsByRoles.keySet()) {
                    for (Comparable entityId : entityIDsByRoles.get(role)) {
                        boolean canManage = modulesSupervisedBySubmittingUser.contains(module) ||
                            loggedUserAuthorities.contains(new SimpleGrantedAuthority(
                                module + UserPermissionController.ROLE_STRING_SEPARATOR + 
                                entityType + UserPermissionController.ROLE_STRING_SEPARATOR + 
                                IRoleDefinition.ENTITY_MANAGER_ROLE + UserPermissionController.ROLE_STRING_SEPARATOR + 
                                entityId));
                        
                        if (!canManage) {
                            grantedAuthorityLabels.add(
                                module + UserPermissionController.ROLE_STRING_SEPARATOR + 
                                entityType + UserPermissionController.ROLE_STRING_SEPARATOR + 
                                role + UserPermissionController.ROLE_STRING_SEPARATOR + 
                                entityId);
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // Modules Endpoint
    // ============================================================

    @GetMapping({"/modules", "/modules.json_"})
    public Map<String, Object> getModules(Authentication auth) {
        Collection<? extends GrantedAuthority> loggedUserAuthorities = auth.getAuthorities();
        boolean isAdmin = loggedUserAuthorities.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
        Map<String, Map<String, Collection<Comparable>>> managedEntities = userDao.getManagedEntitiesByModuleAndType(loggedUserAuthorities);
        Collection<String> supervisedModules = userDao.getSupervisedModules(loggedUserAuthorities);

        // Public modules
        Collection<String> publicModules = new ArrayList<>(moduleManager.getModules(true));
        List<Map<String, Object>> publicModulesList = new ArrayList<>();
        for (String module : publicModules) {
            if (isAdmin || supervisedModules.contains(module) || managedEntities.containsKey(module)) {
                publicModulesList.add(Map.of(
                    "name", module,
                    "isPublic", true,
                    "canSupervise", isAdmin || supervisedModules.contains(module),
                    "managedEntityTypes", managedEntities.getOrDefault(module, Map.of()).keySet()
                ));
            }
        }

        // Private modules
        Collection<String> privateModules = new ArrayList<>(moduleManager.getModules(false));
        List<Map<String, Object>> privateModulesList = new ArrayList<>();
        for (String module : privateModules) {
            if (isAdmin || supervisedModules.contains(module) || managedEntities.containsKey(module)) {
                privateModulesList.add(Map.of(
                    "name", module,
                    "isPublic", false,
                    "canSupervise", isAdmin || supervisedModules.contains(module),
                    "managedEntityTypes", managedEntities.getOrDefault(module, Map.of()).keySet()
                ));
            }
        }

        return Map.of(
            "publicModules", publicModulesList,
            "privateModules", privateModulesList,
            "isAdmin", isAdmin
        );
    }

    // ============================================================
    // Module Entities Endpoint
    // ============================================================

    @GetMapping({"/module/{module}/entities", "/module/{module}/entities.json_"})
    public Map<String, Object> getModuleEntities(
            @PathVariable String module,
            @RequestParam String entityType,
            @RequestParam(required = false) String parentEntityId,
            Authentication auth) throws Exception {
        
        boolean visibilitySupported = moduleManager.doesEntityTypeSupportVisibility(module, entityType);
        boolean descriptionSupported = moduleManager.isInlineDescriptionUpdateSupportedForEntity(entityType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("visibilitySupported", visibilitySupported);
        result.put("descriptionSupported", descriptionSupported);
        result.put("roles", UserPermissionController.rolesByLevel1Type.get(entityType));
        
        // Get entities
        Map<Comparable, String[]> publicEntities = moduleManager.getEntitiesByModule(
            entityType, visibilitySupported ? true : null, Arrays.asList(module), descriptionSupported).get(module);
        
        if (publicEntities != null) {
            result.put("publicEntities", publicEntities.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entity = new HashMap<>();
                    entity.put("id", e.getKey());
                    entity.put("label", e.getValue() != null && e.getValue().length > 0 && e.getValue()[0] != null ? e.getValue()[0] : "");
                    entity.put("description", e.getValue() != null && e.getValue().length > 1 && e.getValue()[1] != null ? e.getValue()[1] : "");
                    return entity;
                })
                .collect(Collectors.toList()));
        } else {
            result.put("publicEntities", List.of());
        }
        
        if (visibilitySupported) {
            Map<Comparable, String[]> privateEntities = moduleManager.getEntitiesByModule(
                entityType, false, Arrays.asList(module), descriptionSupported).get(module);
            
            if (privateEntities != null) {
                result.put("privateEntities", privateEntities.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> entity = new HashMap<>();
                        entity.put("id", e.getKey());
                        entity.put("label", e.getValue() != null && e.getValue().length > 0 && e.getValue()[0] != null ? e.getValue()[0] : "");
                        entity.put("description", e.getValue() != null && e.getValue().length > 1 && e.getValue()[1] != null ? e.getValue()[1] : "");
                        return entity;
                    })
                    .collect(Collectors.toList()));
            } else {
                result.put("privateEntities", List.of());
            }
        }

        Map<String, LinkedHashSet<String>> subEntityTypeToRolesMap = UserPermissionController.rolesByLevel2Type.get(entityType);
        if (subEntityTypeToRolesMap == null) {
            subEntityTypeToRolesMap = new HashMap<>();
        }

        result.put("subEntityTypes", new ArrayList<>(subEntityTypeToRolesMap.keySet()));

        Map<String, Map<String, List<Map<String, Object>>>> subEntitiesByEntityId = new HashMap<>();
        List<Comparable> mainEntityIds = new ArrayList<>();
        if (publicEntities != null) {
            mainEntityIds.addAll(publicEntities.keySet());
        }
        if (visibilitySupported) {
            Map<Comparable, String[]> privateEntities = moduleManager.getEntitiesByModule(
                entityType, false, Arrays.asList(module), false).get(module);
            if (privateEntities != null) {
                for (Comparable mainEntityId : privateEntities.keySet()) {
                    if (!mainEntityIds.contains(mainEntityId)) {
                        mainEntityIds.add(mainEntityId);
                    }
                }
            }
        }

        for (Comparable mainEntityId : mainEntityIds) {
            Map<String, List<Map<String, Object>>> subEntitiesForMainEntity = new HashMap<>();
            for (String subEntityType : subEntityTypeToRolesMap.keySet()) {
                Map<Comparable, String> subEntities = moduleManager.getSubEntities(
                    entityType + "." + subEntityType,
                    module,
                    new Comparable[] { mainEntityId }
                );
                if (subEntities != null && !subEntities.isEmpty()) {
                    List<Map<String, Object>> formatted = subEntities.entrySet().stream()
                        .map(e -> {
                            Map<String, Object> item = new HashMap<>();
                            item.put("id", e.getKey());
                            item.put("label", e.getValue());
                            return item;
                        })
                        .collect(Collectors.toList());
                    subEntitiesForMainEntity.put(subEntityType, formatted);
                }
            }

            if (!subEntitiesForMainEntity.isEmpty()) {
                subEntitiesByEntityId.put(String.valueOf(mainEntityId), subEntitiesForMainEntity);
            }
        }

        result.put("subEntitiesByEntityId", subEntitiesByEntityId);
        
        return result;
    }

    @GetMapping({"/module/{module}/subEntities", "/module/{module}/subEntities.json_"})
    public Map<String, Object> getModuleSubEntities(
            @PathVariable String module,
            @RequestParam String entityType,
            @RequestParam String mainEntityId,
            Authentication auth) throws Exception {

        Map<Comparable, String> subEntities = moduleManager.getSubEntities(
            entityType,
            module,
            new Comparable[] { mainEntityId }
        );

        List<Map<String, Object>> items = new ArrayList<>();
        if (subEntities != null) {
            for (Map.Entry<Comparable, String> entry : subEntities.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", entry.getKey());
                item.put("label", entry.getValue());
                items.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("module", module);
        result.put("entityType", entityType);
        result.put("mainEntityId", mainEntityId);
        result.put("items", items);
        return result;
    }

    // ============================================================
    // Permission Form Data Endpoint (replaces UserPermissions.do_ model)
    // ============================================================

    @GetMapping({"/permissionForm", "/permissionForm.json_"})
    public Map<String, Object> getPermissionFormData(
            @RequestParam String username,
            @RequestParam String module,
            @RequestParam String entityType,
            Authentication auth) throws Exception {
        
        Map<String, Object> result = new HashMap<>();
        result.put("module", module);
        result.put("entityType", entityType);
        result.put("roles", UserPermissionController.rolesByLevel1Type.get(entityType));
        
        // Try to load user (may not exist yet)
        UserWithMethod user = null;
        try {
            user = (UserWithMethod) userDao.loadUserByUsername(username);
            result.put("userExists", true);
            result.put("userAuthorities", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        } catch (UsernameNotFoundException e) {
            result.put("userExists", false);
            result.put("userAuthorities", List.of());
        }
        
        // Get entities for this module and type
        boolean visibilitySupported = moduleManager.doesEntityTypeSupportVisibility(module, entityType);
        result.put("visibilitySupported", visibilitySupported);
        
        Map<Comparable, String[]> publicEntities = moduleManager.getEntitiesByModule(
            entityType, visibilitySupported ? true : null, Arrays.asList(module), false).get(module);
        result.put("publicEntities", formatEntities(publicEntities));
        
        if (visibilitySupported) {
            Map<Comparable, String[]> privateEntities = moduleManager.getEntitiesByModule(
                entityType, false, Arrays.asList(module), false).get(module);
            result.put("privateEntities", formatEntities(privateEntities));
        }
        
        return result;
    }
    
    private List<Map<String, Object>> formatEntities(Map<Comparable, String[]> entities) {
        if (entities == null) return List.of();
        return entities.entrySet().stream()
            .map(e -> {
                Map<String, Object> entity = new HashMap<>();
                entity.put("id", e.getKey());
                entity.put("label", e.getValue()[0]);
                if (e.getValue().length > 1) {
                    entity.put("description", e.getValue()[1]);
                }
                return entity;
            })
            .collect(Collectors.toList());
    }

    // ============================================================
    // Add/Remove Permission Endpoints
    // ============================================================

    @PostMapping({"/user/{username}/permission", "/user/{username}/permission.json_"})
    public ResponseEntity<?> addPermission(
            @PathVariable String username,
            @RequestBody PermissionEntry permission,
            Authentication auth) {
        try {
            UserWithMethod user = (UserWithMethod) userDao.loadUserByUsername(username);
            
            String authorityString = permission.getModule() + UserPermissionController.ROLE_STRING_SEPARATOR +
                permission.getEntityType() + UserPermissionController.ROLE_STRING_SEPARATOR +
                permission.getRole() + UserPermissionController.ROLE_STRING_SEPARATOR +
                permission.getEntityId();
            
            // Check if already has this authority
            SimpleGrantedAuthority newAuth = new SimpleGrantedAuthority(authorityString);
            if (user.getAuthorities().contains(newAuth)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Permission already exists"));
            }
            
            // Add the new authority
            List<GrantedAuthority> newAuthorities = new ArrayList<>(user.getAuthorities());
            newAuthorities.add(newAuth);
            
            userDao.saveOrUpdateUser(username, null, 
                newAuthorities.stream().map(GrantedAuthority::getAuthority).toArray(String[]::new),
                user.isEnabled(), user.getMethod(), user.getEmail());
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOG.error("Error adding permission", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping({"/user/{username}/permission", "/user/{username}/permission.json_"})
    public ResponseEntity<?> removePermission(
            @PathVariable String username,
            @RequestParam String module,
            @RequestParam String entityType,
            @RequestParam String role,
            @RequestParam String entityId,
            Authentication auth) {
        try {
            UserWithMethod user = (UserWithMethod) userDao.loadUserByUsername(username);
            
            String authorityString = module + UserPermissionController.ROLE_STRING_SEPARATOR +
                entityType + UserPermissionController.ROLE_STRING_SEPARATOR +
                role + UserPermissionController.ROLE_STRING_SEPARATOR +
                entityId;
            
            List<GrantedAuthority> newAuthorities = user.getAuthorities().stream()
                .filter(a -> !a.getAuthority().equals(authorityString))
                .collect(Collectors.toList());
            
            if (newAuthorities.isEmpty()) {
                newAuthorities.add(new SimpleGrantedAuthority(IRoleDefinition.DUMMY_EMPTY_ROLE));
            }
            
            userDao.saveOrUpdateUser(username, null,
                newAuthorities.stream().map(GrantedAuthority::getAuthority).toArray(String[]::new),
                user.isEnabled(), user.getMethod(), user.getEmail());
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            LOG.error("Error removing permission", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // DTO Classes
    // ============================================================

    public static class UserSaveRequest {
        private String username;
        private String password;
        private String email;
        private boolean dbCreator;
        private boolean cloning;
        private List<String> supervisorModules;
        private List<PermissionEntry> permissions;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isDbCreator() { return dbCreator; }
        public void setDbCreator(boolean dbCreator) { this.dbCreator = dbCreator; }
        public boolean isCloning() { return cloning; }
        public void setCloning(boolean cloning) { this.cloning = cloning; }
        public List<String> getSupervisorModules() { return supervisorModules; }
        public void setSupervisorModules(List<String> supervisorModules) { this.supervisorModules = supervisorModules; }
        public List<PermissionEntry> getPermissions() { return permissions; }
        public void setPermissions(List<PermissionEntry> permissions) { this.permissions = permissions; }
    }

    public static class PermissionEntry {
        private String module;
        private String entityType;
        private String role;
        private String entityId;

        // Getters and Setters
        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
    }
}
