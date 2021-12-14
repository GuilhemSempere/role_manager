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
<c:set var="loggedUser" value="<%= SecurityContextHolder.getContext().getAuthentication().getPrincipal() %>" />
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />

<html>

<head>
	<link type="text/css" rel="stylesheet" href="../css/bootstrap-select.min.css "> 
	<link type="text/css" rel="stylesheet" href="../css/bootstrap.min.css">
	<link media="screen" type="text/css" href="../css/role_manager.css" rel="StyleSheet" />
	<link media="screen" type="text/css" href="../../css/main.css" rel="StyleSheet" />
	<script type="text/javascript" src="../js/jquery-1.12.4.min.js"></script>
	<script type="text/javascript" src="../js/jquery.cookie.js"></script>
	<script type="text/javascript" src="../js/listNav.js"></script>
	<script type="text/javascript">
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		function removeItem(itemId)
		{
			if (confirm("Do you really want to discard " + itemId + "?"))
				$.getJSON('<c:url value="<%= UserPermissionController.userRemovalURL %>" />', { user:itemId }, function(deleted){
					if (deleted)
						loadCurrentPage();
					else
						alert("Unable to discard " + itemId);
				});
		}
		</c:if>

		function loadCurrentPage()
		{
	        $("body").scrollTop(0);

			$.getJSON('<c:url value="<%=UserPermissionController.userListCountURL%>" />', { loginLookup:$('#loginLookup').val() }, function(jsonCountResult){
				var totalRecordCount = parseInt(jsonCountResult);

				$.getJSON('<c:url value="<%=UserPermissionController.userListDataURL%>" />', { loginLookup:$('#loginLookup').val(),page:getVariable("pageNumber"),size:getVariable("pageSize") }, function(jsonResult){
					nAddedRows = 0;

					$("#userResultTable tr:gt(0)").remove();
					for (var key in jsonResult)
					{
					   	rowContents = "";
				   		if (jsonResult[key] != null)
				   		{
						   	for (var subkey in jsonResult[key])
							{
						   		cellData = "" + jsonResult[key][subkey];
								rowContents += "<td nowrap>" + cellData.replace(/\n/g, "<br>") + "</td>";
							}
							if (subkey == jsonResult[key].length - 1<c:if test="${!fn:contains(loggedUser.authorities, adminRole)}"> && jsonResult[key][0] != "${loggedUser.username}"</c:if>)
							{
								rowContents += "<td style='border:none;' nowrap>&nbsp;<a href='<c:url value="<%= UserPermissionController.userDetailsURL %>" />?user=" + encodeURIComponent(jsonResult[key][0]) + "' title='User details'><img src='../img/magnifier.gif'></a>";
								<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
								if ("(ADMINISTRATOR)" != jsonResult[key][1])
									rowContents += "&nbsp;&nbsp;&nbsp;<a href='javascript:removeItem(\"" + encodeURIComponent(jsonResult[key][0]) + "\");' title='Discard user'><img src='../img/delete.gif'></a>";
								</c:if>
								rowContents += "</td>";
				   			}
					   		add_new_row('#userResultTable', '<tr>' + rowContents + '</tr>');
					   		nAddedRows++;
				   		}
				   		else
				   			break;
					}

					if (getVariable("pageNumber") > 0)
						$('#previousButton').show();
					else
						$('#previousButton').hide();

					lastRecordIndex = getVariable("pageSize") * getVariable("pageNumber") + nAddedRows;
					if (lastRecordIndex == totalRecordCount)
						$('#nextButton').hide();
					else
						$('#nextButton').show();

					$("#listCounter").text(Math.min(lastRecordIndex, 1 + getVariable("pageSize") * getVariable("pageNumber")) + " - " + lastRecordIndex + " / " + totalRecordCount);
				});
			});
		}
		
		function initialiseNavigationVariables()
		{
			setVariable("pageSize", 25);
			setVariable("pageNumber", 0);
			setVariable("sortBy", "");
			setVariable("sortDir", "desc");
		}
		
		initialiseNavigationVariables();
	</script>
</head>

<body style='background-color:#f0f0f0;' onload="applySorting();">

<form onsubmit="return false;">
<table>
<tr>
	<td>
		<b>Login lookup</b><br>
		<input id="loginLookup" class="navListFilter" type='text' style='width:100px;' onChange="setVariable('pageNumber', 0); loadData();" onkeypress="if (event.keyCode == 13) blur();">
	</td>
	<td id="pageSizer"></td>
</tr>
</table>
</form>

<table class="resultTableNavButtons">
<tr>
	<td nowrap style='width:70px;'><a id="previousButton" href="javascript:incrementVariable('pageNumber', true);  loadData();">&lt; Previous</a></td>
	<td nowrap id='listCounter'></td>
	<td nowrap>&nbsp;<a id="nextButton" class="contentDialog" href="javascript:incrementVariable('pageNumber', false); loadData();">Next &gt;</a></td>
</tr>
</table>

<table class="adminListTable" id="userResultTable">
<tr>
	<th>Login</th>
	<th>Accessible modules</th>
	<th>Authentication method</th>
</tr>
<tr>
	<td colspan='6' height='200'></td>
</tr>
</table>

<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
<br><a class="btn btn-sm btn-primary" href="<c:url value="<%=UserPermissionController.userDetailsURL%>" />">Create user</a>
</c:if>

</body>

</html>