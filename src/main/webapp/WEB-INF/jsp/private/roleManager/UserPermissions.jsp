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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="fr.cirad.web.controller.security.UserPermissionController,fr.cirad.security.base.IRoleDefinition,org.springframework.security.core.context.SecurityContextHolder" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="urlEncoder" scope="page" class="fr.cirad.web.controller.security.UserPermissionController" /><%-- dummy controller just to be able to invoke a static method --%>
<c:set var='roleSep' value='<%= UserPermissionController.ROLE_STRING_SEPARATOR %>' />
<c:set var="loggedUserAuthorities" value="${userDao.getLoggedUserAuthorities()}" />
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />
<c:set var='supervisorRole' value='<%= IRoleDefinition.ROLE_DB_SUPERVISOR %>' />
<c:set var='entityManagerRole' value='<%= IRoleDefinition.ENTITY_MANAGER_ROLE %>' />
<%-- <c:set var='isManager' value="${fn:contains(user.authorities, param.module.concat(roleSep).concat(param.entityType).concat(roleSep).concat(entityManagerRole).concat(roleSep).concat(entity.key))}" /> --%>

<html>

<head>
	<link type="text/css" rel="stylesheet" href="../css/bootstrap-select.min.css "> 
	<link type="text/css" rel="stylesheet" href="../css/bootstrap.min.css">
	<link media="screen" type="text/css" href="../css/role_manager.css" rel="StyleSheet" />
	<link media="screen" type="text/css" href="../../css/main.css" rel="StyleSheet" />
	<script type="text/javascript" src="../js/jquery-1.12.4.min.js"></script>
	<script type="text/javascript">
		function doOnLoad(module)
		{
			var permissionInput = $(window.parent.document).find("input[type='hidden'][name='${param.entityType}Permission_" + module + "']");
			$('input[type="radio"][value=""]').each(function() {$(this).prop('checked', true);});
	    	var permissions = permissionInput.val().split(",");
	    	for (var i=0; i<permissions.length; i++)
	    	{
				var radioButton = $('input[type="radio"][value="' + permissions[i] + '"]');
    			radioButton.prop('checked', true);
	    	}
		}

		function toggleEntityBoxes(entityBoxContainerId, enableThem)
		{
			$(entityBoxContainerId + " input").each(function() {
				$(this).attr('disabled', !enableThem);
				if (!enableThem)
					$(this).prop('checked', false);
			});
		}
	</script>
</head>

<body style='background-color:#f0f0f0; text-align:center;' onload="doOnLoad('${urlEncoder.urlEncode(param.module)}');">
	<form>
		<table cellpadding='4' cellspacing='0' border='0' class="margin-top-sm">
		  <tr>
		    <td width='5'>&nbsp;</td>
			<th valign='top'>
			  Public ${param.entityType} entities
			  <table cellpadding='2' cellspacing='0' class='adminListTable margin-top-sm'>
				<tr>
					<th>${param.entityType} name</th>
					<th>${param.entityType} permissions</th>
				</tr>
				<c:forEach var="entity" items="${publicEntities}">
				<c:if test="${fn:contains(loggedUserAuthorities, adminRole) || fn:contains(loggedUserAuthorities, param.module.concat(roleSep).concat(supervisorRole)) || fn:contains(loggedUserAuthorities, param.module.concat(roleSep).concat(param.entityType).concat(roleSep).concat(entityManagerRole).concat(roleSep).concat(entity.key))}">
				<tr>
					<td>${entity.value}</td>
					<td nowrap align='left'>
						<c:forEach var="role" items="${roles}">
							<span>
							<input type='radio' name='permission_${entity.key}' value='${urlEncoder.urlEncode(param.module.concat(roleSep).concat(param.entityType).concat(roleSep).concat(role).concat(roleSep).concat(entity.key))}'>${role}&nbsp;
							</span>
						</c:forEach>
						<span>
							<input type='radio' name='permission_${entity.key}' value=''>NONE
						</span>
					</td>
				</tr>
				</c:if>
				</c:forEach>
			  </table>
			</th>
			<c:if test="${privateEntities ne null}">
		    <td width='20'>&nbsp;</td>
			<th valign='top'>
			  Private ${param.entityType} entities
			  <table cellpadding='2' cellspacing='0' class='adminListTable margin-top-sm'>
				<tr>
					<th>${param.entityType} name</th>
					<th>${param.entityType} permissions</th>
				</tr>
				<c:forEach var="entity" items="${privateEntities}">
				<c:if test="${fn:contains(loggedUserAuthorities, adminRole) || fn:contains(loggedUserAuthorities, param.module.concat(roleSep).concat(supervisorRole)) || fn:contains(loggedUserAuthorities, param.module.concat(roleSep).concat(param.entityType).concat(roleSep).concat(entityManagerRole).concat(roleSep).concat(entity.key))}">
				<tr>
					<td>${entity.value}</td>
					<td nowrap align='left'>
						<c:forEach var="role" items="${roles}">
							<span>
							<input type='radio' name='permission_${entity.key}' value='${urlEncoder.urlEncode(param.module.concat(roleSep).concat(param.entityType).concat(roleSep).concat(role).concat(roleSep).concat(entity.key))}'>${role}&nbsp;
							</span>
						</c:forEach>
						<span>
							<input type='radio' name='permission_${entity.key}' value=''>NONE
						</span>
					</td>
				</tr>
				</c:if>
				</c:forEach>
			  </table>
			</th>
			</c:if>
		  </tr>
		</table>
	</form>
</body>

</html>