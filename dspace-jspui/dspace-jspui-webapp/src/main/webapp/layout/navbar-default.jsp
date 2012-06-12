<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Default navigation bar
--%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="java.util.Map" %>
<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    String admin_email = ConfigurationManager.getProperty("mail.admin");
    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    String sideNews = ConfigurationManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));
    Boolean collectionu = (Boolean) request.getAttribute("collection_pageu");
    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }

    // E-mail may have to be truncated
    String navbarEmail = null;

    if (user != null)
    {
        navbarEmail = user.getEmail();
        if (navbarEmail.length() > 18)
        {
            navbarEmail = navbarEmail.substring(0, 17) + "...";
        }
    }
    
    // get the browse indices
    
	BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
    BrowseInfo binfo = (BrowseInfo) request.getAttribute("browse.info");
    String browseCurrent = "";
    if (binfo != null)
    {
        BrowseIndex bix = binfo.getBrowseIndex();
        // Only highlight the current browse, only if it is a metadata index,
        // or the selected sort option is the default for the index
        if (bix.isMetadataIndex() || bix.getSortOption() == binfo.getSortOption())
        {
            if (bix.getName() != null)
    			browseCurrent = bix.getName();
        }
    }
%>

<%
    if (user != null)
    {
%>
  <p class="loggedIn"><fmt:message key="jsp.layout.navbar-default.loggedin">
      <fmt:param><%= navbarEmail %></fmt:param>
  </fmt:message>
    (<a href="<%= request.getContextPath() %>/logout"><fmt:message key="jsp.layout.navbar-default.logout"/></a>)</p>
<%
    }
%>
  
<%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <%  if(collectionu != null && collectionu.booleanValue() == false){ %>
  <dspace:include page="/layout/admintoolscom.jsp" />
  <% } %>
   <%  if(collectionu != null && collectionu.booleanValue()){ %>
  <dspace:include page="/layout/admintoolscol.jsp" />
  <% } %>
  <dspace:include page="/layout/communityblock.jsp" />
    <tr>
    <td class="navigationBarSpacing" colspan="2">&nbsp;</td>
  </tr>

  <dspace:include page="/layout/rssblock.jsp" />
      <tr>
    <td class="navigationBarSpacing" colspan="2">&nbsp;</td>
  </tr>
  <%-- om het mailto adres aan te melden --%>
  <% String admini = ConfigurationManager.getProperty("mail.mainpage.contact"); %>
  <tr><td class="navigatiobarTitle">Contact:</td></tr>
  <tr><td class="navigatiobarRow"><a href="mailto:<% out.print(admin_email); %>"><br/><fmt:message key="jsp.layout.navbar.contact.admin" /></a><br/><br/></td></tr>
  <tr><td>
  <%= sideNews %>
  </td></tr>
</table>
