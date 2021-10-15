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
<c:set var="loggedUser" value="<%= SecurityContextHolder.getContext().getAuthentication().getPrincipal() %>" />
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />

<html>
<head>
	<link type="text/css" rel="stylesheet" href="css/bootstrap-select.min.css "> 
	<link type="text/css" rel="stylesheet" href="css/bootstrap.min.css">
	<link media="screen" type="text/css" href="css/role_manager.css" rel="StyleSheet" />
	<link media="screen" type="text/css" href="../css/main.css" rel="StyleSheet" />
	<script type="text/javascript" src="js/jquery-1.12.4.min.js"></script>
	<script type="text/javascript" src="js/bootstrap.min.js"></script>
	<script type="text/javascript">
		const progressQueryURL = '<c:url value="<%= BackOfficeController.backupStatusQueryURL %>" />';
		const processID = '${processID}';
		const queryInterval = 1000;
		const nonFinalStatus = ["idle", "running"];
		
		$(document).on("ready", function (){
		    queryStatus(true);
		});
		
		function updateStatus(status){
		    $("#statusCode").html(status.status);
		    $("#statusMessage").html(status.message);
		    $("#log").html(status.log.join("\n"));
		}
		
		function queryStatus(){
		    $.ajax({
		        url: progressQueryURL,
		        method: "GET",
		        data: {processID},
		        dataType: "json",
		    }).then(function (result){
		        updateStatus(result);
		        if (nonFinalStatus.includes(result.status)){
		            setTimeout(queryStatus, queryInterval);
		        }
		    });
		}
	</script>
</head>
<body style='background-color:#f0f0f0;'>	
	<h1 style="text-align:center;">Status of backup process ${processID}</h1>
	<div id="statusCode" style="text-align:center; text-transform:capitalize; font-size:24"></div>
	<div id="statusMessage" style="text-align:center;"></div>
	<pre id="log" style="max-width:600px; margin:auto;"></pre>
</body>
</html>