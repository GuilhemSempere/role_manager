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
<c:set var='entityManagerRole' value='<%= IRoleDefinition.ENTITY_MANAGER_ROLE %>' />
<c:set var='isManager' value="${fn:contains(user.authorities, param.module.concat(roleSep).concat(param.entityType).concat(roleSep).concat(entityManagerRole).concat(roleSep).concat(entity.key))}" />

<html>

<head>
	<script type="text/javascript" src="js/jquery-1.12.4.min.js"></script>
	<link rel ="stylesheet" type="text/css" href="css/role_manager.css" title="style">
	<link type="text/css" rel="stylesheet" href="css/bootstrap-select.min.css ">
	<link type="text/css" rel="stylesheet" href="css/bootstrap.min.css">
	<link media="screen" type="text/css" href="../css/main.css" rel="StyleSheet" />

	<script type="text/javascript">
	var dirty = false;

	function removeItem(entityId, entityName)
	{
		let itemRow = $("#row_" + entityId);
		if (confirm("Do you really want to discard ${param.entityType} " + entityName + "?\nThis will delete all data it contains."))
		{
			itemRow.find("td:eq(2)").prepend("<div style='position:absolute; margin-left:60px; margin-top:5px;'><img src='img/progress.gif'></div>");
		    $.ajax({
		        	url: '<c:url value="<%= BackOfficeController.moduleEntityRemovalURL %>" />',
		        	data : { module:'${param.module}', entityType:'${param.entityType}', entityId:entityId },
		        	success: function(deleted) {
						itemRow.find("td:eq(2) div").remove();
						if (!deleted)
							alert("Unable to discard " + entityName);
			        	else {
							itemRow.remove();
							dirty = true;
			        	}
		        	},
			        error: function (xhr, ajaxOptions, thrownError) {
			            handleError(xhr, ajaxOptions, thrownError);
			            location.reload();
			        }
			});
		}
	}

    function handleError(xhr) {
      	var errorMsg;
      	if (xhr != null && xhr.responseText != null) {
      		try {
      			errorMsg = $.parseJSON(xhr.responseText)['errorMsg'];
      		}
      		catch (err) {
      			errorMsg = xhr.responseText;
      		}
      	}
      	alert(errorMsg);
      }

	function toggleVisibility(entityId, entityName)
	{
		let itemRow = $("#row_" + entityId);
		let visibilityCell = itemRow.find("td:eq(1)");
		let setAsPublic = visibilityCell.find("input").is(":checked");
		itemRow.find("td:eq(2)").prepend("<div style='position:absolute; margin-left:60px; margin-top:5px;'><img src='img/progress.gif'></div>");
		$.getJSON('<c:url value="<%= BackOfficeController.moduleEntityVisibilityURL %>" />', { module:'${param.module}', entityType:'${param.entityType}', entityId:entityId, public:setAsPublic }, function(updated){
			if (!updated)
			{
				itemRow.find("td:eq(2) div").remove();
				visibilityCell.find("input").prop("checked", !setAsPublic);
				alert("Unable to set visibility to " + (setAsPublic ? "public" : "private") + " for " + entityName);
			}
			else
			{
				itemRow.find("td:eq(2) div").html("Change applied!");
				setTimeout(function() {itemRow.find("td:eq(2) div").remove();}, 1000);
				dirty = true;
			}
		});
	}
	</script>
</head>

<body style='background-color:#f0f0f0; text-align:center;' onload="$('#hlContentDialogClose', parent.document).on('click', function() { if (dirty) parent.location.reload(); });">
	<form>
		<table cellpadding='4' cellspacing='0' border='0'>
		  <tr>
		    <td width='5'>&nbsp;</td>
			<th valign='top'>
			  <table cellpadding='2' cellspacing='0' class='adminListTable margin-top-md'>
				<tr>
					<th>${param.entityType} name</th>
					<c:if test="${visibilitySupported}"><th>Public</th></c:if>
					<th>Removal</th>
				</tr>
				<c:forEach var="entity" items="${publicEntities}">
				<tr id="row_${entity.key}">
					<td>${entity.value}</td>
					<c:if test="${visibilitySupported}"><td><input type='checkbox' checked onclick='toggleVisibility("${entity.key}", "${entity.value}");'></td></c:if>
					<td align='center'><a style='padding-left:10px; padding-right:10px;' href='javascript:removeItem("${entity.key}", "${entity.value}");' title='Discard ${param.entityType}'><img src='img/delete.gif'></a></td>
				</tr>
				</c:forEach>
				<c:if test="${privateEntities ne null}">
					<c:forEach var="entity" items="${privateEntities}">
					<tr id="row_${entity.key}">
						<td>${entity.value}</td>
						<c:if test="${visibilitySupported}"><td><input type='checkbox' onclick='toggleVisibility("${entity.key}", "${entity.value}");'></td></c:if>
						<td align='center'><a style='padding-left:10px; padding-right:10px;' href='javascript:removeItem("${entity.key}", "${entity.value}");' title='Discard ${param.entityType}'><img src='img/delete.gif'></a></td>
					</tr>
					</c:forEach>
				</c:if>
			  </table>
		  </tr>
		</table>
	</form>
</body>

</html>