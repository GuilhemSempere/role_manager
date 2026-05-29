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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve the React SPA for Role Manager.
 * Forwards all non-API routes under /roleManager to the React app's index.html.
 */
@Controller
public class RoleManagerSpaController {

    /**
     * Forward root path to React app
     */
    @GetMapping("/roleManager")
    public String indexRoot() {
        return "forward:/roleManager/index.html";
    }

    /**
     * Forward all SPA routes to React app's index.html.
     * This handles client-side routing (e.g., /roleManager/users, /roleManager/user/john).
     * 
     * Note: Requests for static assets (js, css, images) are excluded by the pattern
     * and served directly by the servlet container.
     */
    @GetMapping("/roleManager/{path:[^\\.]*}")
    public String forwardSingleLevel() {
        return "forward:/roleManager/index.html";
    }

    @GetMapping("/roleManager/{path1:[^\\.]*}/{path2:[^\\.]*}")
    public String forwardTwoLevels() {
        return "forward:/roleManager/index.html";
    }

    @GetMapping("/roleManager/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}")
    public String forwardThreeLevels() {
        return "forward:/roleManager/index.html";
    }

    @GetMapping("/roleManager/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}/{path4:[^\\.]*}")
    public String forwardFourLevels() {
        return "forward:/roleManager/index.html";
    }

    @GetMapping("/roleManager/{path1:[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}/{path4:[^\\.]*}/{path5:[^\\.]*}")
    public String forwardFiveLevels() {
        return "forward:/roleManager/index.html";
    }
}
