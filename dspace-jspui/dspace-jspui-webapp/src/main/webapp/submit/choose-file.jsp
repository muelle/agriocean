<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.submit.step.UploadStep" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    // Determine whether a file is REQUIRED to be uploaded (default to true)
    boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);  
%>

<link rel="stylesheet" href="<%= request.getContextPath()%>/static/js/swfupload/default.css" type="text/css" />

<script type="text/javascript" src="<%= request.getContextPath()%>/static/js/swfupload/swfupload.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/static/js/swfupload/handlers.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/static/js/swfupload/fileprogress.js"></script>
<script type="text/javascript" src="<%= request.getContextPath()%>/static/js/swfupload/swfupload.queue.js"></script>

<script type="text/javascript">
    window.onload = function () {
        var settings_object = {
            upload_url : "<%= request.getContextPath()%>/submit;jsessionid=${pageContext.session.id}",
            flash_url : "<%= request.getContextPath()%>/static/js/swfupload/swfupload.swf",
            flash9_url : "<%= request.getContextPath()%>/static/js/swfupload/swfupload_fp9.swf",
            file_size_limit : "100 MB",
            file_post_name: "file",
                   
            custom_settings : {
                progressTarget : "fsUploadProgress",
                cancelButtonId : "btnCancel"
            },
            post_params : <%= SubmissionController.getSubmissionParametersAsJSobject(context, request) %>,
            debug: false,

            // Button settings
            button_image_url : "<%= request.getContextPath()%>/static/js/swfupload/TestImageNoText_65x29.png",
            button_width: "65",
            button_height: "29",
            button_placeholder_id: "UploadButtonPlaceHolder",
            button_text: 'Upload',
            //button_text_style: ".theFont { font-size: 16; }",
            button_text_left_padding: 12,
            button_text_top_padding: 3,

            // The event handler functions are defined in handlers.js
            swfupload_preload_handler : preLoad,
            swfupload_load_failed_handler : loadFailed,
            file_queued_handler : fileQueued,
            file_queue_error_handler : fileQueueError,
            file_dialog_complete_handler : fileDialogComplete,
            upload_start_handler : uploadStart,
            upload_progress_handler : uploadProgress,
            upload_error_handler : uploadError,
            upload_success_handler : uploadSuccess,
            upload_complete_handler : uploadComplete,
            queue_complete_handler : queueComplete	// Queue plugin event

        };

        
        if (swfobject.getFlashPlayerVersion().major <=0) {
            document.getElementById("multipleUpload").hide()
        } else {
            document.getElementById("singleUpload").hide();
            swfu = new SWFUpload(settings_object);
        }
            
    };
</script>




<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.choose-file.title"
               nocache="true">

    <jsp:include page="/submit/progressbar.jsp"/>

    <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
    <%= SubmissionController.getSubmissionParameters(context, request)%>
    <center>
        <%-- <h1>Submit: Upload a File</h1> --%>
        <h1><fmt:message key="jsp.submit.choose-file.heading"/></h1>

        <%-- <p>Please enter the name of
        <%= (si.submission.hasMultipleFiles() ? "one of the files" : "the file" ) %> on your
        local hard drive corresponding to your item.  If you click "Browse...", a
        new window will appear in which you can locate and select the file on your
        local hard drive. <object><dspace:popup page="/help/index.html#upload">(More Help...)</dspace:popup></object></p> --%>

        <p><fmt:message key="jsp.submit.choose-file.info1"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, 
                
            
                \"help.index\") + \"#upload\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></p>

        <%-- FIXME: Collection-specific stuff should go here? --%>
        <%-- <p class="submitFormHelp">Please also note that the DSpace system is
        able to preserve the content of certain types of files better than other
        types.
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>">Information about file types</dspace:popup> and levels of
        support for each are available.</p> 
        
        --%>

        <div class="submitFormHelp"><fmt:message key="jsp.submit.choose-file.info6"/></div>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext,                 
                
        
            \"help.formats\")%>"><fmt:message key="jsp.submit.choose-file.info7"/></dspace:popup>
    </div>



    <form method="post" action="<%= request.getContextPath()%>/submit" enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">

    <!--<a href="<%= request.getContextPath()%>/submit/multiple-files-upload.jsp" 
       onclick="window.open('<%= request.getContextPath()%>/submit/multiple-files-upload.jsp','popup',
           'width=600,height=500,scrollbars=no,resizable=no,toolbar=no,directories=no,location=no,menubar=no,status=no,left=0,top=0'); return false">
        <p><fmt:message key="jsp.submit.choose-multiple-files"/></p></a> -->
        <div id="multipleUpload" class="fileUploadForm">
            <!-- <p><fmt:message key="jsp.submit.choose-multiple-files"/></p> -->
            <div class="fieldset flash" id="fsUploadProgress">
                <span class="legend">Upload Queue</span>
            </div>
            <div id="divStatus">0 Files Uploaded</div>
            <div>
                <span id="UploadButtonPlaceHolder"/>
                <input id="btnCancel" type="button" value="Cancel All Uploads" onclick="swfu.cancelQueue();" disabled="disabled" style="margin-left: 2px; font-size: 8pt; height: 29px;" />
            </div>
        </div>

        <div id="singleUpload" class="fileUploadForm">   
            <table border="0" align="center">
                <tr>
                    <td class="submitFormLabel">
                        <%-- Document File: --%>
                        <label for="tfile"><fmt:message key="jsp.submit.choose-file.document"/></label>
                    </td>
                    <td>
                        <input type="file" size="40" name="file" id="tfile" />
                    </td>
                </tr>
                <%
                    if (subInfo.getSubmissionItem().hasMultipleFiles()) {
                %>
                <tr>
                    <td colspan="2">&nbsp;</td>
                </tr>
                <tr>
                    <td class="submitFormHelp" colspan="2">
                        <%-- Please give a brief description of the contents of this file, for
                        example "Main article", or "Experiment data readings." --%>
                        <fmt:message key="jsp.submit.choose-file.info9"/>
                    </td>
                </tr>
                <tr>
                    <%-- <td class="submitFormLabel">File Description:</td> --%>
                    <td class="submitFormLabel"><label for="tdescription"><fmt:message key="jsp.submit.choose-file.filedescr"/></label></td>
                    <td><input type="text" name="description" id="tdescription" size="40"/></td>
                </tr>
                <%    }
                %>
            </table>
        </div>
        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request)%>

        <p>&nbsp;</p>

        <center>

            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <%  //if not first step, show "Previous" button
                        if (!SubmissionController.isFirstStep(request, subInfo)) {%>
                    <td>
                        <input type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
                    <%  }%>
                    <td>
                        <input id="next" type="submit" name="<%=UploadStep.SUBMIT_UPLOAD_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td> 
                    <%
                        //if upload is set to optional, or user returned to this page after pressing "Add Another File" button
                        if (!fileRequired || UIUtil.getSubmitButton(request, "").equals(UploadStep.SUBMIT_MORE_BUTTON)) {
                    %>
                    <td>
                        <input type="submit" name="<%=UploadStep.SUBMIT_SKIP_BUTTON%>" value="<fmt:message key="jsp.submit.choose-file.skip"/>" />
                    </td>
                    <%
                        }
                    %>   

                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <input type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>  
    </form>



</dspace:layout>
