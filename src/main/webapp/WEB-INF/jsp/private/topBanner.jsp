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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="fr.cirad.security.base.IRoleDefinition,fr.cirad.web.controller.BackOfficeController,org.springframework.security.core.context.SecurityContextHolder" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<c:set var="loggedUser" value="<%= SecurityContextHolder.getContext().getAuthentication().getPrincipal() %>" />
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />

<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<link rel="stylesheet" type="text/css" href="css/role_manager.css" />
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
				setTimeout(
					"checkHover()",
					0); // si vous souhaitez retarder la disparition, c'est ici
			});
		});
	</script>
</head>

<body style='margin:0px; overflow:hidden; height:100%;'>

	<form style='margin:0px;'>

		<c:if test="${loggedUser ne null}">
			<div style='position:absolute; left:80%; width:20%;'>
				<div style="text-align:right; margin-top:8px;">Logged in as <b>${loggedUser.username}</b>
				<a target='_top' style='padding:2px; border:1px solid #e0e0e0; float:right; background-color:#fff;  margin-top:-3px; margin-left:10px; margin-right:5px;' href="../j_spring_security_logout">Log-out</a>
				</div>
			</div>
		</c:if>

		<div style='width:100%; background-color:#21A32C; height:25px;'>
		<table cellpadding="0" cellspacing="0">
		<tr>
			<td width='10'></td>
			<td style='font-weight:bold; font-size:20px;'>
				<a href='<c:url value="<%= BackOfficeController.mainPageURL %>" />' target="_top" style='color:#000;'><%= request.getContextPath().substring(1).toUpperCase() %> - PRIVATE AREA</a>
			</td>
		</tr>
		</table>
		</div>

	</form>

