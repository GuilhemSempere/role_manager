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
	import="org.springframework.security.web.WebAttributes"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link media="screen" type="text/css" href="css/role_manager.css"
	rel="StyleSheet">
<title>Authentication</title>
<script language='javascript'>
	if (self != top)
		top.location.href = location.href;
</script>
</head>

<body onload="document.forms[0].username.focus();"
	style="background: #0A400F; position: relative; height:100vh; margin:0;">

	<%
	Exception lastException = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
	%>

	<div
		style="width: max-content; padding: 46px; background-color: white; border-radius: 24px; position: absolute; top: 40%; left: 50%; transform: translate(-50%, -50%);">
		<form name='f' action='../j_spring_security_check' method='POST'
			style="display: flex; flex-direction: column;">
			<p
				style="font-size: 48px; margin: 0; padding: 0 46px 46px 46px; font-family: sans-serif; border-bottom: 1px solid gray; margin: 0 -46px">User
				authentication</p>
			<div
				style="padding-top: 46px; display: flex; flex-direction: column; gap: 32px; align-items: center; justify-content: center; position: relative">
				<input type='text' name='username' value=''
					style="border: none; outline: none; padding: 12px 0; border-bottom: 2px solid gray; font-size: 16px; width: 100%"
					placeholder="Username">
					<input type='password'
					name='password'
					style="border: none; outline: none; padding: 12px 0; border-bottom: 2px solid gray; font-size: 16px; width: 100%"
					placeholder="Password">
				<div style="height: 10px;">
					<p style="position: absolute; left: 0">
						<%=lastException != null
		&& lastException instanceof org.springframework.security.authentication.BadCredentialsException
				? "<span style='color:#F2961B;'>Authentication failed!</span>"
				: ""%>
					</p>
				</div>
				<input type="submit" name="connexion" value="Login"
					style="cursor: pointer; padding: 16px 32px; width: 100%; border-radius: 25px; border: 2px solid gray; font-size: 16px; font-weight: bold;"
					onmouseover="this.style.backgroundColor = '#DBDFDF';"
					onmouseout="this.style.backgroundColor = '';"> <a
					style="color: black; font-family: sans-serif; text-decoration:none; font-size: 14px" href="../"
					onmouseover="this.style.color = '#21a32c';"
					onmouseout="this.style.color = '';">Return to <%=request.getContextPath().substring(1).toUpperCase()%></a>
			</div>
		</form>
	</div>

	<%
	session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, null);
	%>

</body>
</html>