<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="java.util.MissingResourceException"%>
<%@page import="org.dspace.core.I18nUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.dspace.content.DCValue"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@page import="org.dspace.content.Bundle"%>
<%@page import="java.util.Enumeration"%>
<%@page import="java.util.Map"%>
<%--
  - Preview task page
  -
  -   workflow.item:  The workflow item for the task they're performing
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>

<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
  WorkflowItem workflowItem =
          (WorkflowItem) request.getAttribute("workflow.item");

  Collection collection = workflowItem.getCollection();
  Item item = workflowItem.getItem();
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.preview-task.title"
               nocache="true">

  <h1><fmt:message key="jsp.mydspace.preview-task.title"/></h1>

  <%
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1POOL) {
  %>
  <p><fmt:message key="jsp.mydspace.preview-task.text1"> 
      <fmt:param><%= collection.getMetadata("name")%></fmt:param>
    </fmt:message></p>
    <%
    } else if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP2POOL) {
    %>    
  <p><fmt:message key="jsp.mydspace.preview-task.text3"> 
      <fmt:param><%= collection.getMetadata("name")%></fmt:param>
    </fmt:message></p>
    <%
    } else if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP3POOL) {
    %>
  <p><fmt:message key="jsp.mydspace.preview-task.text4"> 
      <fmt:param><%= collection.getMetadata("name")%></fmt:param>
    </fmt:message></p>
    <%
      }

      String[] fieldsToShow = {"dc.title", "dc.contributor.*", "dc.date.issued", "dc.type", "dc.abstract", "dc.identifier.citation"};
      LinkedHashMap<String, ArrayList<String>> metadata = new LinkedHashMap<String, ArrayList<String>>();
      DCValue[] dcvs;

      for (String field : fieldsToShow) {
        String label = "";
        try {
          label = I18nUtil.getMessage("metadata." + field, UIUtil.obtainContext(request));
        } catch (MissingResourceException e) {
          // if there is not a specific translation for the style we
          // use the default one
          label = field;
        }
        dcvs = item.getMetadata(field);
        if (dcvs.length > 0) {
          ArrayList<String> values = new ArrayList<String>();

          for (DCValue dcv : dcvs) {
            values.add(dcv.value);
          }
          metadata.put(label, values);
        }
      }
    %>

    <table class="itemDisplayTable">
      <%
        String resultValue = "";
        for (Map.Entry<String, ArrayList<String>> entry : metadata.entrySet()) {
          resultValue = "";
          for (String value : entry.getValue()) {
            resultValue = resultValue + value + ", ";
          }
          resultValue = resultValue.substring(0, resultValue.length() - 2);
      %>
      <tr>
        <td class="metadataFieldLabel"><%= entry.getKey()%></td>
        <td class="metadataFieldValue"><%= resultValue%></td>
      </tr>
      <%
        }
      %>
    </table>

  <br />

  <form action="<%= request.getContextPath()%>/mydspace" method="post">

    <input type="hidden" name="workflow_id" value="<%= workflowItem.getID()%>"/>
    <input type="hidden" name="step" value="<%= MyDSpaceServlet.PREVIEW_TASK_PAGE%>"/>
    <table border="0" width="90%" cellpadding="10" align="center">
      <tr>
        <td align="left">
          <input type="submit" name="submit_start" value="<fmt:message key="jsp.mydspace.preview-task.accept.button"/>" />
        </td>
        <td align="right">
          <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.general.cancel"/>" />
        </td>
      </tr>
    </table>
  </form>

</dspace:layout>
