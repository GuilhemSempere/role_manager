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
	
	<style type="text/css">
		td.subEntities div {
			display:inline;
			border:1px solid #cccc80;
			background-color:#ffffc0;
			margin:0 2px;
			padding:0px 8px
		}
	</style>


	<script type="text/javascript">
	var dirty = false;

	function removeItem(entityType, allLevelEntityIDs, entityName)
	{
		let itemRow = $("#row_" + allLevelEntityIDs[0]);
		if (confirm("Do you really want to discard " + entityType.replace(".", " ") + " " + entityName + "?\nThis will delete all data it contains."))
		{
			itemRow.find("td:eq(2)").prepend("<div style='position:absolute; margin-left:60px; margin-top:5px;'><img src='img/progress.gif'></div>");
		    $.ajax({
		        	url: '<c:url value="<%= BackOfficeController.moduleEntityRemovalURL %>" />',
		            method: "DELETE",
		            contentType: "application/json;charset=utf-8",
		        	data : JSON.stringify({ module:'${param.module}', entityType:entityType, allLevelEntityIDs:allLevelEntityIDs }),
		        	success: function(deleted) {
						itemRow.find("td:eq(2) div").remove();
						if (!deleted)
							alert("Unable to discard " + entityName);
			        	else {
			        		if (allLevelEntityIDs.length == 1)
								itemRow.remove();
			        		else
			        			 $("#subEntity_" + allLevelEntityIDs.join("_")).remove();
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
		
	    $.ajax({
        	url: '<c:url value="<%= BackOfficeController.moduleEntityVisibilityUpdateURL %>" />',
            method: "POST",
        	data : { module:'${param.module}', entityType:'${param.entityType}', entityId:entityId, public:setAsPublic },
        	success: function(updated) {
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
        	},
	        error: function (xhr, ajaxOptions, thrownError) {
	            handleError(xhr, ajaxOptions, thrownError);
	        }
		});
	}
	
	function displaySubEntityInfo(subEntityType, mainEntityId, subEntityId) {
	    $.ajax({
        	url: '<c:url value="<%= BackOfficeController.moduleEntityInfoURL %>" />',
            method: "POST",
            contentType: "application/json;charset=utf-8",
        	data : JSON.stringify({ module:'${param.module}', entityType:'${param.entityType}.' + subEntityType, allLevelEntityIDs:[mainEntityId, subEntityId] }),
        	success: function(description) {
				alert(description);
        	},
	        error: function (xhr, ajaxOptions, thrownError) {
	            handleError(xhr, ajaxOptions, thrownError);
	            location.reload();
	        }
		});
	}
	
	function removeSubEntity(subEntityType, subEntityId) {
		alert(subEntityType + " -> " + subEntityId);
	}
	
	function saveDesc(entityId, textAreaNode) {
// 		let itemRow = $("#row_" + entityId);
// 		let visibilityCell = itemRow.find("td:eq(1)");
// 		let setAsPublic = visibilityCell.find("input").is(":checked");
// 		itemRow.find("td:eq(2)").prepend("<div style='position:absolute; margin-left:60px; margin-top:5px;'><img src='img/progress.gif'></div>");
		
	    $.ajax({
        	url: '<c:url value="<%= BackOfficeController.moduleEntityDescriptionUpdateURL %>" />',
            method: "POST",
        	data : { module:'${param.module}', entityType:'${param.entityType}', entityId:entityId, desc:textAreaNode.value },
        	success: function(updated) {
    			if (!updated)
    			{
//     				itemRow.find("td:eq(2) div").remove();
//     				visibilityCell.find("input").prop("checked", !setAsPublic);
    				alert("Unable to update description for " + entityName);
    			}
    			else
    			{
//     				itemRow.find("td:eq(2) div").html("Change applied!");
//     				setTimeout(function() {itemRow.find("td:eq(2) div").remove();}, 1000);
//     				dirty = true;
    				textAreaNode.parentNode.style.backgroundColor=null;
    			}
        	},
	        error: function (xhr, ajaxOptions, thrownError) {
	            handleError(xhr, ajaxOptions, thrownError);
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
					<c:if test="${descriptionSupported}"><th>Description</th></c:if>
					<c:if test="${visibilitySupported}"><th>Public</th></c:if>
					<c:forEach var="subEntityType" items="${subEntityTypes}">
						<th>${subEntityType} sub-entities</th>
					</c:forEach>
					<th>removal</th>
				</tr>
				<c:forEach var="entity" items="${publicEntities}">
				<tr id="row_${entity.key}">
					<td>${entity.value[0]}</td>
					<c:if test="${descriptionSupported}"><td><textarea cols='60' rows='3' onchange='parentNode.style.backgroundColor="#FFA500"; if (confirm("Save description for ${param.entityType} ${entity.value[0]}?")) saveDesc("${entity.key}", this);'>${entity.value[1]}</textarea></td></c:if>
					<c:if test="${visibilitySupported}"><td><input type='checkbox' checked onclick='toggleVisibility("${entity.key}", "${entity.value}");'></td></c:if>
					<c:forEach var="subEntityType" items="${subEntityTypes}">
					<td class="subEntities" style="padding-bottom:2px;">
						<c:forEach var="subEntity" items="${subEntities[entity.key][subEntityType]}">
							<div id="subEntity_${entity.key}_${subEntity.key}">
							<a href="#" onclick='displaySubEntityInfo("${subEntityType}", "${entity.key}", "${subEntity.key}");'><span role='button' title="Click for details" class="glyphicon glyphicon-info-sign" style="color:green;"></span></a>
							${subEntity.value}
							<c:if test="${subEntities[entity.key][subEntityType].size() > 1}">
							<a href="#" onclick='removeItem("${param.entityType}.${subEntityType}", ["${entity.key}", "${subEntity.key}"], "${subEntity.value}");'><span role='button' title="Remove ${subEntityType}" class="glyphicon glyphicon-trash" style="color:red;"></span></a>
							</c:if>
							</div>
						</c:forEach>
					</td>
					</c:forEach>
					<td align='center'><a style='padding-left:10px; padding-right:10px;' href='javascript:removeItem("${param.entityType}", ["${entity.key}"], "${entity.value}");' title='Discard ${param.entityType}'><img src='img/delete.gif'></a></td>
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