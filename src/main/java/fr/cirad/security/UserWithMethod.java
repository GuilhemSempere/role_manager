package fr.cirad.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserWithMethod implements UserDetails {
	private static final long serialVersionUID = 1212867552943815722L;
	
	private static final Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
	
	private String username;
	private String password;
	private String email;
	private ArrayList<GrantedAuthority> authorities;
	private boolean enabled;
	private String method;  // Authentication method

	public UserWithMethod(String username, String password, Collection<? extends GrantedAuthority> authorities, boolean enabled, String method, String email) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.enabled = enabled;
		this.method = method;
		this.authorities = new ArrayList<GrantedAuthority>();
		this.authorities.addAll(authorities);
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return new ArrayList<GrantedAuthority>(authorities);
	}

	@Override
	public String getPassword() {
		return password;
	}

	public String getEmail() {
		return email != null ? email : (emailPattern.matcher(username).matches() ? username : null);
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String getUsername() {
		return username;
	}
	
	public String getMethod() {
		return method;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
		this.authorities.clear();
		this.authorities.addAll(authorities);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setMethod(String method) {
		if (!method.equals(this.method)) {
			throw new IllegalArgumentException("Tried to change user " + username + " authentication method from " +
					(this.method.isEmpty() ? "<default>" : this.method) + " to " + (method.isEmpty() ? "<default>" : method));
		} else {
			this.method = method;
		}
	}
	
	@Override public boolean equals(Object otherUser) {
	    if (otherUser == null)
	        return false;
	    return ((UserWithMethod) otherUser).getUsername().equals(getUsername()) && ((UserWithMethod) otherUser).getMethod().equals(getMethod());
	}
}
