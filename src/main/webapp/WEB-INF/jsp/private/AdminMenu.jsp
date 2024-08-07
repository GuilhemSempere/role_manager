<%--
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
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="fr.cirad.security.base.IRoleDefinition,fr.cirad.web.controller.security.UserPermissionController,fr.cirad.web.controller.BackOfficeController,org.springframework.security.core.context.SecurityContextHolder" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />
<c:set var="loggedUserAuthorities" value="${userDao.getLoggedUserAuthorities()}" />
<html>

<head>
	<link type="text/css" rel="stylesheet" href="css/bootstrap-select.min.css "> 
	<link type="text/css" rel="stylesheet" href="css/bootstrap.min.css">
	<link rel ="stylesheet" type="text/css" href="css/role_manager.css" title="style">
	<script type="text/javascript" src="js/jquery-1.12.4.min.js"></script>
	<script type="text/javascript">
		function highlightMe(anchorObj)
		{
			$("#leftMenu a").each(function() {
				if (this == anchorObj)
					$(this).addClass("active");
				else
					$(this).removeClass("active");
			});
		}
	</script>
</head>

<body leftmargin="2" rightmargin="2" onLoad="window.parent.frames['managementFrame'].document.body.style.backgroundColor='#f0f0f0';">

<div id="leftMenu" class="margin-top margin-left">
	<p style="font-weight:bold;">
	</p>
	<c:if test="${userDao.canLoggedUserWriteToSystem()}">
		<a class="btn btn-sm btn-primary" style="width:130px;" href="<c:url value="<%= BackOfficeController.moduleListPageURL %>" />" target="managementFrame" onClick="highlightMe(this);">Manage databases</a>
		<br/><br/>
	</c:if>
	<a class="btn btn-sm btn-primary" style="width:130px;" href="<c:url value="<%= UserPermissionController.userListPageURL %>" />" target="managementFrame" onClick="highlightMe(this);">
		<c:choose>
			<c:when test="${userDao.canLoggedUserWriteToSystem()}">
				Manage users<br/>and permissions
			</c:when>
			<c:otherwise>
				Manage account
			</c:otherwise>
		</c:choose>
	</a>
	<c:if test="${(fn:contains(loggedUserAuthorities, adminRole) || !userDao.getSupervisedModules(loggedUserAuthorities).isEmpty()) && actionRequiredToEnableDumps eq ''}">
		<br/><br/>
		<a class="btn btn-sm btn-primary" style="width:130px;" href="<c:url value="<%= BackOfficeController.processListPageURL %>" />" target="managementFrame" onClick="highlightMe(this);">Admin processes</a>
	</c:if>
</div>

</body>

</html>