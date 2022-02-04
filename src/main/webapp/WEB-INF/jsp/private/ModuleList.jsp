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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
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
		var StringBuffer = function() {
		    this.buffer = new Array();
		};
		StringBuffer.prototype.append = function(str) {
		    this.buffer[this.buffer.length] = str;
		};
		StringBuffer.prototype.toString = function() {
		    return this.buffer.join("");
		};
		if (!String.prototype.endsWith) {
		   String.prototype.endsWith = function(suffix) {
		     return this.indexOf(suffix, this.length - suffix.length) !== -1;
		   };
		}

		var moduleData;

		const dumpValidityColors = new Map([
		    ["VALID", "#88FF88"],
		    ["OUTDATED", "#FFAA66"],
		    ["DIVERGED", "#CC88FF"],
		    ["BUSY", "#AAAAAA"],
		    ["NONE", "#FF8888"],
		]);

		const dumpValidityTips = new Map([
		    ["VALID", "Up to date"],
		    ["OUTDATED", "Out of date"],
		    ["DIVERGED", "Divergent"],
		    ["BUSY", "Writing locked"],
		    ["NONE", "No existing dump"],
		]);

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
					moduleData[moduleName]['<%= BackOfficeController.DTO_FIELDNAME_PUBLIC %>'] = setToPublic;
					moduleData[moduleName]['<%= BackOfficeController.DTO_FIELDNAME_HIDDEN %>'] = setToHidden;
					setDirty(moduleName, false);
				}
			}).error(function(xhr) { handleError(xhr); });
		}

		function resetFlags(moduleName)
		{
			let itemRow = $("#row_" + moduleName);
			itemRow.find(".flagCol1").prop("checked", moduleData[moduleName]['<%= BackOfficeController.DTO_FIELDNAME_PUBLIC %>']);
			itemRow.find(".flagCol2").prop("checked", moduleData[moduleName]['<%= BackOfficeController.DTO_FIELDNAME_HIDDEN %>']);
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
		   	let rowContents = new StringBuffer();
		   	rowContents.append("<td><a title='Click to browse database' href='../?module=" + key + "' target='_blank'>" + key + "</a></td>");
		   	let dbSize = parseFloat(moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_SIZE %>']);
		   	rowContents.append("<td>" + formatFileSize(dbSize) + "</td>");

		   	<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
	   		if (moduleData[key] != null)
	   			rowContents.append("<td>" + moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_HOST %>'] + "</td>");
			</c:if>

			rowContents.append("<td>");
			<c:forEach var="level1Type" items="${rolesByLevel1Type}">
			rowContents.append("<a id='${urlEncoder.urlEncode(moduleName)}_${level1Type.key}PermissionLink' style='text-transform:none;' href=\"javascript:openModuleContentDialog('${loggedUser.username}', '" + key + "', '${level1Type.key}');\">${level1Type.key} entities</a>");
			</c:forEach>
			rowContents.append("</td>");

			<c:if test="${fn:contains(loggedUser.authorities, adminRole) && actionRequiredToEnableDumps eq ''}">
			rowContents.append('<td style="background-color:' + dumpValidityColors.get(moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_DUMPSTATUS %>']) + '" data-toggle="tooltip" title="' + dumpValidityTips.get(moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_DUMPSTATUS %>']) + '">');
			rowContents.append("<a style=\"color:#113388;\" href=\"javascript:openModuleDumpDialog('" + key + "');\">database dumps</a></td>");
			</c:if>

			<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
	   		if (moduleData[key] != null) {
				rowContents.append("<td><input onclick='setDirty(\"" + encodeURIComponent(key) + "\", true);' class='flagCol1' type='checkbox'" + (moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_PUBLIC %>'] ? " checked" : "") + "></td>");
				rowContents.append("<td><input onclick='setDirty(\"" + encodeURIComponent(key) + "\", true);' class='flagCol2' type='checkbox'" + (moduleData[key]['<%= BackOfficeController.DTO_FIELDNAME_HIDDEN %>'] ? " checked" : "") + "></td>");
			}
	   		rowContents.append("<td><input type='button' value='Reset' class='resetButton btn btn-default btn-sm' disabled onclick='resetFlags(\"" + encodeURIComponent(key) + "\");'><input type='button' class='applyButton btn btn-default btn-sm' value='Apply' disabled onclick='saveChanges(\"" + encodeURIComponent(key) + "\");'></td>");
	   		rowContents.append("<td align='center'><a style='padding-left:10px; padding-right:10px;' href='javascript:removeItem(\"" + encodeURIComponent(key) + "\");' title='Discard module'><img src='img/delete.gif'></a></td>");
	   		</c:if>
	   		return '<tr id="row_' + encodeURIComponent(key) + '">' + rowContents.toString() + '</tr>';
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

		function formatFileSize(sizeInBytes) {
			if (isNaN(sizeInBytes))
				return "";
			if (sizeInBytes >= 1073741824)
				return parseFloat(sizeInBytes / 1073741824).toFixed(1) + " GB";
			if (sizeInBytes >= 1048576)
				return parseFloat(sizeInBytes / 1048576).toFixed(1) + " MB";
			if (sizeInBytes >= 1024)
				return parseFloat(sizeInBytes / 1024).toFixed(1) + " KB";
			return sizeInBytes.toFixed(1) + " B";
		}

		function openModuleDumpDialog(module){
		    $("#moduleDumpDialogTitle").html("Dump management for database <u>" + module + "</u>");
			$("#moduleDumpDialog").modal('show');
			$("#newDumpModule").val(module);
			$.get('<c:url value="<%= BackOfficeController.moduleDumpInfoURL %>" />', {module: module})
				.then(function (dumpData){
				    const container = $("#moduleDumpDialogContent").html("");

				    if (dumpData.locked)
						container.append("<p><strong>This module is busy, dump operations can not be performed at the moment</strong></p>");

				    if (dumpData.dumps.length == 0){
				        container.append("<p><em>No existing dump</em></p>");
				    } else {
					    const dumpTable = $('<table class="adminListTable"></table>');
					    const headerRow = $('<tr></tr>');

					    headerRow.append('<th>Validity</th><th>Dump name</th><th>Archive size</th><th>Creation date</th><th>Description</th>')
					    if (!dumpData.locked){
							headerRow.append('<th>Restore</th><th>Delete</th>');
					    }
					    dumpTable.append(headerRow);

					    dumpData.dumps.forEach(function (dumpInfo) {
					        const row = $("<tr></tr>");
					        row.append('<td style="background-color:' + dumpValidityColors.get(dumpInfo.validity) + '">' + dumpInfo.validity.toLowerCase() + '</td>');
					        row.append("<td>" + dumpInfo.name + "</td>");
					        row.append("<td>" + formatFileSize(dumpInfo.fileSizeMb) + "</td>");
					        const dumpDate = new Date(dumpInfo.creationDate);
						    const dateString = dumpDate.getFullYear() + "-" + ("0" + (dumpDate.getMonth() + 1)).slice(-2) + "-" +  ("0" + dumpDate.getDate()).slice(-2) + " " +
	    							("0" + dumpDate.getHours()).slice(-2) + ":" + ("0" + dumpDate.getMinutes()).slice(-2) + ":" + ("0" + dumpDate.getSeconds()).slice(-2);
					        row.append("<td>" + dateString + "</td>");
					        row.append("<td>" + dumpInfo.description.replaceAll(/\r?\n/mg, "<br />") + "</td>");

					        if (!dumpData.locked){
								const restoreButton = $('<button class="btn btn-sm btn-primary">Restore</button>').on("click", () => confirmDumpRestore(module, dumpInfo));
								const restoreCell = $("<td></td>").append(restoreButton);
								row.append(restoreCell);

								const deleteButton = $('<a><img src="img/delete.gif" /></a>').on("click", () => deleteDump(module, dumpInfo));
								const deleteCell = $('<td align="center"></td>').append(deleteButton);
								row.append(deleteCell);
					        }

					        dumpTable.append(row);
					    });

					    container.append(dumpTable);
				    }

				    const now = new Date();
				    const dateString = now.getFullYear() + ("0" + (now.getMonth() + 1)).slice(-2) + ("0" + now.getDate()).slice(-2) + "_" +
				    					("0" + now.getHours()).slice(-2) + ("0" + now.getMinutes()).slice(-2) + ("0" + now.getSeconds()).slice(-2);
				    $("#newDumpDialogTitle").html("New dump for module <strong>" + module + "</strong>");
				    $("#newDumpName").val("dump_" + module + "_" + dateString);
				    $("#newDumpDescription").val("");
				    $("#startDumpButton").on("click", function (){
				        $("#newDumpDialog").modal("hide");
				        $("#moduleDumpDialog").modal("hide");
				    })
					if (dumpData.locked){
				        $("#newDumpButton").hide();
				    } else {
					    $("#newDumpButton").show();
				    }
			});
		}

		function confirmDumpRestore(module, dumpInfo){
		    const restoreURL = '<c:url value="<%= BackOfficeController.restoreDumpURL %>" />?module=' + module + '&dump=' + dumpInfo.identifier;
		    $("#restoreConfirmationTitle").html("Restore dump " + dumpInfo.name + " ?");

		    $("#dropCheck").prop("checked", true).on("change", function (){
		    	let url = $("#confirmRestoreButton").attr("href");
		    	url = url.replace(/&drop=.*$/, "&drop=" + $("#dropCheck").is(":checked"));
		    	$("#confirmRestoreButton").attr("href", url);
		    });

		    $("#confirmRestoreButton").attr("href", restoreURL + "&drop=true").on("click", function (){
		        $("#restoreConfirmationDialog").modal("hide");
		        $("#moduleDumpDialog").modal("hide");
		        return true;
		    });

		    $("#restoreConfirmationDialog").modal("show");
		}

		function deleteDump(module, dumpInfo){
		    if (window.confirm("Delete dump " + dumpInfo.name + " of module " + module + " ?")){
		        const deleteURL = '<c:url value="<%= BackOfficeController.deleteDumpURL %>" />?module=' + module + '&dump=' + dumpInfo.identifier;
		        $.ajax({
		            url: deleteURL,
		            method: "DELETE",
		        }).then(() => window.location.reload());
		    }
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

	    function refreshTable() {
	        window.location.reload();
	    }
	</script>
</head>

<body style='background-color:#f0f0f0;'>
	<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<div style="max-width:600px; padding:10px; margin-bottom:10px; border:2px dashed grey; background-color:lightgrey;">
			<b>Create new empty database</b><br/>
			On host <select id="hosts"></select> named <input type="text" id="newModuleName" onkeypress="if (!isValidKeyForNewName(event)) { event.preventDefault(); event.stopPropagation(); }" onkeyup="$(this).next().prop('disabled', !isValidNewName($(this).val()));">
			<input type="button" value="Create" class="btn btn-xs btn-primary" onclick="createModule($(this).prev().val(), $('#hosts').val());" disabled>
		</div>
		<c:if test='${fn:contains(loggedUser.authorities, adminRole) && !fn:startsWith(dumpFolder, "??") && !empty actionRequiredToEnableDumps}'>
			<div class="margin-top-md text-danger">
				DB dump support feature may be enabled as follows: <u>${actionRequiredToEnableDumps}</u>
			</div>
		</c:if>
	</c:if>
	<table class="adminListTable margin-top-md" id="moduleTable">
	<thead>
	<tr>
		<th>Database name</th>
		<th>Storage size</th>
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<th style="text-transform:capitalize;"><%= BackOfficeController.DTO_FIELDNAME_HOST %></th>
		</c:if>
		<th>Entity management</th>
		<c:if test="${fn:contains(loggedUser.authorities, adminRole) && actionRequiredToEnableDumps eq ''}">
		<th>Dump management</th>
		</c:if>
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<c:if test="${fn:contains(loggedUser.authorities, adminRole)}">
		<th style="text-transform:capitalize;"><%= BackOfficeController.DTO_FIELDNAME_PUBLIC %></th>
		<th style="text-transform:capitalize;"><%= BackOfficeController.DTO_FIELDNAME_HIDDEN %></th>
		<th>Changes</th>
		</c:if>
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

	<div class="modal fade" tabindex="-1" role="dialog" id="moduleDumpDialog" aria-hidden="true">
		<div class="modal-dialog modal-lg">
			<div class="modal-content">
				<div class="modal-header">
					<div id="moduleDumpDialogTitle" style="font-weight:bold; margin-bottom:5px;"></div>
				</div>
				<div class="modal-body">
					<div id="moduleDumpDialogContent"></div>
					<br /><br />
					<div id="moduleDumpManagementOptions">
						<button id="newDumpButton" class="btn btn-sm btn-primary" onclick="$('#newDumpDialog').modal('show')">New dump</button>
					</div>
				</div>
				<div class="modal-footer">
					<input type="button" class="btn btn-sm btn-primary" value="Close" id="hlDumpDialogClose" onclick="$('#moduleDumpDialog').modal('hide');" />
				</div>
			</div>
		</div>
	</div>

	<div class="modal fade" role="dialog" id="newDumpDialog" aria-hidden="true">
		<div class="modal-dialog modal-md">
			<div class="modal-content">
				<div class="modal-header">
					<div id="newDumpDialogTitle" style="font-weight:bold; margin-bottom:5px;"></div>
				</div>
				<form id="moduleNewDumpInfo" method="GET" target="_blank" rel="opener" action='<c:url value="<%= BackOfficeController.newDumpURL %>" />'>
					<div class="modal-body">
						<input type="hidden" id="newDumpModule" name="module" /><br />
						<table>
							<tr>
								<td><label for="newDumpName">Dump name</label></td>
								<td><input type="text" id="newDumpName" style="margin-left:5px; width:450px;" name="name" pattern="^[\w\s-]*$" title="Only letters, digits, spaces, underscores and hyphens are allowed" /></td>
							</tr>
							<tr>
								<td><label for="newDumpDescription">Dump description</label></td>
								<td><textarea class='margin-top-md' style="margin-left:5px; width:450px;" rows='5' id="newDumpDescription" name="description"></textarea></td>
							</tr>
						</table>
					</div>
					<div class="modal-footer">
						<input type="button" class="btn btn-sm btn-primary" onclick="$('#newDumpDialog').modal('hide')" value="Cancel" />
						<input id="startDumpButton" type="submit" class="btn btn-sm btn-danger" value="Start dump" />
					</div>
				</form>
			</div>
		</div>
	</div>

	<div class="modal fade" role="dialog" id="restoreConfirmationDialog" aria-hidden="true">
		<div class="modal-dialog modal-md">
			<div class="modal-content">
				<div class="modal-header">
					<div id="restoreConfirmationTitle" style="font-weight:bold; margin-bottom:5px;"></div>
				</div>
				<div class="modal-body">
					<input type="checkbox" name="dropCheck" id="dropCheck" autocomplete="off" />
					<label for="dropCheck">Drop the database before restoring</label>
					<p><em>Only untick this if you know what you are doing</em></p>
				</div>
				<div class="modal-footer">
					<button class="btn btn-sm btn-primary" onclick="$('#restoreConfirmationDialog').modal('hide')">Cancel</button>
					<a id="confirmRestoreButton" class="btn btn-sm btn-danger" target="_blank" rel="opener">Confirm</a>
				</div>
			</div>
		</div>
	</div>
</body>

</html>