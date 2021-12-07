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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="org.springframework.security.web.WebAttributes" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>    

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link media="screen" type="text/css" href="css/role_manager.css" rel="StyleSheet">
	<title>Authentication</title>
	<script language='javascript'>
		if (self != top)
			top.location.href = location.href;
	</script>
</head>

<body onload="document.forms[0].j_username.focus();">

<%
	Exception lastException = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
%>

<center>
 <br>
 <table style="border:1px dashed #303030; background-color:#f0f0f0;">
	<tr>
		<th colspan='2' style="background-color:#21A32C; color:lightYellow; height:30px; font-size:14px;">User authentication</th>
	</tr>
	
	<tr align="center">
		<td width="5%"></td>
		<td>
		<form name='f' action='../j_spring_security_check' method='POST'>
			<table cellspacing="0">
			<tbody>
			<tr>
				<td>
					<br>
					<table style="width:200px;" cellspacing="0" border="0">
					<tbody>
						<td class="default_left"></td>
						<td class="default_center">
						<table border='0'>
							<tbody>
							<tr>
								<td width="50">Login</td>
								<td>
									<input type='text' name='j_username' value=''>
								</td>
							</tr>
							<tr>
								<td>Password</td>
								<td>
									<input type='password' name='j_password'>
								</td>
							</tr>
							<tr>
								<td style="height: 26px;"></td>
								<td colspan="2" style="height: 26px; text-align:left;">
								<input type="submit" name="connexion" value="Submit" style="margin-bottom:5px;">
								<br><%= lastException != null && lastException instanceof org.springframework.security.authentication.BadCredentialsException ? "&nbsp;&nbsp;&nbsp;<span style='color:#F2961B;'>Authentication failed!</span>" : "" %>&nbsp;
								</td>
							</tr>
							</tbody>
						</table>
						</td>
						<td class="default_right"></td>
					</tr>
					<tr>
						<td class="default_bottom_left"></td>
						<td class="default_bottom"></td>
						<td class="default_bottom_right"></td>
					</tr>
					</tbody>
				</table>
				</td>
				<td style="width: 10px;">
				</td>
			</tr>
			</tbody>
			</table>
		</form>
		</td>
	</tr>	
 </table>
 
 <br/>
 
 <a href="../">Return to <%= request.getContextPath().substring(1).toUpperCase() %></a>
 
</center>

<%
	session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, null);
%>
						  
</body>
</html>