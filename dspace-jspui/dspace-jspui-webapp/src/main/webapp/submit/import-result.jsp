<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
String output = (String) request.getAttribute("output");
String error = (String) request.getAttribute("error");
%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.batchimport.result" nocache="true">
    <p><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.goto-mydspace"/></a></p>
    <h1><fmt:message key="jsp.batchimport.result"/></h1>
    <%
    if(error != null) { %>
        <div><%= error %></div>
    <%
    }
    if(output != null) { %>
        <p><%= output %></p>
    <%
    }
    else { %>
        <p>No Output</p>
    <% } %>
    
    <p><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.goto-mydspace"/></a></p>
</dspace:layout>