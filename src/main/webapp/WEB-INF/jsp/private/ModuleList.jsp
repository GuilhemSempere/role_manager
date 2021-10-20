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
		var moduleData;
		
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		function createModule(moduleName, host)
		{
			let itemRow = $("#row_" + moduleName);
			$.getJSON('<c:url value="<%= BackOfficeController.moduleCreationURL %>" />', { module:moduleName,host:host }, function(created){
				if (!created)
					alert("Unable to create " + moduleName);
				else
				{
					$("#newModuleName").val("");
					$("#newModuleName").keyup();
					moduleData[moduleName] = {	'<%= BackOfficeController.DTO_FIELDNAME_HOST %>' : $("select#hosts").val(),
												'<%= BackOfficeController.DTO_FIELDNAME_PUBLIC %>' : false,
												'<%= BackOfficeController.DTO_FIELDNAME_HIDDEN %>' : false
					}
					$('#moduleTable tbody').prepend(buildRow(moduleName));
				}
			}).error(function(xhr) { handleError(xhr); });
		}
		
		function handleError(xhr) {
			if (!xhr.getAllResponseHeaders())
				return;	// user is probably leaving the current page
			
		    if (xhr.status == 403) {
		        alert("You do not have access to this resource");
		        return;
		    }

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
		
		function removeItem(moduleName)
		{
			let itemRow = $("#row_" + moduleName);
			if (confirm("Do you really want to discard database " + moduleName + "?\nThis will delete all data it contains.")) {
				itemRow.find("td:eq(6)").prepend("<div style='position:absolute; margin-left:60px; margin-top:5px;'><img src='img/progress.gif'></div>");
				$.getJSON('<c:url value="<%= BackOfficeController.moduleRemovalURL %>" />', { module:moduleName }, function(deleted){
					if (!deleted) {
						alert("Unable to discard " + moduleName);
						itemRow.find("td:eq(6) div").remove();
					}
					else
					{
						delete moduleData[moduleName];
						itemRow.remove();
					}
				}).error(function(xhr) { itemRow.find("td:eq(6) div").remove(); handleError(xhr); });
			}
		}

		function saveChanges(moduleName)
		{
			let itemRow = $("#row_" + moduleName);
			let setToPublic = itemRow.find(".flagCol1").prop("checked");
			let setToHidden = itemRow.find(".flagCol2").prop("checked");
			$.getJSON('<c:url value="<%= BackOfficeController.moduleVisibilityURL %>" />', { module:moduleName,public:setToPublic,hidden:setToHidden }, function(updated){
				if (!updated)
					alert("Unable to apply changes for " + moduleName);
				else
				{
					moduleData[moduleName][0] = setToPublic;
					moduleData[moduleName][1] = setToHidden;
					setDirty(moduleName, false);
				}
			}).error(function(xhr) { handleError(xhr); });
		}
		
		function resetFlags(moduleName)
		{
			let itemRow = $("#row_" + moduleName);
			itemRow.find(".flagCol1").prop("checked", moduleData[moduleName][0]);
			itemRow.find(".flagCol2").prop("checked", moduleData[moduleName][1]);
			setDirty(moduleName, false);
		}
		
		function setDirty(moduleName, flag)
		{
			let itemRow = $("#row_" + moduleName);
			itemRow.css("background-color", flag ? "#ffff80" : "");
			itemRow.find(".resetButton").prop("disabled", !flag);
			itemRow.find(".applyButton").prop("disabled", !flag);
		}
		</c:if>

		
		function buildRow(key)
		{
		   	let rowContents = "<td>" + key + "</td>";
		   	
		   	<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
	   		if (moduleData[key] != null) {
	   			rowContents += "<td>" + moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_HOST %>'] + "</td>";
				rowContents += "<td><input onclick='setDirty(\"" + encodeURIComponent(key) + "\", true);' class='flagCol1' type='checkbox'" + (moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_PUBLIC %>'] ? " checked" : "") + "></td>";
				rowContents += "<td><input onclick='setDirty(\"" + encodeURIComponent(key) + "\", true);' class='flagCol2' type='checkbox'" + (moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_HIDDEN %>'] ? " checked" : "") + "></td>";
			}
			</c:if>

			rowContents += "<td>";
			<c:forEach var="level1Type" items="${rolesByLevel1Type}">
			rowContents += "<a id='${urlEncoder.urlEncode(moduleName)}_${level1Type.key}PermissionLink' style='text-transform:none;' href=\"javascript:openModuleContentDialog('${loggedUser.username}', '" + key + "', '${level1Type.key}');\">${level1Type.key} entities</a>";
			</c:forEach>
			rowContents += "</td>";
			
			rowContents += "<td><a href=\"javascript:openModuleBackupDialog('" + key + "');\">module backups</a></td>";
			
			<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
	   		rowContents += "<td><input type='button' value='Reset' class='resetButton btn btn-default btn-sm' disabled onclick='resetFlags(\"" + encodeURIComponent(key) + "\");'><input type='button' class='applyButton btn btn-default btn-sm' value='Apply' disabled onclick='saveChanges(\"" + encodeURIComponent(key) + "\");'></td>";
	   		rowContents += "<td align='center'><a style='padding-left:10px; padding-right:10px;' href='javascript:removeItem(\"" + encodeURIComponent(key) + "\");' title='Discard module'><img src='img/delete.gif'></a></td>";
	   		</c:if>
	   		return '<tr id="row_' + encodeURIComponent(key) + '">' + rowContents + '</tr>';
		}

		function loadData()
		{
			let tableBody = $('#moduleTable tbody');
			$.getJSON('<c:url value="<%=BackOfficeController.moduleListDataURL%>" />', {}, function(jsonResult){
				moduleData = jsonResult;
				nAddedRows = 0;
				for (var key in moduleData)
			   		tableBody.append(buildRow(key));
			});
			
			$.getJSON('<c:url value="<%=BackOfficeController.hostListURL%>" />', {}, function(jsonResult){
				$("#hosts").html("");
				for (var key in jsonResult)
					$("#hosts").append("<option value='" + jsonResult [key]+ "'>" + jsonResult [key]+ "</option>");
			});
		}
		
    	function isValidKeyForNewName(evt)
    	{
             return isValidCharForNewName((evt.which) ? evt.which : evt.keyCode);
    	}

        function isValidCharForNewName(charCode) {
            return ((charCode >= 48 && charCode <= 57) || (charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122) || charCode == 8 || charCode == 9 || charCode == 35 || charCode == 36 || charCode == 37 || charCode == 39 || charCode == 45 || charCode == 46 || charCode == 95);
        }

        function isValidNewName(newName) {
        	if (newName.trim().length == 0)
        		return false;
            for (var i = 0; i < newName.length; i++)
                if (!isValidCharForNewName(newName.charCodeAt(i))) {
                    return false;
                }
            return true;
        }
        
		function openModuleContentDialog(username, module, entityType)
		{
	    	$('#moduleContentFrame').contents().find("body").html("");
	        $("#moduleContentDialog #moduleContentDialogTitle").html(entityType + " entities for user <u>" + username + "</u> in database <u id='moduleName'>" + module + "</u>");
	        $("#moduleContentDialog").modal('show');
	        $("#moduleContentFrame").attr('src', '<c:url value="<%= BackOfficeController.moduleContentPageURL %>" />?user=' + username + '&module=' + module + '&entityType=' + entityType);
		}
		
		function openModuleBackupDialog(module){
		    $("#moduleBackupDialogTitle").html("Backup management for database <u>" + module + "</u>");
			$("#moduleBackupDialog").modal('show');
			$.get('<c:url value="<%= BackOfficeController.moduleBackupInfoURL %>" />', {module: module})
				.then(function (backupData){
				    let tableHtml = "<table><tr><th>Backup file</th><th>Restore</th></tr>";
				    backupData.forEach(function (backupInfo) {
						const restoreURL = '<c:url value="<%= BackOfficeController.restoreBackupURL %>" />?module=' + module + '&backup=' + backupInfo;
						const restoreButton = '<button onclick="confirmBackupRestore(\'' + backupInfo + '\', \'' + restoreURL + '\')" class="btn btn-sm btn-primary">Restore</a>';
						tableHtml += "<tr><td>" + backupInfo + "</td><td>" + restoreButton + "</td></tr>";
				    });
				    $("#moduleBackupDialogContent").html(tableHtml);
				    
				    const newBackupURL = '<c:url value="<%= BackOfficeController.newBackupURL %>" />' + '?module=' + module;
				    $("#newBackupButton").attr("href", newBackupURL);
			});
		}
		
		function confirmBackupRestore(backupName, restoreURL){
		    $("#restoreConfirmationTitle").html("Restore backup " + backupName + " ?");
		    
		    $("#dropCheck").on("change", function (){
		    	let url = $("#confirmRestoreButton").attr("href");
		    	url = url.replace(/&drop=.*$/, "&drop=" + $("#dropCheck").is(":checked"));
		    	$("#confirmRestoreButton").attr("href", url);
		    });
		    
		    $("#confirmRestoreButton").attr("href", restoreURL + "&drop=false");
		    $("#restoreConfirmationDialog").modal("show");
		}
		
		function resizeIFrame() {
			$('#moduleContentFrame').css('height', (document.body.clientHeight - 200)+'px');
		}

	    $(document).ready(function() {
	    	resizeIFrame();
	    	loadData();
	    });
	    $(window).resize(function() {
	    	resizeIFrame();
	    });
	</script>
</head>

<body style='background-color:#f0f0f0;'>
	<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<div style="max-width:600px; padding:10px; margin-bottom:10px; border:2px dashed grey; background-color:lightgrey;">
			<b>Create new empty database</b><br/>
			On host <select id="hosts"></select> named <input type="text" id="newModuleName" onkeypress="if (!isValidKeyForNewName(event)) { event.preventDefault(); event.stopPropagation(); }" onkeyup="$(this).next().prop('disabled', !isValidNewName($(this).val()));">
			<input type="button" value="Create" class="btn btn-xs btn-primary" onclick="createModule($(this).prev().val(), $('#hosts').val());" disabled>
		</div>
	</c:if>
	<table class="adminListTable" id="moduleTable">
	<thead>
	<tr>
		<th>Database name</th>
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<th style="text-transform:capitalize;"><%= BackOfficeController.DTO_FIELDNAME_HOST %></th>
		<th style="text-transform:capitalize;"><%= BackOfficeController.DTO_FIELDNAME_PUBLIC %></th>
		<th style="text-transform:capitalize;"><%= BackOfficeController.DTO_FIELDNAME_HIDDEN %></th>
		</c:if>
		<th>Entity management</th>
		<th>Backup management</th>
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<th>Changes</th>
		<th>Removal</th>
		</c:if>
	</tr>
	</thead>
	<tbody>
	</tbody>
	</table>

	<div class="modal fade" tabindex="-1" role="dialog" id="moduleContentDialog" aria-hidden="true">
		<div class="modal-dialog modal-lg">
			<div class="modal-content">
				<div class="modal-header" id="projectInfoContainer">
					<div id="moduleContentDialogTitle" style='font-weight:bold; margin-bottom:5px;'></div>
					<iframe style='margin-bottom:10px; width:100%;' id="moduleContentFrame" name="moduleContentFrame"></iframe>
					<br>
					<form>
						<input type='button' class='btn btn-sm btn-primary' value='Close' id="hlContentDialogClose" onclick="$('#moduleContentDialog').modal('hide');" />
					</form>
				</div>
			</div>
		</div>
	</div>
	
	<div class="modal fade" tabindex="-1" role="dialog" id="moduleBackupDialog" aria-hidden="true">
		<div class="modal-dialog modal-lg" style="background-color:white;">
			<div class="modal-header">
				<div id="moduleBackupDialogTitle" style="font-weight:bold; margin-bottom:5px;"></div>
			</div>
			<div class="modal-content">
				<div id="moduleBackupDialogContent"></div>
				<div id="moduleBackupManagementOptions">
					<a id="newBackupButton" class="btn btn-sm btn-primary" target="_blank">New backup</a>
				</div>
				<input type="button" class="btn btn-sm btn-primary" value="Close" id="hlBackupDialogClose" onclick="$('#moduleBackupDialog').modal('hide');" />
			</div>
		</div>
	</div>
	
	<div class="modal fade" role="dialog" id="restoreConfirmationDialog" aria-hidden="true">
		<div class="modal-dialog modal-md" style="background-color:white;">
			<div class="modal-header">
				<div id="restoreConfirmationTitle" style="font-weight:bold; margin-bottom:5px;"></div>
			</div>
			<div class="modal-content">
				<div>
					<input type="checkbox" name="dropCheck" id="dropCheck" autocomplete="off" />
					<label for="dropCheck">Drop the database before restoring</label>
				</div>
				<div>
					<button class="btn btn-sm btn-primary" onclick="$('#restoreConfirmationDialog').modal('hide')">Cancel</button>
					<a id="confirmRestoreButton" class="btn btn-sm btn-danger" target="_blank">Confirm</a>
				</div>
			</div>
		</div>
	</div>
</body>

</html>