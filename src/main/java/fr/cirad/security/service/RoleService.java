package fr.cirad.security.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.base.IRoleDefinition;

@Service
public class RoleService {
	
	@Autowired private ReloadableInMemoryDaoImpl userService;
    
    public boolean hasAdminRole(Authentication authentication) {
    	UserDetails user = userService.loadUserByUsername(authentication.getName());
        return user.getAuthorities().contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN));
    }
        
    public boolean hasDbCreatororOrAdminRole(Authentication authentication) {
    	UserDetails user = userService.loadUserByUsername(authentication.getName());
    	Collection<? extends GrantedAuthority> auth = user.getAuthorities();
        return auth.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_ADMIN)) || auth.contains(new SimpleGrantedAuthority(IRoleDefinition.ROLE_DB_CREATOR));
    }

    public boolean hasSupervisorOrAdminRole(Authentication authentication, String module) {
    	UserDetails user = userService.loadUserByUsername(authentication.getName());
        return hasAdminRole(authentication) || user.getAuthorities().contains(new SimpleGrantedAuthority(module + "$" + IRoleDefinition.ROLE_DB_SUPERVISOR));
    }
}