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
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
	import="fr.cirad.security.base.IRoleDefinition,fr.cirad.web.controller.BackOfficeController,org.springframework.security.core.context.SecurityContextHolder"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<c:set var="loggedUser"
	value="<%=SecurityContextHolder.getContext().getAuthentication().getPrincipal()%>" />
<c:set var='adminRole' value='<%=IRoleDefinition.ROLE_ADMIN%>' />

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<link rel="stylesheet" type="text/css" href="css/role_manager.css" />
<link rel="stylesheet" type="text/css" href="css/bootstrap.min.css" />
<script type="text/javascript" src="js/jquery-1.12.4.min.js"></script>
<script type="text/javascript">
	var obj = null;

	function checkHover() {
		if (obj) {
			obj.find('ul').fadeOut('fast');
		} //if
	} //checkHover

	$(document).ready(function() {
		$('#Nav > li').hover(function() {
			if (obj) {
				obj.find('ul').fadeOut('fast');
				obj = null;
			} //if

			$(this).find('ul').fadeIn('fast');
		}, function() {
			obj = $(this);
			setTimeout("checkHover()", 0); // si vous souhaitez retarder la disparition, c'est ici
		});
	});
</script>
</head>

<body>
	<div
		style='background: background: linear-gradient(0deg, rgba(31,107,38,1) 0%, rgba(33,163,44,1) 100%);
height: 47px; display: flex; flex-direction: row; justify-content: space-between; align-items: center; padding-left: 15px; padding-right: 15px'>
		<div style='font-weight: bold; font-size: 20px; color: white;'>
			<a href='<c:url value="<%=BackOfficeController.mainPageURL%>" />'
				target="_top"
				style='color: black; font-size: 20px; text-decoration: none;'><%=request.getContextPath().substring(1).toUpperCase()%>
				- PRIVATE AREA</a>
		</div>
		<c:if test="${loggedUser ne null}">
			<div style="color: white">
				Logged in as <b>${loggedUser.username}</b>
			</div>
			<a target='_top' href="../j_spring_security_logout"
				style="color: white; display: flex; align-items: center; gap: 8px; text-decoration: none; padding: 0 10px; height: 100%;"
				onmouseover="this.style.backgroundColor = '#15531b';"
				onmouseout="this.style.backgroundColor = '';"> <span
				class="glyphicon glyphicon-log-out margin-icon"
				style="margin-bottom: 4px;" aria-hidden="true"></span>
				<p style="margin-bottom: 0">Log-out</p>
			</a>
		</c:if>
	</div>
</body>