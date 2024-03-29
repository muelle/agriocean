<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Collection home JSP
  -
  - Attributes required:
  -    collection  - Collection to render home page for
  -    community   - Community this collection is in
  -    last.submitted.titles - String[], titles of recent submissions
  -    last.submitted.urls   - String[], corresponding URLs
  -    logged.in  - Boolean, true if a user is logged in
  -    subscribed - Boolean, true if user is subscribed to this collection
  -    admin_button - Boolean, show admin 'edit' button
  -    editor_button - Boolean, show collection editor (edit submitters, item mapping) buttons
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.ItemCounter"%>
<%@ page import="org.dspace.content.*"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.eperson.Group"     %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.content.ItemIterator"%>
<%@ page import="org.dspace.content.Item"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.GregorianCalendar"%>
<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<%@ page import="proj.oceandocs.components.RecentSubm" %>
<%@ page import="proj.oceandocs.components.CollectionItems" %>

<%
    Context context = null;
    context = UIUtil.obtainContext(request);
    // Retrieve attributes
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");
    Group submitters = (Group) request.getAttribute("submitters");

    RecentSubmissions rs = (RecentSubmissions) request.getAttribute("recently.submitted");

    boolean loggedIn =
            ((Boolean) request.getAttribute("logged.in")).booleanValue();
    boolean subscribed =
            ((Boolean) request.getAttribute("subscribed")).booleanValue();
    Boolean admin_b = (Boolean) request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

    Boolean editor_b = (Boolean) request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());

    Boolean submit_b = (Boolean) request.getAttribute("can_submit_button");
    boolean submit_button = (submit_b == null ? false : submit_b.booleanValue());

    // get the browse indices
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();

    // Put the metadata values into guaranteed non-null variables
    String name = collection.getMetadata("name");
    String intro = collection.getMetadata("introductory_text");
    if (intro == null) {
        intro = "";
    }
    String copyright = collection.getMetadata("copyright_text");
    if (copyright == null) {
        copyright = "";
    }
    String sidebar = collection.getMetadata("side_bar_text");
    if (sidebar == null) {
        sidebar = "";
    }

    String communityName = community.getMetadata("name");
    String communityLink = "/handle/" + community.getHandle();

    Bitstream logo = collection.getLogo();

    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled) {
        feedData = "coll:" + ConfigurationManager.getProperty("webui.feed.formats");
    }

    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    boolean customXMLimportEnabled = ConfigurationManager.getBooleanProperty("batchimport.customxml.enable");
    
    String viewpage = (String)request.getAttribute("javax.servlet.forward.query_string") != null ? request.getAttribute("javax.servlet.forward.query_string").toString().split("=")[1] : "1";
    int cur_page = 1;
    try{
        cur_page = Integer.parseInt(viewpage);
    }catch(Exception e)
    {
        cur_page = 1;
    }
    if(cur_page < 1)
        cur_page = 1;
    
    String nextaction = request.getContextPath() + "/";
	nextaction = nextaction + "handle/" + collection.getHandle() + "?page="  + (cur_page + 1);
	
    
    String prevaction = request.getContextPath() + "/";
	prevaction = prevaction + "handle/" + collection.getHandle() + "?page=" + (cur_page - 1);
        
   boolean showItems = ConfigurationManager.getBooleanProperty("agriocean.showCollectionItems");
%>


<dspace:layout locbar="commLink" title="<%= name%>" feedData="<%= feedData%>">
    <table>
        <tr>
            <td valign="top">
                <table border="0" cellpadding="5" width="100%">
                    <tr>
                        <td width="100%">
                            <h1><%= name%>
                                <%
                                    if (ConfigurationManager.getBooleanProperty("webui.strengths.show")) {
                                %>
                                : [<%= ic.getCount(collection)%>]
                                <%
                                    }
                                %>
                            </h1>
                            <h3><fmt:message key="jsp.collection-home.heading1"/></h3>
                        </td>
                        <td valign="top">
                            <%  if (logo != null) {%>
                            <img alt="Logo" src="<%= request.getContextPath()%>/retrieve/<%= logo.getID()%>" />
                            <% }%></td>
                    </tr>
                </table>

                <%-- Search/Browse --%>
                <table class="miscTable" align="center" summary="This table allows you to search through all collections in the repository">
                    <tr>
                        <td class="evenRowEvenCol" colspan="2">
                            <form method="get" action="">
                                <table>
                                    <tr>
                                        <td class="standard" align="center">
                                            <label for="tlocation"><small><strong><fmt:message key="jsp.general.location"/></strong></small></label>&nbsp;
                                            <select name="location" id="tlocation">
                                                <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                                                <option selected="selected" value="<%= community.getHandle()%>"><%= communityName%></option>
                                                <option selected="selected" value="<%= collection.getHandle()%>"><%= name%></option>
                                            </select>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="standard" align="center">
                                            <label for="tquery"><small><fmt:message key="jsp.general.searchfor"/>&nbsp;</small></label><input type="text" name="query" id="tquery"/>&nbsp;
                                            <input type="submit" name="submit_search" value="<fmt:message key="jsp.general.go"/>" />
                                        </td>
                                    </tr>
                                </table>
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" class="standard" valign="middle">
                            <div style="float: left"><small><fmt:message key="jsp.general.orbrowse"/>&nbsp;</small></div>
                            <%-- Insert the dynamic list of browse options --%>
                            <%
                                for (int i = 0; i < bis.length; i++) {
                                    String key = "browse.menu." + bis[i].getName();
                            %>
                            <div class="browse_buttons">
                                <form method="get" action="<%= request.getContextPath()%>/handle/<%= collection.getHandle()%>/browse">
                                    <input type="hidden" name="type" value="<%= bis[i].getName()%>"/>
                                    <%-- <input type="hidden" name="collection" value="<%= collection.getHandle() %>" /> --%>
                                    <input type="submit" name="submit_browse" value="<fmt:message key="<%= key%>"/>"/>
                                </form>
                            </div>
                            <%
                                }
                            %>
                        </td>
                    </tr>
                </table>

                <table width="100%" align="center" cellspacing="10">
                    <tr>
                        <td>
                            <%-- HACK: <center> used for Netscape 4.x, which doesn't accept align="center"
                              for a paragraph with a button in it --%>
                            <%  if (submit_button) {%>
                    <center>
                        <form action="<%= request.getContextPath()%>/submit" method="post">
                            <input type="hidden" name="collection" value="<%= collection.getID()%>" />
                            <input type="submit" name="submit" value="<fmt:message key="jsp.collection-home.submit.button"/>" />
                        </form>
                    </center>
                    <%  }%>
            </td>
            <td class="oddRowEvenCol">
                <form method="get" action="">
                    <table>
                        <tr>
                            <td class="standard">
                                <%  if (loggedIn && subscribed) {%>
                                <small><fmt:message key="jsp.collection-home.subscribed"/> <a href="<%= request.getContextPath()%>/subscribe"><fmt:message key="jsp.collection-home.info"/></a></small>
                            </td>
                            <td class="standard">
                                <input type="submit" name="submit_unsubscribe" value="<fmt:message key="jsp.collection-home.unsub"/>" />
                                <%  } else {%>
                                <small>
                                    <fmt:message key="jsp.collection-home.subscribe.msg"/>
                                </small>
                            </td>
                            <td class="standard">
                                <input type="submit" name="submit_subscribe" value="<fmt:message key="jsp.collection-home.subscribe"/>" />
                                <%  }%>
                            </td>
                        </tr>
                    </table>
                </form>
            </td>
        </tr>
        <tr>
            <td colspan="2">
                <% if (submit_button) {%>
                <div class="metadataFieldrow">
                    <form action="<%= request.getContextPath()%>/submit/batchimport" method="post" enctype="multipart/form-data">
                        <input type="hidden" name="collection_id" value="<%= collection.getID()%>" />

                        <div>
                            <label><fmt:message key="jsp.batchimport.format"/></label>
                            <input type="radio" name="metadataFormat" value="AGRIS" checked="true"/>AGRIS AP
                            <input type="radio" name="metadataFormat" value="MODS" />MODS
                            <input type="radio" name="metadataFormat" value="INMAGIC" />Inmagic
                            <input type="radio" name="metadataFormat" value="ENDNOTE" />EndNote / Other
                        </div>
                        <% if (customXMLimportEnabled) {%>
                        <div>
                            <input type="radio" name="metadataFormat" value="XML" />XML ...
                            <label><fmt:message key="jsp.batchimport.xslt"/></label>
                            <input type="file" size="30" name="xslt"/>
                        </div>
                        <%}%>
                        <!-- <div>
                             <label><fmt:message key="jsp.batchimport.mode"/></label>
                             <input type="radio" name="importmode" value="WORKSPACE" checked="true"/><fmt:message key="jsp.batchimport.mode.workspace"/>
                             <input type="radio" name="importmode" value="DIRECT" /><fmt:message key="jsp.batchimport.mode.direct"/>
                         </div> -->
                        <input type="hidden" name="importmode" value="WORKSPACE"/>
                        <div style="float: left;">
                            <label><fmt:message key="jsp.batchimport.file"/></label>
                            <input type="file" size="50" name="file"/>
                            <input type="submit" name="submit_proceed" value="<fmt:message key="jsp.batchimport.button"/>" />
                        </div>
                    </form>
                </div>
                <% }%>
            </td>
        </tr>
    </table>
    <div align="center">
        <form method="get" action="<%= request.getContextPath()%>/displaystats">
            <input type="hidden" name="handle" value="<%= collection.getHandle()%>"/>
            <input type="submit" name="submit_simple" value="<fmt:message key="jsp.collection-home.display-statistics"/>" />
        </form>
    </div>

    <%= intro%>

    <p class="copyrightText"><%= copyright%></p>

    <%-- list Items in current collection if turned on in dspace.cfg--%>
    <% if (showItems) {%>
    <table cellspacing="2" width="100%">
   <tr>
       <td colspan="2">
           <%--  do the top previous and next page links --%>
	<div align="center">
	
        <a href="<%= prevaction %>"><fmt:message key="browse.single.prev"/></a>&nbsp;

	&nbsp;<a href="<%= nextaction %>"><fmt:message key="browse.single.next"/></a>

	</div>
       </td>
   </tr>
   <tr>
    <td valign="top" class="latestLayoutTitle"><fmt:message key="itemlist.dc.title" /> - <fmt:message key="metadata.dc.identifier.citation" /> - <fmt:message key="itemlist.dc.contributor.author" /></td>
     <td align="right" valign="top" class="latestLayoutTitle"><fmt:message key="itemlist.dc.type" /></td>
   </tr>

   <%
      out.print(CollectionItems.GenerateHTML(context, collection, cur_page, request.getContextPath()));
   %>
      
   <tr>
       <td colspan="2">
           <%--  do the top previous and next page links --%>
	<div align="center">
	
        <a href="<%= prevaction %>"><fmt:message key="browse.single.prev"/></a>&nbsp;

	&nbsp;<a href="<%= nextaction %>"><fmt:message key="browse.single.next"/></a>

	</div>
       </td>
   </tr>
  </table>
        <%}%>
    
    <%-- Added by Dimitri Surinx --%>
    <%--<dspace:include page="overviewcol.jsp" /> --%>
</td>
<td width="20%" valign="top">
    <% if (admin_button || editor_button) {%>
    <table class="miscTable" align="center">
        <tr>
            <td class="evenRowEvenCol" colspan="2">
                <table>
                    <tr>
                        <th id="t1" class="standard">
                            <strong><fmt:message key="jsp.admintools"/></strong>
                        </th>
                    </tr>

                    <% if (editor_button) {%>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                                <input type="hidden" name="collection_id" value="<%= collection.getID()%>" />
                                <input type="hidden" name="community_id" value="<%= community.getID()%>" />
                                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION%>" />
                                <input type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                            </form>
                        </td>
                    </tr>
                    <% }%>

                    <% if (admin_button) {%>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <form method="post" action="<%=request.getContextPath()%>/tools/itemmap">
                                <input type="hidden" name="cid" value="<%= collection.getID()%>" />
                                <input type="submit" value="<fmt:message key="jsp.collection-home.item.button"/>" />
                            </form>
                        </td>
                    </tr>
                    <% if (submitters != null) {%>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <form method="get" action="<%=request.getContextPath()%>/tools/group-edit">
                                <input type="hidden" name="group_id" value="<%=submitters.getID()%>" />
                                <input type="submit" name="submit_edit" value="<fmt:message key="jsp.collection-home.editsub.button"/>" />
                            </form>
                        </td>
                    </tr>
                    <% }%>
                    <% if (editor_button || admin_button) {%>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <form method="post" action="<%=request.getContextPath()%>/mydspace">
                                <input type="hidden" name="collection_id" value="<%= collection.getID()%>" />
                                <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE%>" />
                                <input type="submit" value="<fmt:message key="jsp.mydspace.request.export.collection"/>" />
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <form method="post" action="<%=request.getContextPath()%>/mydspace">
                                <input type="hidden" name="collection_id" value="<%= collection.getID()%>" />
                                <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE%>" />
                                <input type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecollection"/>" />
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                                <input type="hidden" name="handle" value="<%= collection.getHandle()%>" />
                                <input type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
                            </form>
                        </td>
                    </tr>
                    <% }%>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, 
                 
                            \"help.collection-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
                            </td>
                        </tr>
                    <% } %>

                </table>
            </td>
        </tr>
    </table>
    <%  } %>


    <h3><fmt:message key="jsp.collection-home.recentsub"/></h3>
    <%
            if (rs != null) {
            Item[] items = rs.getRecentSubmissions();
            for (int i = 0; i < items.length; i++) {
                DCValue[] dcv = items[i].getMetadata("dc", "title", null, Item.ANY);
                String displayTitle = "Untitled";
                if (dcv != null) {
                    if (dcv.length > 0) {
                        displayTitle = dcv[0].value;
                    }
                }
    %><p class="recentItem"><a href="<%= request.getContextPath()%>/handle/<%= items[i].getHandle()%>"><%= displayTitle%></a></p><%
            }
        }
    %>
    <p>&nbsp;</p>
    <%
        if (feedEnabled) {
    %>
<center>
    <h4><fmt:message key="jsp.collection-home.feeds"/></h4>
    <%
        String[] fmts = feedData.substring(5).split(",");
        String icon = null;
        int width = 0;
        for (int j = 0; j < fmts.length; j++) {
            if ("rss_1.0".equals(fmts[j])) {
                icon = "rss1.gif";
            } else if ("rss_2.0".equals(fmts[j])) {
                icon = "rss2.gif";
            } else {
                icon = "rss.gif";
            }
    %> <a href="<%= request.getContextPath()%>/feed/<%= fmts[j]%>/<%= collection.getHandle()%>">
        <img src="<%= request.getContextPath()%>/image/<%= icon%>" alt="RSS Feed" vspace="2" border="0" /></a>
        <%
            }
        %>
</center>
<%
    }
%>
<%= sidebar%>
</td>
</tr>
</table>

</dspace:layout>

