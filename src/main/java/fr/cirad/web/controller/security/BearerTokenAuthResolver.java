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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import fr.cirad.security.ReloadableInMemoryDaoImpl;
import fr.cirad.security.UserWithMethod;

/**
 * The host app's own React UI authenticates via a stateless Bearer token, which never touches
 * the HTTP session that this module's endpoints rely on for their session-based Authentication
 * (so an iframe embedding this app appears logged out even though the host UI is logged in).
 *
 * Called explicitly at the top of each controller method that needs it (rather than via a Filter/
 * ServletContainerInitializer, which for unresolved reasons never got invoked by the dev server
 * this was tested against) — a plain method call on an ordinary Spring bean, so it's guaranteed
 * to run wherever it's called from.
 *
 * Resolves the token to a user for the current request only (never persisted to the session,
 * unlike the host app's own session), so it can't clash with e.g. a concurrently open Swagger tab
 * authenticated as a different user on the same browser/session.
 */
@Component
public class BearerTokenAuthResolver {

    private static final Logger LOG = Logger.getLogger(BearerTokenAuthResolver.class);

    private static final Pattern USERNAME_PATTERN = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]*)\"");

    @Autowired
    private ReloadableInMemoryDaoImpl userDao;

    // Configurable so this stays reusable by host apps other than Gigwa2.
    @Value("${roleManager.tokenValidationPath:/rest/gigwa/userInfo}")
    private String tokenValidationPath;

    /**
     * If there's no real session-based identity yet, tries to resolve one from an
     * "Authorization: Bearer ..." header and, if successful, installs it into
     * SecurityContextHolder for the remainder of this request.
     */
    public void resolveIfNeeded(HttpServletRequest request) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated() && !"anonymousUser".equals(current.getName()))
            return; // already have a real, session-based identity: nothing to do

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7))
            return; // no token to try: leave the normal (anonymous) behavior in place

        String username = resolveUsernameFromToken(request, authHeader);
        if (username == null)
            return;

        try {
            UserWithMethod user = (UserWithMethod) userDao.loadUserByUsername(username);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (UsernameNotFoundException e) {
            LOG.warn("Bearer token resolved to unknown user: " + username);
        }
    }

    private String resolveUsernameFromToken(HttpServletRequest request, String authHeader) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + tokenValidationPath).openConnection();
            conn.setRequestProperty("Authorization", authHeader);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            if (conn.getResponseCode() != 200)
                return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
            }
            Matcher m = USERNAME_PATTERN.matcher(sb.toString());
            return m.find() ? m.group(1) : null;
        } catch (IOException e) {
            LOG.warn("Failed to validate bearer token against " + tokenValidationPath, e);
            return null;
        }
    }
}
