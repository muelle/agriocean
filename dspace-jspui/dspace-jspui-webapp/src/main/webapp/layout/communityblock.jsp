<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
  <%--
  -communityblock.jsp
 
  - Made by Dimitri Surinx, UHasselt University
  --%>
  
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>
<%@ page import="java.io.File" %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.browse.ItemCounter" %>


<%
	// Pickup the community list
	Context context = null;
	context = UIUtil.obtainContext(request);
	Community[] comList = Community.findAllTop(context);
        ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
	
	if(comList.length > 0) {
	%>
		<tr>
			<td class="navigatiobarTitle" colspan="2"><fmt:message key="jsp.home.com1"/></td>
				
		</tr>     
	<%
	
	}
	
	for(int i = 0;i < comList.length;i++)
        {
            %> <tr>
            <td class="navigatiobarRow" style="word-wrap:break-word">
            <a href="<%= request.getContextPath() %>/handle/<%= comList[i].getHandle() %>"><%= comList[i].getMetadata("name") %></a>
            <%
            if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
            %>
               [<%= ic.getCount(comList[i])%>]
            <%
            }
            %>
            </td>
	<% }

	
	%>
		</tr>