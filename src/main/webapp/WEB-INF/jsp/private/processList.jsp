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
	if (this == top)
		location.href = "..";

	const progressQueryURL = '<c:url value="<%= BackOfficeController.processListStatusURL %>" />';
	const queryInterval = 2000;
	const nonFinalStatus = ["idle", "running"];

	$(document).on("ready", function (){
	    queryStatus();
	});

	function updateStatus(processList){
	    const table = $("#processList");
	    table.empty();

	    let tableContent = "";
	    processList.forEach(function (status){
	        tableContent += "<tr><td>" + status.processID + "</td><td>" + status.module + "</td><td>" + status.status + "</td><td>" + status.message + "</td>";

	        const statusPageURL = '<c:url value="<%= BackOfficeController.dumpStatusPageURL %>" />?module=' + status.module + '&processID=' + status.processID;
	        tableContent += '<td align="center"><a href="' + statusPageURL + '" target="_blank"><img id="igvTooltip" style="cursor:pointer; cursor:hand;" src="img/magnifier.gif"></a></td>';
	        tableContent += "</tr>";
	    });
	    table.html(tableContent);
	}

	function queryStatus(){
	    $.get(progressQueryURL).then(function (result){
	        updateStatus(result);
	    });
	}
	</script>
</head>

<body style='background-color:#f0f0f0;'>
	<h2>Admin processes</h2>
	<button class="btn btn-sm btn-primary" onclick="queryStatus()">Refresh</button>
	<br /><br />
	<table class="adminListTable">
		<thead>
			<tr>
				<th>Process ID</th>
				<th>Database</th>
				<th>Status</th>
				<th>Execution message</th>
				<th>Details</th>
			</tr>
		</thead>
		<tbody id="processList"></tbody>
	</table>
</body>

</html>