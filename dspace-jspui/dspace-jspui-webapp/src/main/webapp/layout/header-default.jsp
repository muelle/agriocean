<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%--
  - HTML header for main home page
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.List"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.app.util.Util" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>

<%
    String title = (String) request.getAttribute("dspace.layout.title");
    String navbar = (String) request.getAttribute("dspace.layout.navbar");
    boolean locbar = ((Boolean) request.getAttribute("dspace.layout.locbar")).booleanValue();

    String siteName = ConfigurationManager.getProperty("dspace.name");
    String feedRef = (String)request.getAttribute("dspace.layout.feedref");
    boolean osLink = ConfigurationManager.getBooleanProperty("websvc.opensearch.autolink");
    String osCtx = ConfigurationManager.getProperty("websvc.opensearch.svccontext");
    String osName = ConfigurationManager.getProperty("websvc.opensearch.shortname");
    List parts = (List)request.getAttribute("dspace.layout.linkparts");
    String extraHeadData = (String)request.getAttribute("dspace.layout.head");
    String dsVersion = Util.getSourceVersion();
    String generator = dsVersion == null ? "DSpace" : "DSpace "+dsVersion;

    Boolean admin = (Boolean) request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf('?');
    if (c > -1)
        currentPage = currentPage.substring(0, c);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <title><%= siteName %>: <%= title %></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="Generator" content="<%= generator %>" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/print.css" media="print" type="text/css" />
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
<%
    if (!"NONE".equals(feedRef))
    {
        for (int i = 0; i < parts.size(); i+= 3)
        {
%>
        <link rel="alternate" type="application/<%= (String)parts.get(i) %>" title="<%= (String)parts.get(i+1) %>" href="<%= request.getContextPath() %>/feed/<%= (String)parts.get(i+2) %>/<%= feedRef %>"/>
<%
        }
    }
    
    if (osLink)
    {
%>
        <link rel="search" type="application/opensearchdescription+xml" href="<%= request.getContextPath() %>/<%= osCtx %>description.xml" title="<%= osName %>"/>
<%
    }

    if (extraHeadData != null)
        { %>
<%= extraHeadData %>
<%
        }
%>
        
    <script type="text/javascript" src="<%= request.getContextPath() %>/utils.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/prototype.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/effects.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/builder.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/controls.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/dropdown.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/authority-controll.js"> </script>
    </head>

    <%-- HACK: leftmargin, topmargin: for non-CSS compliant Microsoft IE browser --%>
    <%-- HACK: marginwidth, marginheight: for non-CSS compliant Netscape browser --%>
    <body>
        <div id="ds-main">
        <%-- DSpace top-of-page banner --%>
        <%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
        <table class="pageBanner" width="100%" border="0" cellpadding="0" cellspacing="0">

            <%-- DSpace logo --%>
            <tr>
                <td>
                    <a href="<%= request.getContextPath() %>/"><img src="<%= request.getContextPath() %>/image/banner-large.jpg" alt="<fmt:message key="jsp.layout.header-default.alt"/>" width="977" border="0"/>
                    </a></td>
            </tr>
            <tr class="stripe"> <%-- Menu bar --%>
                <td colspan="3">
                    <ul id="sddm">
                            <li><a href="<%=request.getContextPath()%>/"><fmt:message
                                        key="jsp.layout.navbar-default.home" /></a></li>
                            <li><a a href="<%= request.getContextPath()%>/mydspace" onmouseover="mopen('m5')"
                                   onmouseout="mclosetime()"><fmt:message
                                        key="jsp.layout.navbar-default.users" /></a>
                                <div id="m5" onmouseover="mcancelclosetime()"
                                     onmouseout="mclosetime()">
                                    <a href="<%= request.getContextPath()%>/subscribe"><fmt:message key="jsp.layout.navbar-default.receive"/></a>
                                    <a href="<%= request.getContextPath()%>/mydspace"><fmt:message key="jsp.layout.navbar-default.users"/></a>
                                    <a href="<%= request.getContextPath()%>/profile"><fmt:message key="jsp.layout.navbar-default.edit"/></a>
                                    <%
                                                if (isAdmin)
                                                {
                                    %>
                                    <a href="<%= request.getContextPath()%>/dspace-admin"><fmt:message key="jsp.administer"/></a>
                                    <%
                                                }
                                    %>
                                </div>
                            </li>
                            <li><a href="#" onmouseover="mopen('m2')"
                                   onmouseout="mclosetime()"><fmt:message
                                        key="jsp.layout.navbar-default.browse" /></a>
                                <div id="m2" onmouseover="mcancelclosetime()"
                                     onmouseout="mclosetime()"><a
                                        href="<%=request.getContextPath()%>/community-list"><fmt:message
                                            key="jsp.layout.navbar-default.communities-collections" /></a> <a
                                        href="<%=request.getContextPath()%>/browse?type=title"><fmt:message
                                            key="jsp.layout.navbar-default.titles" /></a> <a
                                        href="<%=request.getContextPath()%>/browse?type=author"><fmt:message
                                            key="jsp.layout.navbar-default.authors" /></a> <a
                                        href="<%=request.getContextPath()%>/browse?type=subject"><fmt:message
                                            key="jsp.layout.navbar-default.subjects" /></a> <a
                                        href="<%=request.getContextPath()%>/browse?type=dateissued"><fmt:message
                                            key="browse.type.item.dateissued" /></a></div>
                            </li>
                            <li><a href="#" onmouseover="mopen('m4')"
                                   onmouseout="mclosetime()"> <fmt:message key="jsp.home.search1" /></a>
                                <div id="m4" onmouseover="mcancelclosetime()"
                                     onmouseout="mclosetime()">

                                    <form name="dummy" method="get"
                                          action="<%=request.getContextPath()%>/simple-search"><%-- <input type="text" name="query" id="tequery" size="10"/><input type=image border="0" src="<%= request.getContextPath() %>/image/search-go.gif" name="submit" alt="Go" value="Go"/> --%>
                                        <center><fmt:message key="jsp.home.search1" /></center>
                                        <br />
                                        <center><input type="text" name="query" id="tequery"
                                                       size="12" /><br />
                                            <input type="submit" name="submit"
                                                   value="<fmt:message key="jsp.layout.navbar-default.go"/>" /></center>
                                        <br />
                                    </form>
                                    <a href="<%=request.getContextPath()%>/advanced-search"><fmt:message
                                            key="jsp.layout.navbar-default.advanced" /></a></div>
                            </li>
                            <li><a href="#" onmouseover="mopen('m3')"
                                   onmouseout="mclosetime()"><fmt:message
                                        key="org.dspace.app.webui.jsptag.ItemTag.lang" /> </a>
                                <div id="m3" onmouseover="mcancelclosetime()" onmouseout="mclosetime()">
                                    <a href="<%=currentPage%>?locale=es"><fmt:message key="oceandocs.jsp.layout.header-default.es" /></a>
                                    <a href="<%=currentPage%>?locale=fr"><fmt:message key="oceandocs.jsp.layout.header-default.fr" /></a>
                                    <a href="<%=currentPage%>?locale=en"><fmt:message key="oceandocs.jsp.layout.header-default.en" /></a>
                                    <a href="<%=currentPage%>?locale=uk"><fmt:message key="oceandocs.jsp.layout.header-default.uk" /></a>
                                    <a href="<%=currentPage%>?locale=ru"><fmt:message key="oceandocs.jsp.layout.header-default.ru" /></a>
                                </div>
                            </li>


                            <li><script type="text/javascript">
                                // Popup window code
                                function newPopup(url) {
                                    popupWindow = window.open(
                                    url,'popUpWindow','height=660,width=500,left=10,top=10,resizable=yes,scrollbars=yes,toolbar=no,menubar=no,location=no,directories=no,status=yes')
                                }
                                </script><a
                                    href="JavaScript:newPopup('<%=request.getContextPath()%>/help/index.html');"><fmt:message
                                        key="jsp.layout.navbar-default.help" /></a></li>
                            <li><a href="http://www.dspace.org/"><fmt:message
                                        key="jsp.layout.navbar-default.about" /></a></li>

                        </ul>
                </td>
            </tr>
        </table>

        <%-- Localization --%>
<%--  <c:if test="${param.locale != null}">--%>
<%--   <fmt:setLocale value="${param.locale}" scope="session" /> --%>
<%-- </c:if> --%>
<%--        <fmt:setBundle basename="Messages" scope="session"/> --%>

        <%-- Page contents --%>

        <%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
        <table class="centralPane" width="100%" border="0" cellpadding="3" cellspacing="1">

            <%-- HACK: valign: for non-CSS compliant Netscape browser --%>
            <tr valign="top">

            <%-- Navigation bar --%>
<%
    if (!navbar.equals("off"))
    {
%>
            <td class="navigationBar">
                <dspace:include page="<%=navbar %>" />
            </td>
<%
    }
%>
            <%-- Page Content --%>

            <%-- HACK: width specified here for non-CSS compliant Netscape 4.x --%>
            <%-- HACK: Width shouldn't really be 100%, but omitting this means --%>
            <%--       navigation bar gets far too wide on certain pages --%>
            <td class="pageContents">

                <%-- Location bar --%>
<%
    if (locbar)
    {
%>
                <dspace:include page="/layout/location-bar.jsp" />
<%
    }
%>
