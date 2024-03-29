<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="java.util.MissingResourceException"%>
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.core.I18nUtil"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.dspace.content.DCValue"%>
<%--
  - Perform task page
  -
  - Attributes:
  -    workflow.item: The workflow item for the task being performed
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

<%
    WorkflowItem workflowItem =
        (WorkflowItem) request.getAttribute("workflow.item");

    Collection collection = workflowItem.getCollection();
    Item item = workflowItem.getItem();
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.perform-task.title"
               nocache="true">

    <%-- <h1>Perform Task</h1> --%>
    <h1><fmt:message key="jsp.mydspace.perform-task.title"/></h1>
    
<%
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1)
    {
%>
	<p><fmt:message key="jsp.mydspace.perform-task.text1">
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
         </fmt:message></p>
<%
    }
    else if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP2)
    {
%>
	<p><fmt:message key="jsp.mydspace.perform-task.text3">
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
	</fmt:message></p>
<%
    }
    else if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP3)
    {
%>
	<p><fmt:message key="jsp.mydspace.perform-task.text4">
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
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

    <p>&nbsp;</p>

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.PERFORM_TASK_PAGE %>"/>
        <table class="miscTable" width="80%">
<%
    String row = "odd";
    
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1 ||
        workflowItem.getState() == WorkflowManager.WFSTATE_STEP2)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <%-- If you have reviewed the item and it is suitable for inclusion in the collection, select "Approve". --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct1"/>
                </td>
                <td class="<%= row %>RowEvenCol" valign="middle">
                    <%-- <input type="submit" name="submit_approve" value="Approve"> --%>
					<input type="submit" name="submit_approve" value="<fmt:message key="jsp.mydspace.general.approve"/>" />
                </td>
            </tr>
<%
    }
    else
    {
        // Must be an editor (step 3)
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <%-- Once you've edited the item, use this option to commit the
                    item to the archive. --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct2"/>
                </td>
                <td class="<%= row %>RowEvenCol" valign="middle">
                    <%-- <input type="submit" name="submit_approve" value="Commit to Archive"> --%>
					<input type="submit" name="submit_approve" value="<fmt:message key="jsp.mydspace.perform-task.commit.button"/>" />
                </td>
            </tr>
<%
    }
    row = "even";

    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1 ||
        workflowItem.getState() == WorkflowManager.WFSTATE_STEP2)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <%-- If you have reviewed the item and found it is <strong>not</strong> suitable
                    for inclusion in the collection, select "Reject".  You will then be asked 
                    to enter a message indicating why the item is unsuitable, and whether the
                    submitter should change something and re-submit. --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct3"/>
                </td>
                <td class="<%= row %>RowEvenCol" valign="middle">
	        	<input type="submit" name="submit_reject" value="<fmt:message key="jsp.mydspace.general.reject"/>"/>
                </td>
            </tr>
<%
        row = ( row.equals( "odd" ) ? "even" : "odd" );
    }

    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP2 ||
        workflowItem.getState() == WorkflowManager.WFSTATE_STEP3)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <%-- Select this option to correct, amend or otherwise edit the item's metadata. --%>
			<fmt:message key="jsp.mydspace.perform-task.instruct4"/>
                </td>
                <td class="<%= row %>RowEvenCol" valign="middle">
			<input type="submit" name="submit_edit" value="<fmt:message key="jsp.mydspace.perform-task.edit.button"/>" />
                </td>
            </tr>
<%
        row = (row.equals( "odd" ) ? "even" : "odd");
    }
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <%-- If you wish to leave this task for now, and return to your "My DSpace", use this option. --%>
                    <fmt:message key="jsp.mydspace.perform-task.instruct5"/>
				</td>
                <td class="<%= row %>RowEvenCol" valign="middle">
			<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.perform-task.later.button"/>" />
                </td>
            </tr>
<%
    row = (row.equals( "odd" ) ? "even" : "odd");
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    <%-- To return the task to the pool so that another user can perform the task, use this option. --%>
                    <fmt:message key="jsp.mydspace.perform-task.instruct6"/>
				</td>
                <td class="<%= row %>RowEvenCol" valign="middle">
			<input type="submit" name="submit_pool" value="<fmt:message key="jsp.mydspace.perform-task.return.button"/>" />
                </td>
            </tr>
        </table>
    </form>
</dspace:layout>
