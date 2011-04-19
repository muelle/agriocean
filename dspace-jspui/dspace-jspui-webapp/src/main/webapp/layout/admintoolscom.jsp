<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - admintoolscom.jsp
  - Made by Dimitri Surinx, UHasselt University
  - Community admin block
  --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ page contentType="text/html;charset=UTF-8"%>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="org.dspace.app.webui.util.JSPManager"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="javax.servlet.jsp.jstl.core.*"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.*"%>
<%@ page import="org.dspace.eperson.EPerson"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>

<%@ page import="java.util.Vector"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.io.*"%>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions"%>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>
<%@ page import="org.dspace.browse.BrowseIndex"%>
<%@ page import="org.dspace.browse.ItemCounter"%>
<%@ page import="org.dspace.content.*"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.eperson.Group"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="proj.oceandocs.components.BrowseAC" %>
<%@ page import="java.sql.*"%>
<%@ page import="org.dspace.core.I18nUtil"%>
<%@ page import="org.dspace.content.Community"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.browse.ItemCounter"%>

<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.content.ItemIterator"%>
<%@ page import="org.dspace.content.*"%>
<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>

<%
	// Pickup the community list
	Context context = null;
	context = UIUtil.obtainContext(request);
	Community[] comList = Community.findAllTop(context);
    Community community = null;
    Collection[] collections = null;
    Community[] subcommunities = null;
    
    
    
    Boolean editor_b = false;
    boolean editor_button = false;
    Boolean add_b = false;
    boolean add_button = false;
    Boolean remove_b = false;
    boolean remove_button = false;
    
    
   
    community = (Community) request.getAttribute( "community" );
    collections =
        (Collection[]) request.getAttribute("collections");
    subcommunities =
        (Community[]) request.getAttribute("subcommunities");
    
    
    editor_b = (Boolean)request.getAttribute("editor_button");
    editor_button = (editor_b == null ? false : editor_b.booleanValue());
    add_b = (Boolean)request.getAttribute("add_button");
    add_button = (add_b == null ? false : add_b.booleanValue());
    remove_b = (Boolean)request.getAttribute("remove_button");
    remove_button = (remove_b == null ? false : remove_b.booleanValue());
	%>



<% if(community != null &&(editor_button || add_button) )  // edit button(s)
                    { %>

<tr>
	<td class="navigatiobarTitle" nowrap="nowrap" colspan="2"><fmt:message
		key="jsp.admintools" /></td>
</tr>
<tr>
	<td class="navigatiobarRow">
	<% if(editor_button) { %>
	<form method="post"
		action="<%=request.getContextPath()%>/tools/edit-communities">
	<input type="hidden" name="community_id"
		value="<%= community.getID() %>" /> <input type="hidden"
		name="action" value="<%=EditCommunitiesServlet.START_EDIT_COMMUNITY%>" />
	<%--<input type="submit" value="Edit..." />--%> <input type="submit"
		value="<fmt:message key="jsp.general.edit.button"/>" /></form>
	<% } %> <% if(add_button) { %>

	<form method="post"
		action="<%=request.getContextPath()%>/tools/collection-wizard">
	<input type="hidden" name="community_id"
		value="<%= community.getID() %>" /> <input type="submit"
		value="<fmt:message key="jsp.community-home.create1.button"/>" /></form>

	<form method="post"
		action="<%=request.getContextPath()%>/tools/edit-communities">
	<input type="hidden" name="action"
		value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY%>" /> <input
		type="hidden" name="parent_community_id"
		value="<%= community.getID() %>" /> <%--<input type="submit" name="submit" value="Create Sub-community" />--%>
	<input type="submit" name="submit"
		value="<fmt:message key="jsp.community-home.create2.button"/>" /></form>
	<% } %>
	</td>
</tr>
<% if( editor_button ) { %>
<tr>
	<td class="navigatiobarRow">
	<form method="post" action="<%=request.getContextPath()%>/mydspace"><input
		type="hidden" name="community_id" value="<%= community.getID() %>" />
	<input type="hidden" name="step"
		value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" /> <input
		type="submit"
		value="<fmt:message key="jsp.mydspace.request.export.community"/>" />
	</form>
	</td>
</tr>
<tr>
	<td class="navigatiobarRow">
	<form method="post" action="<%=request.getContextPath()%>/mydspace"><input
		type="hidden" name="community_id" value="<%= community.getID() %>" />
	<input type="hidden" name="step"
		value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" /> <input
		type="submit"
		value="<fmt:message key="jsp.mydspace.request.export.migratecommunity"/>" />
	</form>
	</td>
</tr>
<tr>
	<td class="navigatiobarRow">
	<form method="post"
		action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
	<input type="hidden" name="handle" value="<%= community.getHandle() %>" />
	<input type="submit"
		value="<fmt:message key="jsp.general.metadataexport.button"/>" /></form>
	</td>
</tr>
<% } %>
            <tr>
              <td class="navigatiobarRow" nowrap="nowrap" colspan="2">
                 <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
              </td>
            </tr>
<tr>
	<td class="navigationBarSpacing" colspan="2">&nbsp;</td>
</tr>
<% }%>