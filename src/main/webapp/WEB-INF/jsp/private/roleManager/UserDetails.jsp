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
	import="fr.cirad.web.controller.security.UserPermissionController,fr.cirad.security.base.IRoleDefinition,org.springframework.security.core.context.SecurityContextHolder"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<c:set var="loggedUserAuthorities"
	value="${userDao.getLoggedUserAuthorities()}" />
<c:set var='adminRole' value='<%= IRoleDefinition.ROLE_ADMIN %>' />
<c:set var='supervisorRole'
	value='<%= IRoleDefinition.ROLE_DB_SUPERVISOR %>' />
<c:set var='roleSep'
	value='<%= UserPermissionController.ROLE_STRING_SEPARATOR %>' />
<c:set var='trimmedUsername' value='${fn:trim(user.username)}' />
<jsp:useBean id="urlEncoder" scope="page"
	class="fr.cirad.web.controller.security.UserPermissionController" /><%-- dummy controller just to be able to invoke a static method --%>

<html>

<head>
<link type="text/css" rel="stylesheet"
	href="../css/bootstrap-select.min.css ">
<link type="text/css" rel="stylesheet" href="../css/bootstrap.min.css">
<link media="screen" type="text/css" href="../css/role_manager.css"
	rel="StyleSheet" />
<link media="screen" type="text/css" href="../../css/main.css"
	rel="StyleSheet" />
<script type="text/javascript" src="../js/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="../js/bootstrap.min.js"></script>
<script type="text/javascript">
		function doOnLoad()
		{
			if ($("#username").val() != null)
				$("#username").val($("#username").val().trim());

			<c:if test="${fn:contains(loggedUserAuthorities, adminRole) && !fn:contains(user.authorities, adminRole)}">
			if (doesUserExist($("#username").val()))
				$("#cloneButtonSpan").append('<input type="button" class="btn btn-sm btn-default active" value="Clone" onclick="cloneUser();"><input type="hidden" name="cloning" value="false">');
			</c:if>
		}
		
		<c:if test="${fn:contains(loggedUserAuthorities, adminRole) && user.getMethod().isEmpty()}">
		function cloneUser()
		{
			var cloneName = prompt("Enter username:");
			if (cloneName == null || cloneName == "")
				return;

			if (doesUserExist(cloneName))
			{
				alert("This username already exists!");
				return;
			}

			$("#userForm").attr("method", "get");
 			$("#username").val(cloneName);
 			$("#cloneButtonSpan input[name='cloning']").val("true");
 			$("#user").submit();
		}
		</c:if>
	    
	    function updatePermissionLink(module, entityType, permissionCount)
	    {
	    	var permissionLink = $("[id='" + module + "_" + entityType + "PermissionLink']");
	    	if (permissionCount == 0)
	    	{
	    		permissionLink.removeClass('linkToExistingPermissions');
	    		<c:if test="${fn:contains(loggedUserAuthorities, adminRole)}">
	    		permissionLink.removeAttr('title');
	    		</c:if>
	    	}
	    	else
	    	{
	    		permissionLink.addClass('linkToExistingPermissions');
	    		<c:if test="${fn:contains(loggedUserAuthorities, adminRole)}">
	    		permissionLink.attr('title', "currently " + permissionCount + " permission(s)");
	    		</c:if>
	    	}
	    	$('#hlContentDialogClose').click();
	    }

		function doesUserExist(username)
		{
			var jsonResult = $.parseJSON(
			    $.ajax(
			        {
			           url: '<c:url value="<%=UserPermissionController.userListDataURL%>" />?loginLookup=' + username + '&page=0&size=0',
			           type: 'GET',
			           async: false
			        }
			    ).responseText
			);
			for (var key in jsonResult)
				if (username == jsonResult[key][0])
					return true;

			return false;
		}

		function openPermissionDialog(username, module, entityType)
		{
	    	$('#permissionFrame').contents().find("body").html("");
	        $("#permissionDialog #permissionDialogTitle").html("Permissions for user <u>" + username + "</u> on <u id='moduleName'>" + module + "</u> <span id='entityType'>" + entityType + "</span> entities");
			$('#permissionDialog').modal('show');
	        $("#permissionFrame").attr('src', '<c:url value="<%= UserPermissionController.userPermissionURL %>" />?user=' + username + '&module=' + module + '&entityType=' + entityType);
		}

		function resizeIFrame() {
			$('#permissionFrame').css('height', (document.body.clientHeight - 180)+'px');
		}
		
	    $(document).ready(function() {
	    	resizeIFrame();
	    	
    	 	$("input#password").blur(function(e) {
    	 		var maxLength = 20;
    		    if (this.value.length > maxLength) {
    		      e.preventDefault();
    		      alert("Password length may not exceed " + maxLength +" characters");
    		      this.value = this.value.substring(0, 20);
    		    }
    		});
	    });
	    $(window).resize(function() {
	    	resizeIFrame();
	    });

	    function storePermissions()
	    {
	    	var module = $("div#permissionDialogTitle u#moduleName").text();
	    	var entityType = $("#permissionDialog #permissionDialogTitle span#entityType").text();
	    	var permissionInput = $("input[type='hidden'][name='" + entityType + "Permission_" + encodeURIComponent(module) + "']");
	    	permissionInput.val("");
	    	$('#permissionFrame').contents().find('input[type="radio"][value!=""]:checked').each(function() {
	    		permissionInput.val(permissionInput.val() + (permissionInput.val() == "" ? "" : ",") + this.value);
	    	});
	    	updatePermissionLink(encodeURIComponent(module), entityType, permissionInput.val() == "" ? 0 : permissionInput.val().split(",").length);
	    }
	</script>
</head>

<body style='background-color: #f0f0f0;' onload="doOnLoad();">
	<form:form modelAttribute="user" name="userForm">
		<div>
			<div style="display: inline-block;">
				User:
				<c:choose>
					<c:when test="${!empty trimmedUsername}">
						<b>${user.username}</b>
						<form:hidden path="username" />
						<span style='margin-left: 100px;' id="cloneButtonSpan"></span>
					</c:when>
					<c:otherwise>
						<form:input path="username" autocomplete="off" />
					</c:otherwise>
				</c:choose>
			</div>
			<div style="display: inline-block; margin-left: 50px;">
				Authentication method: <b>${user.method eq "" ? "Local" : user.method}</b>
			</div>
		</div>
		<br>

		<c:if test="${!fn:contains(user.authorities, adminRole)}">
			<table>
				<tr>
					<td valign='top'>
						<div style='margin-bottom: 10px; background-color: #f0f0f0;'>&nbsp;Permissions
							for this user&nbsp;</div> <c:if test="${fn:length(publicModules) > 0}">
							<div class="databaseListContainer">
								<div class="databaseListHeader" style="background-color: #075a80;">Public databases</div>
								<div class="databaseListModules">
									<c:forEach var="moduleName" items="${publicModules}">
										<div class="databaseModule">
											<div class="databaseColumn">
												<div class="databaseCell">
													<b> ${moduleName} </b>
												</div>
												<div class="databaseCell">
													<c:if
														test="${fn:contains(loggedUserAuthorities, adminRole)}">
														<input type="checkbox"
															onclick='$("a.${urlEncoder.urlEncode(moduleName)}_PermissionLink").toggle();'
															name='${urlEncoder.urlEncode(moduleName.concat(roleSep).concat(supervisorRole))}'
															${fn:contains(user.authorities, moduleName.concat(roleSep).concat(supervisorRole)) ? " checked" : ""}>
														<span class="databaseSpan">DB Supervisor</span>
													</c:if>
												</div>
												<c:forEach var="level1Type" items="${rolesByLevel1Type}">
													<c:set var="roles" value="" />
													<c:forEach var="auth" items="${user.authorities}">
														<c:set var="encodedAuth"
															value='${urlEncoder.urlEncode(auth)}' />
														<c:forEach var="level1TypeRole"
															items="${level1Type.value}">
															<c:set var="rolePrefix"
																value='${urlEncoder.urlEncode(moduleName.concat(roleSep).concat(level1Type.key).concat(roleSep).concat(level1TypeRole).concat(roleSep))}' />
															<c:if test="${fn:startsWith(encodedAuth, rolePrefix)}">
																<c:set var="roles"
																	value='${roles}${roles == "" ? "" : ","}${encodedAuth}' />
															</c:if>
														</c:forEach>
													</c:forEach>
														<c:if test="${rolesByLevel1Type[level1Type.key] ne null}">
															<div class="separator"></div>
															<div class="databaseCell" style='min-height:44px;'>
																<a
																id='${urlEncoder.urlEncode(moduleName)}_${level1Type.key}PermissionLink'
																class='${urlEncoder.urlEncode(moduleName)}_PermissionLink'
																style='text-transform:none;${fn:contains(user.authorities, moduleName.concat(roleSep).concat(supervisorRole)) ? " display:none;" : ""}'
																href="javascript:openPermissionDialog('${user.username}', '${moduleName}', '${level1Type.key}');">${level1Type.key}<br />permissions
															</a>
															<c:if test="${roles ne ''}">
																<script type="text/javascript">updatePermissionLink("${urlEncoder.urlEncode(moduleName)}", "${level1Type.key}", "${roles}".split(",").length);</script>
															</c:if>
															<input type='hidden'
																name="${urlEncoder.urlEncode(level1Type.key.concat('Permission_').concat(moduleName))}"
																value="${roles}">
															</div>
														</c:if>
												</c:forEach>
											</div>
										</div>
									</c:forEach>
								</div>
							</div>
						</c:if>
						<c:if test="${fn:length(privateModules) > 0}">
							<br/>
							<div class="databaseListContainer">
								<div class="databaseListHeader" style="background-color: #075a80;">Private databases</div>
								<div class="databaseListModules">
									<c:forEach var="moduleName" items="${privateModules}">
										<div class="databaseModule">
											<div class="databaseColumn">
												<div class="databaseCell">
													<b> ${moduleName} </b>
												</div>
												<div class="databaseCell">
													<c:if
														test="${fn:contains(loggedUserAuthorities, adminRole)}">
														<input type="checkbox"
															onclick='$("a.${urlEncoder.urlEncode(moduleName)}_PermissionLink").toggle();'
															name='${urlEncoder.urlEncode(moduleName.concat(roleSep).concat(supervisorRole))}'
															${fn:contains(user.authorities, moduleName.concat(roleSep).concat(supervisorRole)) ? " checked" : ""}>
														<span class="databaseSpan">DB Supervisor</span>
													</c:if>
												</div>
												<c:forEach var="level1Type" items="${rolesByLevel1Type}">
													<c:set var="roles" value="" />
													<c:forEach var="auth" items="${user.authorities}">
														<c:set var="encodedAuth"
															value='${urlEncoder.urlEncode(auth)}' />
														<c:forEach var="level1TypeRole"
															items="${level1Type.value}">
															<c:set var="rolePrefix"
																value='${urlEncoder.urlEncode(moduleName.concat(roleSep).concat(level1Type.key).concat(roleSep).concat(level1TypeRole).concat(roleSep))}' />
															<c:if test="${fn:startsWith(encodedAuth, rolePrefix)}">
																<c:set var="roles"
																	value='${roles}${roles == "" ? "" : ","}${encodedAuth}' />
															</c:if>
														</c:forEach>
													</c:forEach>
														<c:if test="${rolesByLevel1Type[level1Type.key] ne null}">
															<div class="separator"></div>
															<div class="databaseCell" style='min-height:44px;'>
																<a
																id='${urlEncoder.urlEncode(moduleName)}_${level1Type.key}PermissionLink'
																class='${urlEncoder.urlEncode(moduleName)}_PermissionLink'
																style='text-transform:none;${fn:contains(user.authorities, moduleName.concat(roleSep).concat(supervisorRole)) ? " display:none;" : ""}'
																href="javascript:openPermissionDialog('${user.username}', '${moduleName}', '${level1Type.key}');">${level1Type.key}<br />permissions
															</a>
															<c:if test="${roles ne ''}">
																<script type="text/javascript">updatePermissionLink("${urlEncoder.urlEncode(moduleName)}", "${level1Type.key}", "${roles}".split(",").length);</script>
															</c:if>
															<input type='hidden'
																name="${urlEncoder.urlEncode(level1Type.key.concat('Permission_').concat(moduleName))}"
																value="${roles}">
															</div>
														</c:if>
												</c:forEach>
											</div>
										</div>
									</c:forEach>
								</div>
							</div>
						</c:if>
					</td>
				</tr>
			</table>
		</c:if>

		<div style="margin-top: 10px;">
			<p>
				<c:if
					test="${fn:contains(loggedUserAuthorities, adminRole) && user.getMethod().isEmpty()}">
			You can modify this user's password by typing a new password here: <input
						id="password" type='password' name="password"
						style='width: 100px;' autocomplete="new-password" /> (max-length: 20)
		</c:if>
			</p>
			<table>
				<tr>
					<td><input type='submit' value='Save user details'
						class='btn btn-sm btn-primary active' style='display: inline;'></td>
					<td><c:if test="${fn:length(errors) > 0}">
							<span class="formErrors" style='margin-left: 10px;'> <c:forEach
									var="error" items="${errors}">
							${error}<br>
								</c:forEach>
							</span>
						</c:if></td>
				</tr>
			</table>
		</div>

	</form:form>

	<div class="modal fade" tabindex="-1" role="dialog"
		id="permissionDialog" aria-hidden="true">
		<div class="modal-dialog modal-lg">
			<div class="modal-content">
				<div class="modal-header" id="projectInfoContainer">
					<div id="permissionDialogTitle"
						style='font-weight: bold; margin-bottom: 5px;'></div>
					<iframe style='margin-bottom: 10px; width: 100%;'
						id="permissionFrame" name="permissionFrame"></iframe>
					<br>
					<form style="margin: 0;">
						<input type='button' class='btn btn-sm btn-primary' value='Apply'
							id="applyButton" onclick="storePermissions();">
						&nbsp;&nbsp;&nbsp; <input type='button'
							class='btn btn-sm btn-primary' value='Cancel'
							id="hlContentDialogClose"
							onclick="$('#permissionDialog').modal('hide');">
					</form>
				</div>
			</div>
		</div>
	</div>
</body>

</html>