<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - admintoolscol.jsp
  - Made by Dimitri Surinx, UHasselt University
  - Collection admin block
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
<%@ page import="org.dspace.browse.BrowseAC"%>
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
    Collection collection = (Collection) request.getAttribute("collection");
    Community  community  = (Community) request.getAttribute("community");
    Group      submitters = (Group) request.getAttribute("submitters");
	   boolean loggedIn =
	        ((Boolean) request.getAttribute("logged.in")).booleanValue();
	    boolean subscribed =
	        ((Boolean) request.getAttribute("subscribed")).booleanValue();
	    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
	    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

	    Boolean editor_b      = (Boolean)request.getAttribute("editor_button");
	    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());

	    Boolean submit_b      = (Boolean)request.getAttribute("can_submit_button");
	    boolean submit_button = (submit_b == null ? false : submit_b.booleanValue());
	%>



<% if(community != null && collection != null && (admin_button || editor_button) ) {  // edit button(s)
                     %>

<tr>
	<td class="navigatiobarTitle" nowrap="nowrap" colspan="2"><fmt:message
		key="jsp.admintools" /></td>

</tr>
<% if( editor_button ) { %>
<tr>
	<td class="navigatiobarTitle" nowrap="nowrap" colspan="2">
	<form method="post"
		action="<%=request.getContextPath()%>/tools/edit-communities">
	<input type="hidden" name="collection_id"
		value="<%= collection.getID() %>" /> <input type="hidden"
		name="community_id" value="<%= community.getID() %>" /> <input
		type="hidden" name="action"
		value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>" /> <input
		type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
	</form>
	</td>
</tr>
<% } %>
<% if( admin_button ) { %>
            <tr>
              <td class="navigatiobarTitle" nowrap="nowrap" colspan="2">
                 <form method="post" action="<%=request.getContextPath()%>/tools/itemmap">
                  <input type="hidden" name="cid" value="<%= collection.getID() %>" />
				  <input type="submit" value="<fmt:message key="jsp.collection-home.item.button"/>" />                  
                </form>
              </td>
            </tr>
<% if(submitters != null) { %>
            <tr>
	         <td class="navigatiobarTitle" nowrap="nowrap" colspan="2">
		      <form method="get" action="<%=request.getContextPath()%>/tools/group-edit">
		        <input type="hidden" name="group_id" value="<%=submitters.getID()%>" />
		        <input type="submit" name="submit_edit" value="<fmt:message key="jsp.collection-home.editsub.button"/>" />
		      </form>
	         </td>
           </tr>
<% } %>
<% if( editor_button || admin_button) { %>
            <tr>
              <td class="navigatiobarTitle" nowrap="nowrap" colspan="2">
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                  <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                  <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                  <input type="submit" value="<fmt:message key="jsp.mydspace.request.export.collection"/>" />
                </form>
              </td>
            </tr>
            <tr>
             <td class="navigatiobarTitle" nowrap="nowrap" colspan="2">
               <form method="post" action="<%=request.getContextPath()%>/mydspace">
                 <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                 <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                 <input type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecollection"/>" />
               </form>
             </td>
           </tr>
           <tr>
             <td class="navigatiobarTitle" nowrap="nowrap" colspan="2">
               <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                 <input type="hidden" name="handle" value="<%= collection.getHandle() %>" />
                 <input type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
               </form>
             </td>
           </tr>
<% } %>
            <tr>
              <td class="navigatiobarRow" nowrap="nowrap" colspan="2">
                 <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
              </td>
            </tr>
<% } %>
<tr>
	<td class="navigationBarSpacing" colspan="2">&nbsp;</td>
</tr>
<%}%>