package fr.cirad.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserWithMethod implements UserDetails {
	private static final Logger LOG = Logger.getLogger(UserWithMethod.class);
	private static final long serialVersionUID = 1212867552943815722L;

	/** {@link #getMethod()} value for CAS-authenticated accounts. */
	public static final String METHOD_CAS = "CAS";

	/**
	 * Prefix for {@link #getMethod()} on accounts provisioned through an OpenID Connect provider;
	 * the provider's registration id is appended (e.g. "oidc:google"). Guarantees an OIDC provider
	 * a deployment happens to name "cas" is never confused with real CAS authentication.
	 */
	public static final String METHOD_OIDC_PREFIX = "oidc:";
	
	private static final Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
	
	private String username;
	private String password;
	private String email;
	private ArrayList<GrantedAuthority> authorities;
	private boolean enabled;
	private String method;  // Authentication method
	private String sub; // identifier of the user in the oauth provider
	private String userId; // not persisted: key used in the in-memory users map

	public UserWithMethod(String username, String password, Collection<? extends GrantedAuthority> authorities, boolean enabled, String method, String email) {
		this(username, password, authorities, enabled, method, email, null);
	}

	public UserWithMethod(String username, String password, Collection<? extends GrantedAuthority> authorities, boolean enabled, String method, String email, String sub) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.enabled = enabled;
		this.method = method;
		this.authorities = new ArrayList<GrantedAuthority>();
		this.authorities.addAll(authorities);
		this.sub = normalizeSub(sub);
		updateUserId();
	}

	private static String normalizeSub(String sub) {
		return sub == null || sub.isBlank() ? null : sub.trim();
	}

	/** Builds the map key: the username for local/CAS accounts, or method_sub for OAuth accounts. */
	public static String buildUserId(String username, String method, String sub) {
		String normalizedSub = normalizeSub(sub);
		return normalizedSub == null ? username : (method == null ? "" : method) + ":" + normalizedSub;
	}

	private void updateUserId() {
		this.userId = buildUserId(username, method, sub);
	}
	
	public static boolean isEmailAddress(String stringToTest) {
		return emailPattern.matcher(stringToTest).matches();
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
		return email/* != null ? email : (isEmailAddress(username) ? username : null)*/;
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
		updateUserId();
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
			updateUserId();
		}
	}
	
	@Override public boolean equals(Object otherUser) {
	    if (otherUser == null)
	        return false;
	    return ((UserWithMethod) otherUser).getUsername().equals(getUsername()) && ((UserWithMethod) otherUser).getMethod().equals(getMethod());
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = normalizeSub(sub);
		updateUserId();
	}

	public String getUserId() {
		return userId;
	}

}
