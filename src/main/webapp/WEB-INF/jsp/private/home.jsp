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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="fr.cirad.web.controller.BackOfficeController,fr.cirad.security.base.IRoleDefinition,org.springframework.security.core.context.SecurityContextHolder" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<c:set var="mainPageURL" value="<%= BackOfficeController.mainPageURL %>" />
<c:set var="loggedUserAuthorities" value="${userDao.getLoggedUserAuthorities()}" />
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />

<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<link media="screen" type="text/css" href="css/role_manager.css" rel="StyleSheet" />
	</head>
	<body>
		<c:choose>
		<c:when test="${(fn:length(moduleNames) == 1 && !fn:contains(loggedUserAuthorities, adminRole))}">
			<script language="javascript">
				top.location.href = '<c:url value="${mainPageURL}" />?module=${moduleNames[0]}';
			</script>
		</c:when>
		<c:otherwise>
			<c:if test="${!empty menuItems}">
			<div style='float:right; border:1px dashed #000; width:200px; min-height:200px; margin:10px; padding:20px;'>
				<c:forEach var="menuItem" items="${menuItems}">
					<a style='font-weight:bold;' href="${menuItem.value}">${menuItem.key}</a><br>
				</c:forEach>
			</div>
			</c:if>
		</c:otherwise>
		</c:choose>
	</body>
</html>