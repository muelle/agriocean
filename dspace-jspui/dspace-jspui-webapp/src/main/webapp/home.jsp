<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.core.Context"%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>

<%@ page import="proj.oceandocs.components.BrowseAC"%>
<%@ page import="proj.oceandocs.components.RecentSubm" %>

<%
            Context context = null;
            context = UIUtil.obtainContext(request);

            Community[] communities = (Community[]) request.getAttribute("communities");

            Locale[] supportedLocales = I18nUtil.getSupportedLocales();
            Locale sessionLocale = UIUtil.getSessionLocale(request);
            Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
            String topNews = ConfigurationManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
            String sideNews = ConfigurationManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

            boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
            String feedData = "NONE";
            if (feedEnabled)
            {
                feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
            }

            ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData%>">

    <table  width="95%" align="center" class="miscTable">
        
        <tr>
            <td class="latestLayoutTitle"><%= topNews%></td>
        </tr>
    </table>
    <br/>
    <form action="<%= request.getContextPath()%>/simple-search" method="get">
        <table class="miscTable" width="95%" align="center">
            <tr>
                <td class="latestLayoutTitle">
                    <h3><fmt:message key="jsp.home.search1"/></h3>
                    <p><label for="tquery"><fmt:message key="jsp.home.search2"/></label></p>
                    <p><input type="text" name="query" size="20" id="tquery" />&nbsp;
                        <input type="submit" name="submit" value="<fmt:message key="jsp.general.search.button"/>" /></p>
                </td>
            </tr>
        </table>
    </form>
    
    <table class="miscTable" width="95%" align="center">
        <tr>
            <td class="latestLayoutTitle"><fmt:message key="jsp.collection-home.recentsub" /></td>
        </tr>
        <tr>
            <td>
                <table border="0" cellpadding="2" width="100%">
                    <tr>
                        <td>
                            <table cellspacing="2" width="100%">
                                <tr>
                                    <td valign="top" class="latestLayoutTitle"><fmt:message key="itemlist.dc.title" /> - <fmt:message key="metadata.dc.identifier.citation" /> - <fmt:message key="itemlist.dc.contributor.author" /></td>
                                    <td width="10px" align="right" valign="top" class="latestLayoutTitle"><fmt:message key="itemlist.dc.type" /></td>
                                </tr>

                                <%
                                            // generate the recent submissions
                                            out.print(RecentSubm.GenerateHTML(context, 5, request.getContextPath()));%>
                            </table>
                        </td>

                    </tr>
                </table>
            </td>
        </tr>
    </table>
    
</dspace:layout>
