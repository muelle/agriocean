<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.submit.step.UploadStep"%>
<%--
    Created on : Jun 10, 2012
    Author     : Denys Slipetskyy
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<!DOCTYPE html>
<html>
    <head>
        <title>Upload files</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="Generator" content="AgriOcean" />
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
                    /*post_params : {
                        "<%=UploadStep.SUBMIT_UPLOAD_BUTTON%>" : "<fmt:message key="jsp.submit.general.next"/>"
                    },*/
                    debug: true,

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

                swfu = new SWFUpload(settings_object);
            };
        </script>
    </head>

    <body>
        <div id="content">
            <p><fmt:message key="jsp.submit.choose-multiple-files"/></p>
            <form id="form1" method="post" action="<%= request.getContextPath()%>/submit" enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">
                <div class="fieldset flash" id="fsUploadProgress">
                    <span class="legend">Upload Queue</span>
                </div>
                <div id="divStatus">0 Files Uploaded</div>
                <div>
                    <span id="UploadButtonPlaceHolder"/>
                    <input id="btnCancel" type="button" value="Cancel All Uploads" onclick="swfu.cancelQueue();" disabled="disabled" style="margin-left: 2px; font-size: 8pt; height: 29px;" />
                </div>

            </form>
        </div>
    </body>
</html>

