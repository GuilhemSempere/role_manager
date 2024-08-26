package fr.cirad.security.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import fr.cirad.security.base.IRoleDefinition;

@Service
public class RoleService {
    
    public boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
    }

    public boolean hasSupervisorOrAdminRole(Authentication authentication, String module) {
        return hasAdminRole(authentication) || authentication.getAuthorities().contains(new SimpleGrantedAuthority(module + "$SUPERVISOR"));
    }
}