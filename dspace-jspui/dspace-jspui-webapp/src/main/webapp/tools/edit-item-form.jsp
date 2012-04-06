<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="java.util.Arrays"%>
<%--
  - Show form allowing edit of collection metadata
  -
  - Attributes:
  -    item        - item to edit
  -    collections - collections the item is in, if any
  -    handle      - item's Handle, if any (String)
  -    dc.types    - MetadataField[] - all metadata fields in the registry
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@page import="java.util.Collections"%>
<%@page import="java.util.Date" %>
<%@page import="java.util.HashMap" %>
<%@page import="java.util.Map" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@page import="javax.servlet.jsp.PageContext" %>
<%@page import="org.dspace.content.MetadataField" %>
<%@page import="org.dspace.app.webui.servlet.admin.AuthorizeAdminServlet" %>
<%@page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>
<%@page import="org.dspace.content.Bitstream" %>
<%@page import="org.dspace.content.BitstreamFormat" %>
<%@page import="org.dspace.content.Bundle" %>
<%@page import="org.dspace.content.Collection" %>
<%@page import="org.dspace.content.DCDate" %>
<%@page import="org.dspace.content.DCValue" %>
<%@page import="org.dspace.content.Item" %>
<%@page import="org.dspace.core.ConfigurationManager" %>
<%@page import="org.dspace.eperson.EPerson" %>
<%@page import="org.dspace.core.Utils" %>
<%@page import="org.dspace.content.authority.MetadataAuthorityManager" %>
<%@page import="org.dspace.content.authority.ChoiceAuthorityManager" %>
<%@page import="org.dspace.content.authority.Choices" %>

<%@page import="proj.oceandocs.submission.DCInputGroup" %>
<%@page import="org.dspace.app.util.DCInputsReaderException" %>
<%@page import="proj.oceandocs.submission.DCInputsReaderExt" %>
<%@page import="proj.oceandocs.submission.DCInputSetExt" %>

<%@page import="org.apache.commons.collections.KeyValue"%>
<%@page import="org.dspace.app.util.DCInput"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.dspace.core.I18nUtil"%>
<%@page import="java.util.List"%>


<!--Authority-->
<%!    // required by Controlled Vocabulary  add-on and authority addon
    String contextPath;
    // An unknown value of confidence for new, empty input fields,
    // so no icon appears yet.
    int unknownConfidence = Choices.CF_UNSET - 100;

%>
<%!    void doAuthorityField(javax.servlet.jsp.JspWriter out, Item item, String fieldName, DCInput field, int fieldCountIncr, boolean readonly, PageContext pageContext, String contextPath) throws java.io.IOException, DCInputsReaderException {
        String isize = field.getSize() != 0 ? Integer.toString(field.getSize()) : "30";
        String fieldCounter = "";

        DCValue[] defaults = item.getMetadata(field.getSchema(), field.getElement(), field.getQualifier(), Item.ANY);

        boolean repeatable = field.isRepeatable();
        boolean askLang = field.getAskLanguage();

        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        String val = "", auth = "";

        int conf = unknownConfidence;
        if (fieldCount == 0) {
            fieldCount = 1;
        }

        String acField = "", acIndicator = "", acUrl = "";

        for (int i = 0; i < fieldCount; i++) {
            if (i < defaults.length) {
                val = defaults[i].value.replaceAll("\"", "&quot;");
                auth = defaults[i].authority != null ? defaults[i].authority : "";
                conf = defaults[i].confidence;
            } else {
                val = "";
                auth = "";
                conf = unknownConfidence;
            }


            if (field.getPresentation() == DCInput.AuthorityPresentation.SUGGEST) {
                sb.append("<td colspan=\"2\">");
                String fieldNameIdx = fieldName + ((repeatable && i != fieldCount - 1) ? "_" + (i + 1) : "");
                String fieldAuthority = fieldName + "_authority" + ((repeatable && i != fieldCount - 1) ? "_" + (i + 1) : "");
                String fieldConfidence = fieldName + "_confidence" + ((repeatable && i != fieldCount - 1) ? "_" + (i + 1) : "");
                sb.append("<input type=\"text\" name=\"").append(fieldNameIdx).append("\" id=\"").append(fieldNameIdx).append("\" size=\"" + isize + "\" value=\"").append(val + "\"").append(readonly ? " readonly=\"true\" " : "").append("/>").append("\n");

                //autocomplete "magic"
                acIndicator = fieldNameIdx + "_indicator";
                acField = "autocomplete_" + fieldNameIdx;
                acUrl = contextPath + "/authority/" + field.getAuthorityURLsuffix();
                sb.append("<span id=\"").append(acIndicator).append("\" style=\"display: none;\">").append("<img src=\"").append(contextPath).append("/image/authority/load-indicator.gif\" alt=\"Loading...\"/></span>");
                sb.append("<div id=\"").append(acField).append("\" class=\"autocomplete\"></div>");
                //====================
                sb.append("</td>\n");

                //authority value field ... editable if the field value is not in the authority list and authorityis not closed
                sb.append("<td>").append("<input type=\"text\" name=\"").append(fieldAuthority).append("\" id=\"").append(fieldAuthority).append("\" \" value=\"").append(auth + "\" size=\"10\"").append(field.isAuthorityClosed() ? " readonly=\"true\"" : "").append("/>").append("\n");
                sb.append("<input type=\"hidden\" name=\"").append(fieldConfidence).append("\" id=\"").append(fieldConfidence).append("\" value=\"").append(conf + "\" />");
                sb.append("</td>");

                if (repeatable && !readonly && i < defaults.length) {
                    // put language selection list if neccessary (for the dc lang attribute)
                    if (askLang) {
                        String fieldNameLang = fieldName + "_lang";
                        if (repeatable && i != fieldCount - 1) {
                            fieldCounter = '_' + Integer.toString(i + 1);
                        }
                        doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                    } else {
                        sb.append("<td>&nbsp;</td>");
                    } // put a remove button next to filled in values
                    sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");

                } else if (repeatable && !readonly && i == fieldCount - 1) {
                    // put language selection list if neccessary (for the dc lang attribute)
                    if (askLang) {
                        String fieldNameLang = fieldName + "_lang";

                        if (repeatable && i != fieldCount - 1) {
                            fieldCounter = '_' + Integer.toString(i + 1);
                        }
                        doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                    } else {
                        sb.append("<td>&nbsp;</td>");
                    }
                    // put a 'more' button next to the last space
                    sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td>");
                } else {
                    if (askLang) {
                        String fieldNameLang = fieldName + "_lang";
                        doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                    } else {
                        sb.append("<td>&nbsp;</td>");
                    }
                } // put language selection list if neccessary (for the dc lang attribute)

                sb.append("</tr>");
                sb.append("<script type=\"text/javascript\">");
                sb.append("authoritySuggest(\"").append(fieldNameIdx).append("\", \"").append(acField).append("\", \"").append(acUrl).append("\", '").append(acIndicator).append("');");
                sb.append("</script>");
            }
        }
        out.write(sb.toString());
        sb.append("</tr>");
    }
%>
<%!     // This method is resposible for showing a link next to an input box
    // that pops up a window that to display a controlled vocabulary.
    // It should be called from the doOneBox and doTwoBox methods.
    // It must be extended to work with doTextArea.
    String doControlledVocabulary(String fieldName, PageContext pageContext, String vocabulary, boolean readonly) {
        String link = "";
        boolean enabled = ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable");
        boolean useWithCurrentField = vocabulary != null && !"".equals(vocabulary);

        if (enabled && useWithCurrentField && !readonly) {
            // Deal with the issue of _0 being removed from fieldnames in the configurable submission system
            if (fieldName.endsWith("_0")) {
                fieldName = fieldName.substring(0, fieldName.length() - 2);
            }
            link = "<br/>"
                    + "<a href='javascript:void(null);' onclick='javascript:popUp(\""
                    + contextPath + "/controlledvocabulary/controlledvocabulary.jsp?ID="
                    + fieldName + "&amp;vocabulary=" + vocabulary + "\")'>"
                    + "<span class='controlledVocabularyLink'>"
                    + LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.controlledvocabulary")
                    + "</span>"
                    + "</a>";
        }
        return link;
    }

    boolean hasVocabulary(String vocabulary) {
        boolean enabled = ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable");
        boolean useWithCurrentField = (vocabulary != null && !"".equals(vocabulary));
        boolean has = false;

        if (enabled && useWithCurrentField) {
            has = true;
        }
        return has;
    }

    //is this field going to be rendered as Choice-driven <select>?
    boolean isSelectable(String fieldKey) {
        ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
        return (cam.isChoicesConfigured(fieldKey)
                && "select".equals(cam.getPresentation(fieldKey)));
    }

    // Render the choice/authority controlled entry, or, if not indicated,
    // returns the given default inputBlock
 /*
     * StringBuffer doAuthority(PageContext pageContext, String fieldName, int
     * idx, int fieldCount, String fieldInput, String authorityValue, int
     * confidenceValue, boolean isName, boolean repeatable, DCValue[] dcvs,
     * StringBuffer inputBlock, int collectionID, int size, String contextPath)
     * { MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
     * ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
     * StringBuffer sb = new StringBuffer();
     *
     * if (cam.isChoicesConfigured(fieldName)) { boolean authority =
     * mam.isAuthorityControlled(fieldName); boolean required = authority &&
     * mam.isAuthorityRequired(fieldName); boolean isSelect =
     * "select".equals(cam.getPresentation(fieldName)) && !isName;
     *
     * // if this is not the only or last input, append index to input @names
     * String authorityName = fieldName + "_authority"; String confidenceName =
     * fieldName + "_confidence"; if (repeatable && !isSelect && idx !=
     * fieldCount - 1) { fieldInput += '_' + String.valueOf(idx + 1);
     * authorityName += '_' + String.valueOf(idx + 1); confidenceName += '_' +
     * String.valueOf(idx + 1); }
     *
     * String confidenceSymbol = confidenceValue == unknownConfidence ? "blank"
     * : Choices.getConfidenceText(confidenceValue).toLowerCase(); String
     * confIndID = fieldInput + "_confidence_indicator_id"; if (authority) {
     * sb.append(" <img id=\"" + confIndID + "\"
     * title=\"").append(LocaleSupport.getLocalizedMessage(pageContext,
     * "jsp.authority.confidence.description." + confidenceSymbol)).append("\"
     * class=\"ds-authority-confidence cf-") // set confidence to cf-blank if
     * authority is empty .append(authorityValue == null ||
     * authorityValue.length() == 0 ? "blank" : confidenceSymbol).append(" \"
     * src=\"").append(contextPath).append("/image/confidence/invisible.gif\"
     * />").append("<input type=\"text\" value=\"").append(authorityValue !=
     * null ? authorityValue : "").append("\"
     * id=\"").append(authorityName).append("\"
     * name=\"").append(authorityName).append("\"
     * class=\"ds-authority-value\"/>").append("<input type=\"hidden\"
     * value=\"").append(confidenceSymbol).append("\"
     * id=\"").append(confidenceName).append("\"
     * name=\"").append(confidenceName).append("\"
     * class=\"ds-authority-confidence-input\"/>"); }
     *
     * // suggest is not supported for name input type if
     * ("suggest".equals(cam.getPresentation(fieldName)) && !isName) { if
     * (inputBlock != null) { sb.insert(0, inputBlock); } sb.append("<span
     * id=\"").append(fieldInput).append("_indicator\">").append("<img
     * src=\"").append(contextPath).append("/image/authority/load-indicator.gif\"
     * alt=\"Loading...\"/>").append("</span><div
     * id=\"").append(fieldInput).append("_autocomplete\" class=\"autocomplete\"
     * style=\"display: block;\"> </div>");
     *
     * sb.append("<script type=\"text/javascript\">").append("var gigo =
     * DSpaceSetupAutocomplete('edit_metadata',").append("{ metadataField:
     * '").append(fieldName).append("', isClosed: '").append(required ? "true" :
     * "false").append("', inputName: '").append(fieldInput).append("',
     * authorityName: '").append(authorityName).append("', containerID:
     * '").append(fieldInput).append("_autocomplete', indicatorID:
     * '").append(fieldInput).append("_indicator', ").append("contextPath:
     * '").append(contextPath).append("', confidenceName:
     * '").append(confidenceName).append("', confidenceIndicatorID:
     * '").append(confIndID).append("', collection:
     * ").append(String.valueOf(collectionID)).append(" }); </script>"); } //
     * put up a SELECT element containing all choices else if (isSelect) {
     * sb.append("<select id=\"").append(fieldInput).append("_id\"
     * name=\"").append(fieldInput).append("\"
     * size=\"").append(String.valueOf(repeatable ? 6 : 1)).append(repeatable ?
     * "\" multiple>\n" : "\">\n"); Choices cs = cam.getMatches(fieldName, "",
     * collectionID, 0, 0, null); // prepend unselected empty value when nothing
     * can be selected. if (!repeatable && cs.defaultSelected < 0 && dcvs.length
     * == 0) { sb.append("<option value=\"\"><!-- empty --></option>\n"); } for
     * (int i = 0; i < cs.values.length; ++i) { boolean selected = false; for
     * (DCValue dcv : dcvs) { if (dcv.value.equals(cs.values[i].value)) {
     * selected = true; } } sb.append("<option
     * value=\"").append(cs.values[i].value.replaceAll("\"",
     * "\\\"")).append("\"").append(selected ? " selected>" :
     * ">").append(cs.values[i].label).append("</option>\n"); }
     * sb.append("</select>\n"); } // use lookup for any other presentation
     * style (i.e "select") else { if (inputBlock != null) { sb.insert(0,
     * inputBlock); } sb.append("<input type=\"image\"
     * name=\"").append(fieldInput).append("_lookup\"
     * ").append("onclick=\"javascript: return
     * DSpaceChoiceLookup('").append(contextPath).append("/tools/lookup.jsp','").append(fieldName).append("','edit_metadata','").append(fieldInput).append("','").append(authorityName).append("','").append(confIndID).append("',").append(String.valueOf(collectionID)).append(",").append(String.valueOf(isName)).append(",false);\"").append("
     * title=\"").append(LocaleSupport.getLocalizedMessage(pageContext,
     * "jsp.tools.lookup.lookup")).append("\" width=\"16px\" height=\"16px\"
     * src=\"" + contextPath + "/image/authority/zoom.png\" />"); } } else if
     * (inputBlock != null) { sb = inputBlock; } return sb; }
     *
     */
%>
<!--doLang-->
<%!    void doLang(StringBuffer sb, Item item,
            String fieldName, String fieldCounter, boolean repeatable, int size)
            throws java.io.IOException, DCInputsReaderException {
        //String isize = size !=0 ? Integer.toString(size): "6";

        List languages = new DCInputsReaderExt().getPairs("common_iso_languages");
        String display, value;
        int j;
        String langAttr = "";
        String[] qualifiedFieldName = fieldName.split("_");
        String schema = null;
        String element = null;
        String qulifer = null;

        if (qualifiedFieldName.length > 2 && qualifiedFieldName.length < 4) {
            schema = qualifiedFieldName[0];
            element = qualifiedFieldName[1];
        } else if (qualifiedFieldName.length >= 4) {
            schema = qualifiedFieldName[0];
            element = qualifiedFieldName[1];
            qulifer = qualifiedFieldName[2];
        }
        DCValue[] metadata = item.getMetadata(schema, element, qulifer, Item.ANY);
        if (metadata.length > 0) {
            try {
                if (java.lang.Integer.parseInt(fieldCounter.replace("_", "")) <= metadata.length) {
                    langAttr = metadata[java.lang.Integer.parseInt(fieldCounter.replace("_", "")) - 1].language;
                }
            } catch (Exception e) {
                langAttr = "";
            }
        }
        sb.append("<td>").append("<select name=\"").append(fieldName);

        if (repeatable && !"".equals(fieldCounter)) {
            sb.append(fieldCounter);
        }

        sb.append("\"");
        //sb.append(" size=\""+isize+"\">");
        sb.append(">");


        for (int i = 0;
                i < languages.size();
                i += 2) {
            display = (String) languages.get(i);
            value = (String) languages.get(i + 1);

            sb.append("<option ");
            if (value.equals(langAttr)) {
                sb.append("selected=\"selected\" ");
            }
            sb.append("value=\"").append(value.replaceAll("\"", "&quot;")).append("\">").append(display).append("</option>");
        }

        sb.append("</select></td>");
    }
%>
<!--doPersonalName-->
<%!    void doPersonalName(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable,
            boolean readonly, int fieldCountIncr, String label, PageContext pageContext, int collectionID, int size, boolean askLang, String contextPath)
            throws java.io.IOException, DCInputsReaderException {

        String isize = size != 0 ? Integer.toString(size) : "23";
        String fieldCounter = "";

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);

        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer headers = new StringBuffer();
        StringBuffer sb = new StringBuffer();
        org.dspace.content.DCPersonName dpn;
        String auth;

        int conf = 0;
        StringBuffer name = new StringBuffer();
        StringBuffer first = new StringBuffer();
        StringBuffer last = new StringBuffer();

        if (fieldCount == 0) {
            fieldCount = 1;
        }

        //Width hints used here to affect whole table
        headers.append("<td class=\"submitFormDateLabel\" >").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.lastname")).append("</td>").append("<td class=\"submitFormDateLabel\" >")//width=\"5%\"
                .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.firstname")).append("</td>").append("<td >&nbsp;</td><td>&nbsp;</td>")//width=\"40%\"
                .append("</tr>");
        out.write(headers.toString());


        for (int i = 0; i < fieldCount; i++) {
            first.setLength(0);
            first.append(fieldName).append("_first");
            if (repeatable && i != fieldCount - 1) {
                first.append('_').append(i + 1);
            }

            last.setLength(0);
            last.append(fieldName).append("_last");


            if (repeatable && i != fieldCount - 1) {
                last.append('_').append(i + 1);
            }

            if (i < defaults.length) {
                dpn = new org.dspace.content.DCPersonName(defaults[i].value);
                auth = defaults[i].authority;
                conf = defaults[i].confidence;
            } else {
                dpn = new org.dspace.content.DCPersonName();
                auth = "";
                conf = unknownConfidence;
            }

            sb.append("<tr><td><input type=\"text\" name=\"").append(last.toString()).append("\" size=\"" + isize + "\" ");


            if (readonly) {
                sb.append("disabled=\"disabled\" ");


            }
            sb.append("value=\"").append(dpn.getLastName().replaceAll("\"", "&quot;")) // Encode "
                    .append("\"/></td>\n<td nowrap=\"nowrap\"><input type=\"text\" name=\"").append(first.toString()).append("\" size=\"" + isize + "\" ");


            if (readonly) {
                sb.append("disabled=\"disabled\" ");
            }
            sb.append("value=\"").append(dpn.getFirstNames()).append("\"/>").append("</td>\n");

            if (repeatable && !readonly && i < defaults.length) {
                name.setLength(0);
                name.append(dpn.getLastName()).append(' ').append(dpn.getFirstNames());

                // put language selection list if neccessary (for the dc lang attribute)


                if (askLang) {
                    String fieldNameLang = fieldName + "_lang";


                    if (repeatable && i != fieldCount - 1) {
                        fieldCounter = '_' + Integer.toString(i + 1);
                    }

                    doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);

                } else {
                    sb.append("<td>&nbsp;</td>");


                } // put a remove button next to filled in values
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");//
            } else if (repeatable && !readonly && i == fieldCount - 1) {
                // put language selection list if neccessary (for the dc lang attribute)
                if (askLang) {
                    String fieldNameLang = fieldName + "_lang";


                    if (repeatable && i != fieldCount - 1) {
                        fieldCounter = '_' + Integer.toString(i + 1);


                    }
                    doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);


                } else {
                    sb.append("<td>&nbsp;</td>");


                } // put a 'more' button next to the last space
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td></tr>");//
            } else {

                // put language selection list if neccessary (for the dc lang attribute)
                if (askLang) {
                    String fieldNameLang = fieldName + "_lang";


                    if (repeatable && i != fieldCount - 1) {
                        fieldCounter = '_' + Integer.toString(i + 1);


                    }
                    doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                    sb.append("<td>&nbsp;</td>");
                } else {
                    // put a blank if nothing else
                    sb.append("<td>&nbsp;</td>");
                }
                sb.append("</tr>");


            }
        }

        out.write(sb.toString());


    }
%>
<!--doDate-->
<%!    void doDate(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable,
            boolean readonly, int fieldCountIncr, String label, PageContext pageContext, HttpServletRequest request)
            throws java.io.IOException {

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);

        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        org.dspace.content.DCDate dateIssued;

        if (fieldCount == 0) {
            fieldCount = 1;
        }

        for (int i = 0; i < fieldCount; i++) {
            if (i < defaults.length) {
                dateIssued = new org.dspace.content.DCDate(defaults[i].value);
            } else {
                dateIssued = new org.dspace.content.DCDate("");
            }
           
            sb.append("<td colspan=\"2\" nowrap=\"nowrap\" class=\"submitFormDateLabel\">").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.month")).append("<select name=\"").append(fieldName).append("_month");

            if (repeatable && i > 0) {
                sb.append('_').append(i);
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            }
            sb.append("\"><option value=\"-1\"").append((dateIssued.getMonth() == -1 ? " selected=\"selected\"" : "")) //          .append(">(No month)</option>");
                    .append(">").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.no_month")).append("</option>");

            for (int j = 1; j
                    < 13; j++) {
                sb.append("<option value=\"").append(j).append((dateIssued.getMonth() == j ? "\" selected=\"selected\"" : "\"")).append(">").append(org.dspace.content.DCDate.getMonthName(j, I18nUtil.getSupportedLocale(request.getLocale()))).append("</option>");
            }

            sb.append("</select>") //            .append("Day:<input type=text name=\"")
                    .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.day")).append("<input type=\"text\" name=\"").append(fieldName).append("_day");

            if (repeatable && i > 0) {
                sb.append("_").append(i);
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            }
            sb.append("\" size=\"2\" maxlength=\"2\" value=\"").append((dateIssued.getDay() > 0
                    ? String.valueOf(dateIssued.getDay()) : "")) //          .append("\"/>Year:<input type=text name=\"")
                    .append("\"/>").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.year")).append("<input type=\"text\" name=\"").append(fieldName).append("_year");


            if (repeatable && i > 0) {
                sb.append("_").append(i);


            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");


            }
            sb.append("\" size=\"4\" maxlength=\"4\" value=\"").append((dateIssued.getYear() > 0
                    ? String.valueOf(dateIssued.getYear()) : "")).append("\"/></td>\n");



            if (repeatable && !readonly && i < defaults.length) {
                // put a remove button next to filled in values
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i) //            .append("\" value=\"Remove This Entry\"/> </td></tr>");
                        .append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");


            } else if (repeatable && !readonly && i == fieldCount - 1) {
                // put a 'more' button next to the last space
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName) //            .append("_add\" value=\"Add More\"/> </td></tr>");
                        .append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td></tr>");


            } else {
                // put a blank if nothing else
                sb.append("<td>&nbsp;</td></tr>");


            }
        }

        out.write(sb.toString());


    }
%>
<!--doSeriesNumber-->
<%!    void doSeriesNumber(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable,
            boolean readonly, int fieldCountIncr, String label, PageContext pageContext, int size)
            throws java.io.IOException {
        String isize = size != 0 ? Integer.toString(size) : "23";

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);

        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        org.dspace.content.DCSeriesNumber sn;
        StringBuffer headers = new StringBuffer();

        //Width hints used here to affect whole table <td width=\"40%\">&nbsp;</td>"
        //.append("<td width=\"40%\">&nbsp;</td>").
        headers.append("<tr>").append("<td class=\"submitFormDateLabel\" width=\"5%\">").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.seriesname")).append("</td>").append("<td class=\"submitFormDateLabel\" width=\"5%\">").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.paperno")).append("</td>").append("</tr>");
        out.write(headers.toString());

        if (fieldCount == 0) {
            fieldCount = 1;
        }

        for (int i = 0; i < fieldCount; i++) {
            if (i < defaults.length) {
                sn = new org.dspace.content.DCSeriesNumber(defaults[i].value);
            } else {
                sn = new org.dspace.content.DCSeriesNumber();
            }

            sb.append("<td><input type=\"text\" name=\"").append(fieldName).append("_series");

            if (repeatable && i != fieldCount) {
                sb.append("_").append(i + 1);
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            }
            sb.append("\" size=\"" + isize + "\" value=\"").append(sn.getSeries().replaceAll("\"", "&quot;")).append("\"/></td>\n<td><input type=\"text\" name=\"").append(fieldName).append("_number");

            if (repeatable && i != fieldCount) {
                sb.append("_").append(i + 1);
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            }
            sb.append("\" size=\"15\" value=\"").append(sn.getNumber().replaceAll("\"", "&quot;")).append("\"/></td>\n");

            if (repeatable && !readonly && i < defaults.length) {
                // put a remove button next to filled in values
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i) //            .append("\" value=\"Remove This Entry\"/> </td></tr>");
                        .append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");
            } else if (repeatable && !readonly && i == fieldCount - 1) {
                // put a 'more' button next to the last space
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName) //            .append("_add\" value=\"Add More\"/> </td></tr>");
                        .append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td></tr>");
            } else {
                // put a blank if nothing else
                sb.append("<td>&nbsp;</td></tr>");
            }
        }
        out.write(sb.toString());
    }
%>
<!--doTextArea-->
<%!    void doTextArea(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable, boolean readonly,
            int fieldCountIncr, String label, PageContext pageContext, String vocabulary, boolean closedVocabulary, int collectionID, int size, boolean askLang, String contextPath)
            throws java.io.IOException, DCInputsReaderException {
        String isize = (size != 0 && size < 50) ? Integer.toString(size) : "60";
        String fieldCounter = "";

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);

        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        String val, auth;

        int conf = unknownConfidence;

        if (fieldCount == 0) {
            fieldCount = 1;
        }

        for (int i = 0; i < fieldCount; i++) {
            if (i < defaults.length) {
                val = defaults[i].value;
                auth = defaults[i].authority;
                conf = defaults[i].confidence;
            } else {
                val = "";
                auth = "";
            }
            sb.append("<td colspan=\"2\">\n");
            String fieldNameIdx = fieldName + ((repeatable && i != fieldCount - 1) ? "_" + (i + 1) : "");
            sb.append("<textarea name=\"").append(fieldNameIdx).append("\" rows=\"4\" cols=\"" + isize + "\" id=\"").append(fieldNameIdx).append("_id\" ").append((hasVocabulary(vocabulary) && closedVocabulary) || readonly ? " disabled=\"disabled\" " : "").append(">").append(val).append("</textarea>\n").append(doControlledVocabulary(fieldNameIdx, pageContext, vocabulary, readonly));
            sb.append("</td>\n");

            // put language selection list if neccessary (for the dc lang attribute)
            if (askLang) {
                String fieldNameLang = fieldName + "_lang";
                if (repeatable && i != fieldCount - 1) {
                    fieldCounter = '_' + Integer.toString(i + 1);
                }
                doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
            } else {
                sb.append("<td>&nbsp;</td>");
            }

            if (repeatable && !readonly && (i < defaults.length || i == fieldCount - 2)) {
                // put a remove button next to filled in values
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");
            } else if (repeatable && !readonly && i == fieldCount - 1) {
                // put a 'more' button next to the last space
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td></tr>");
            } else {
                // put a blank if nothing else
                sb.append("<td>&nbsp;</td>");
                sb.append("</tr>");
            } // put language selection list if neccessary (for the dc lang attribute)
            sb.append("</tr>");
        }
        out.write(sb.toString());
    }
%>
<!--doOneBox-->
<%!    void doOneBox(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable, boolean readonly,
            int fieldCountIncr, String label, PageContext pageContext, String vocabulary, boolean closedVocabulary, int collectionID, int size, boolean askLang, String contextPath)
            throws java.io.IOException, DCInputsReaderException {
        String isize = size != 0 ? Integer.toString(size) : "30";
        String fieldCounter = "";

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);

        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        String val, auth;

        int conf = 0;
        if (fieldCount == 0) {
            fieldCount = 1;
        }

        for (int i = 0; i
                < fieldCount; i++) {
            if (i < defaults.length) {
                val = defaults[i].value.replaceAll("\"", "&quot;");
                auth = defaults[i].authority;
                conf = defaults[i].confidence;
            } else {
                val = "";
                auth = "";
                conf = unknownConfidence;
            }

            sb.append("<td colspan=\"2\">");
            String fieldNameIdx = fieldName + ((repeatable && i != fieldCount - 1) ? "_" + (i + 1) : "");
            sb.append("<input type=\"text\" name=\"").append(fieldNameIdx).append("\" id=\"").append(fieldNameIdx).append("\" size=\"" + isize + "\" value=\"").append(val + "\"").append((hasVocabulary(vocabulary) && closedVocabulary) || readonly ? " disabled=\"disabled\" " : "").append("/>").append(doControlledVocabulary(fieldNameIdx, pageContext, vocabulary, readonly)).append("\n");
            sb.append("</td>\n");

            if (repeatable && !readonly && i < defaults.length) {
                // put language selection list if neccessary (for the dc lang attribute)
                if (askLang) {
                    String fieldNameLang = fieldName + "_lang";
                    if (repeatable && i != fieldCount - 1) {
                        fieldCounter = '_' + Integer.toString(i + 1);
                    }
                    doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                } else {
                    sb.append("<td>&nbsp;</td>");
                } // put a remove button next to filled in values
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");

            } else if (repeatable && !readonly && i == fieldCount - 1) {
                // put language selection list if neccessary (for the dc lang attribute)
                if (askLang) {
                    String fieldNameLang = fieldName + "_lang";

                    if (repeatable && i != fieldCount - 1) {
                        fieldCounter = '_' + Integer.toString(i + 1);
                    }
                    doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                } else {
                    sb.append("<td>&nbsp;</td>");
                }
                // put a 'more' button next to the last space
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td>");
            } else {
                if (askLang) {
                    String fieldNameLang = fieldName + "_lang";
                    doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);
                } else {
                    sb.append("<td>&nbsp;</td>");
                }
            } // put language selection list if neccessary (for the dc lang attribute)
            sb.append("</tr>");
        }
        out.write(sb.toString());
    }
%>
<!--doTwoBox-->
<%!    void doTwoBox(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable, boolean readonly,
            int fieldCountIncr, String label, PageContext pageContext, String vocabulary, boolean closedVocabulary, int size, boolean askLang)
            throws java.io.IOException, DCInputsReaderException {
        String isize = size != 0 ? Integer.toString(size) : "15";
        String fieldCounter = "";

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);


        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        StringBuffer headers = new StringBuffer();

        String fieldParam = "";

        if (element.equals("relation") && qualifier.equals("ispartofseries")) {
            //Width hints used here to affect whole table
            headers.append("<tr><td width=\"40%\">&nbsp;</td>").append("<td class=\"submitFormDateLabel\" width=\"5%\">").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.seriesname")).append("</td>").append("<td class=\"submitFormDateLabel\" width=\"5%\">").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.paperno")).append("</td>").append("<td width=\"40%\">&nbsp;</td><td>&nbsp;</td>").append("</tr>");
            out.write(headers.toString());


        }

        if (fieldCount == 0) {
            fieldCount = 1;


        }

        for (int i = 0; i
                < fieldCount; i++) {
            if (i != fieldCount) {
                //param is field name and index, starting from 1 (e.g. myfield_2)
                fieldParam = fieldName + "_" + (i + 1);
            } else {
                //param is just the field name
                fieldParam = fieldName;
            }

            if (i < defaults.length) {
                sb.append("<td align=\"left\"><input type=\"text\" name=\"").append(fieldParam).append("\" size=\"" + isize + "\" value=\"").append(defaults[i].value.replaceAll("\"", "&quot;")).append("\"").append((hasVocabulary(vocabulary) && closedVocabulary) || readonly ? " disabled=\"disabled\" " : "").append("/>");


                if (!readonly) {
                    sb.append("&nbsp;<input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove2")).append("\"/>");
                }
                sb.append(doControlledVocabulary(fieldParam, pageContext, vocabulary, readonly)).append("</td>\n");


            } else {
                sb.append("<td align=\"left\"><input type=\"text\" name=\"").append(fieldParam).append("\" size=\"" + isize + "\"").append((hasVocabulary(vocabulary) && closedVocabulary) || readonly ? " disabled=\"disabled\" " : "").append("/>").append(doControlledVocabulary(fieldParam, pageContext, vocabulary, readonly)).append("</td>\n");


            }

            i++;



            if (i != fieldCount) {
                //param is field name and index, starting from 1 (e.g. myfield_2)
                fieldParam = fieldName + "_" + (i + 1);


            } else {
                //param is just the field name
                fieldParam = fieldName;


            }

            if (i < defaults.length) {
                sb.append("<td align=\"left\"><input type=\"text\" name=\"").append(fieldParam).append("\" size=\"" + isize + "\" value=\"").append(defaults[i].value.replaceAll("\"", "&quot;")).append("\"").append((hasVocabulary(vocabulary) && closedVocabulary) || readonly ? " disabled=\"disabled\" " : "").append("/>");


                if (!readonly) {
                    sb.append("&nbsp;<input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(i).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove2")).append("\"/>");
                }

                sb.append(doControlledVocabulary(fieldParam, pageContext, vocabulary, readonly)).append("</td></tr>\n");

            } else {
                sb.append("<td align=\"left\"><input type=\"text\" name=\"").append(fieldParam) //.append("\" size=\"15\"/></td>");
                        .append("\" size=\"" + isize + "\"").append((hasVocabulary(vocabulary) && closedVocabulary) || readonly ? " disabled=\"disabled\" " : "").append("/>").append(doControlledVocabulary(fieldParam, pageContext, vocabulary, readonly)).append("</td>\n");

                if (i + 1 >= fieldCount && !readonly) {
                    sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/></td>\n");

                } else {
                    sb.append("</td>");


                }
                sb.append("<td>&nbsp;</td>");


            } // put language selection list if neccessary (for the dc lang attribute)
            if (askLang) {
                String fieldNameLang = fieldName + "_lang";


                if (repeatable && i != fieldCount - 1) {
                    fieldCounter = '_' + Integer.toString(i + 1);


                }
                doLang(sb, item, fieldNameLang, fieldCounter, repeatable, 0);


            } else {
                sb.append("<td>&nbsp;</td>");


            }
        }
        sb.append("</tr>");
        out.write(sb.toString());


    }
%>
<!--doQualdropValue-->
<%!    void doQualdropValue(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, DCInputSetExt inputs, boolean repeatable,
            boolean readonly, int fieldCountIncr, List qualMap, String label, PageContext pageContext, int size, boolean askLang)
            throws java.io.IOException, DCInputsReaderException {
        String isize = size != 0 ? Integer.toString(size) : "34";

        DCValue[] unfiltered = item.getMetadata(schema, element, Item.ANY, Item.ANY);
        // filter out both unqualified and qualified values occuring elsewhere in inputs
        List<DCValue> filtered = new ArrayList<DCValue>();

        for (int i = 0; i
                < unfiltered.length; i++) {
            String unfilteredFieldName = unfiltered[i].element;

            if (unfiltered[i].qualifier != null && unfiltered[i].qualifier.length() > 0) {
                unfilteredFieldName += "." + unfiltered[i].qualifier;
            }

            if (!inputs.isFieldPresent(unfilteredFieldName)) {
                filtered.add(unfiltered[i]);
            }
        }
        DCValue[] defaults = filtered.toArray(new DCValue[0]);
        //DCValue[] defaults = item.getMetadata(element, Item.ANY, Item.ANY);
        int fieldCount = defaults.length + fieldCountIncr;
        StringBuffer sb = new StringBuffer();
        String q, v, currentQual, currentVal;

        if (fieldCount == 0) {
            fieldCount = 1;
        }

        for (int j = 0; j
                < fieldCount; j++) {

            if (j < defaults.length) {
                currentQual = defaults[j].qualifier;

                if (currentQual == null) {
                    currentQual = "";
                }
                currentVal = defaults[j].value;

            } else {
                currentQual = "";
                currentVal = "";
            } // do the dropdown box
            sb.append("<td colspan=\"2\"><select name=\"").append(fieldName).append("_qualifier");

            if (repeatable && j != fieldCount - 1) {
                sb.append("_").append(j + 1);
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            }
            sb.append("\">");

            for (int i = 0; i
                    < qualMap.size(); i += 2) {
                q = (String) qualMap.get(i);
                v = (String) qualMap.get(i + 1);
                sb.append("<option").append((v.equals(currentQual) ? " selected=\"selected\" " : "")).append(" value=\"").append(v).append("\">").append(q).append("</option>");
            } // do the input box
            sb.append("</select>&nbsp;<input type=\"text\" name=\"").append(fieldName).append("_value");

            if (repeatable && j != fieldCount - 1) {
                sb.append("_").append(j + 1);
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            } //size=\""+isize+"\"
            sb.append("\"  value=\"").append(currentVal.replaceAll("\"", "&quot;")).append("\"/></td>\n");

            if (repeatable && !readonly && j < defaults.length) {
                // put a remove button next to filled in values
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_remove_").append(j).append("\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.remove")).append("\"/> </td></tr>");
            } else if (repeatable && !readonly && j == fieldCount - 1) {
                // put a 'more' button next to the last space
                sb.append("<td><input type=\"submit\" name=\"submit_").append(fieldName).append("_add\" value=\"").append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.edit-metadata.button.add")).append("\"/> </td></tr>");
            } else {
                // put a blank if nothing else
                sb.append("<td>&nbsp;</td></tr>");
            }
        }
        out.write(sb.toString());
    }
%>
<!--doDropDown-->
<%!    void doDropDown(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable,
            boolean readonly, List valueList, String label, int size, boolean askLang)
            throws java.io.IOException, DCInputsReaderException {
        String isize = size != 0 ? Integer.toString(size) : "6";
        String fieldCounter = "";

        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);
        StringBuffer sb = new StringBuffer();
        Iterator vals;
        String display, value;
        int j;

        sb.append("<td colspan=\"2\">").append("<select name=\"").append(fieldName).append("\"");
        if (repeatable) {
            if (readonly) {
                sb.append(" disabled=\"disabled\"");
            }
        }
        sb.append(">");

        for (int i = 0; i
                < valueList.size(); i += 2) {
            display = (String) valueList.get(i);
            value = (String) valueList.get(i + 1);

            for (j = 0; j
                    < defaults.length; j++) {
                if (value.equals(defaults[j].value)) {
                    break;
                }
            }
            sb.append("<option ").append(j < defaults.length ? " selected=\"selected\" " : "").append("value=\"").append(value.replaceAll("\"", "&quot;")).append("\">").append(display).append("</option>");
        }
        sb.append("</select></td>");

        // put language selection list if neccessary (for the dc lang attribute)
        if (askLang) {
            String fieldNameLang = fieldName + "_lang";
            doLang(
                    sb, item, fieldNameLang, fieldCounter, false, 0);
        } else {
            sb.append("<td>&nbsp;</td>");
        }
        sb.append("</tr>");
        out.write(sb.toString());
    }
%>
<!--doChoiceSelect-->
<%!    void doChoiceSelect(javax.servlet.jsp.JspWriter out, PageContext pageContext, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable,
            boolean readonly, List valueList, String label, int collectionID, String contextPath)
            throws java.io.IOException {
        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);
        StringBuffer sb = new StringBuffer();

        sb.append("<td colspan=\"2\">").append("</td></tr>");
        out.write(sb.toString());
    }
%>
<!--doList-->
<%!    /**
     * Display Checkboxes or Radio buttons, depending on if repeatable! *
     */
    void doList(javax.servlet.jsp.JspWriter out, Item item,
            String fieldName, String schema, String element, String qualifier, boolean repeatable,
            boolean readonly, List valueList, String label, boolean askLang)
            throws java.io.IOException, DCInputsReaderException {
        DCValue[] defaults = item.getMetadata(schema, element, qualifier, Item.ANY);


        int valueCount = valueList.size();

        StringBuffer sb = new StringBuffer();
        String display, value;
        int j;
        int numColumns = 1;
        //if more than 3 display+value pairs, display in 2 columns to save space
        if (valueCount > 6) {
            numColumns = 2;
        }

        if (numColumns > 1) {
            sb.append("<td valign=\"top\">");
        } else {
            sb.append("<td valign=\"top\" colspan=\"3\">");
        } //flag that lets us know when we are in Column2
        boolean inColumn2 = false;

        //loop through all values
        for (int i = 0; i
                < valueList.size(); i += 2) {
            //get display value and actual value
            display = (String) valueList.get(i);
            value = (String) valueList.get(i + 1);

            //check if this value has been selected previously
            for (j = 0; j
                    < defaults.length; j++) {
                if (value.equals(defaults[j].value)) {
                    break;
                }
            }
            // print input field
            sb.append("<input type=\"");

            //if repeatable, print a Checkbox, otherwise print Radio buttons
            if (repeatable) {
                sb.append("checkbox");
            } else {
                sb.append("radio");
            }
            if (readonly) {
                sb.append("\" disabled=\"disabled");
            }
            sb.append("\" name=\"").append(fieldName).append("\"").append(j < defaults.length ? " checked=\"checked\" " : "").append(" value=\"").append(value.replaceAll("\"", "&quot;")).append("\">");

            //print display name immediately after input
            sb.append("&nbsp;").append(display).append("<br/>");

            // if we are writing values in two columns,
            // then start column 2 after half of the values
            if ((numColumns == 2) && (i + 2 >= (valueList.size() / 2)) && !inColumn2) {
                //end first column, start second column
                sb.append("</td>");
                sb.append("<td colspan=\"2\" valign=\"top\">");
                inColumn2 = true;
            }
        }//end for each value
        sb.append("</td>");

        // put language selection list if neccessary (for the dc lang attribute)
        if (askLang) {
            doLang(sb, item, fieldName, "", false, 0);
        }
        sb.append("</tr>");
        out.write(sb.toString());
    }//end doList

%>

<%
    Item item = (Item) request.getAttribute("item");
    String handle = (String) request.getAttribute("handle");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    MetadataField[] dcTypes = (MetadataField[]) request.getAttribute("dc.types");
    HashMap metadataFields = (HashMap) request.getAttribute("metadataFields");
    request.setAttribute("LanguageSwitch", "hide");

    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    // Is the logged in user an admin of the item
    Boolean itemAdmin = (Boolean) request.getAttribute("admin_button");
    boolean isItemAdmin = (itemAdmin == null ? false : itemAdmin.booleanValue());

    Boolean policy = (Boolean) request.getAttribute("policy_button");
    boolean bPolicy = (policy == null ? false : policy.booleanValue());

    Boolean delete = (Boolean) request.getAttribute("delete_button");
    boolean bDelete = (delete == null ? false : delete.booleanValue());

    Boolean createBits = (Boolean) request.getAttribute("create_bitstream_button");
    boolean bCreateBits = (createBits == null ? false : createBits.booleanValue());

    Boolean removeBits = (Boolean) request.getAttribute("remove_bitstream_button");
    boolean bRemoveBits = (removeBits == null ? false : removeBits.booleanValue());

    Boolean ccLicense = (Boolean) request.getAttribute("cclicense_button");
    boolean bccLicense = (ccLicense == null ? false : ccLicense.booleanValue());

    Boolean withdraw = (Boolean) request.getAttribute("withdraw_button");
    boolean bWithdraw = (withdraw == null ? false : withdraw.booleanValue());

    Boolean reinstate = (Boolean) request.getAttribute("reinstate_button");
    boolean bReinstate = (reinstate == null ? false : reinstate.booleanValue());

    // owning Collection ID for choice authority calls
    int collectionID = -1;
    if (collections.length > 0) {
        collectionID = collections[0].getID();
    }

    List<String> tobeAdded = new ArrayList<String>();
    List<DCValue> allItemsFields = Arrays.asList(item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY));
%>

<dspace:layout titlekey="jsp.tools.edit-item-form.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">


    <script type="text/javascript">
        function updatedoctype()
        {
            document.change_doctype.submit();
        }
    </script>

    <%-- <h1>Edit Item</h1> --%>
    <h1><fmt:message key="jsp.tools.edit-item-form.title"/></h1>

    <%-- <p><strong>PLEASE NOTE: These changes are not validated in any way.
    You are responsible for entering the data in the correct format.
    If you are not sure what the format is, please do NOT make changes.</strong></p> --%>
    <%-- <p><strong><fmt:message key="jsp.tools.edit-item-form.note"/></strong></p> --%>

    <%-- <p><dspace:popup page="/help/collection-admin.html#editmetadata">More help...</dspace:popup></p>  --%>
    <div><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, 
        \"help.collection-admin\") + \"#editmetadata\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>

        <div class="metadataForm">
            <center>
                <table width="90%" summary="Edit item table">
                    <tr>
                    <%-- <td class="submitFormLabel">Item&nbsp;internal&nbsp;ID:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.itemID"/></td>
                    <td class="standard"><%= item.getID()%></td>
                    <td class="standard" width="100%" align="right" rowspan="5">
                        <%
                            if (!item.isWithdrawn() && bWithdraw) {
                        %>
                        <form method="post" action="<%= request.getContextPath()%>/tools/edit-item">
                            <input type="hidden" name="item_id" value="<%= item.getID()%>" />
                            <input type="hidden" name="action" value="<%= EditItemServlet.START_WITHDRAW%>" />
                            <%-- <input type="submit" name="submit" value="Withdraw..."> --%>
                            <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.withdraw-w-confirm.button"/>"/>
                        </form>
                        <%
                        } else if (item.isWithdrawn() && bReinstate) {
                        %>
                        <form method="post" action="<%= request.getContextPath()%>/tools/edit-item">
                            <input type="hidden" name="item_id" value="<%= item.getID()%>" />
                            <input type="hidden" name="action" value="<%= EditItemServlet.REINSTATE%>" />
                            <%-- <input type="submit" name="submit" value="Reinstate"> --%>
                            <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.reinstate.button"/>"/>
                        </form>
                        <%
                            }
                        %>

                        <br/>
                        <%
                            if (bDelete) {
                        %>
                        <form method="post" action="<%= request.getContextPath()%>/tools/edit-item">
                            <input type="hidden" name="item_id" value="<%= item.getID()%>" />
                            <input type="hidden" name="action" value="<%= EditItemServlet.START_DELETE%>" />
                            <%-- <input type="submit" name="submit" value="Delete (Expunge)..."> --%>
                            <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.delete-w-confirm.button"/>"/>
                        </form>
                        <%
                            }

                            if (isItemAdmin) {
                        %>                     <form method="post" action="<%= request.getContextPath()%>/tools/edit-item">
                            <input type="hidden" name="item_id" value="<%= item.getID()%>" />
                            <input type="hidden" name="action" value="<%= EditItemServlet.START_MOVE_ITEM%>" />
                            <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.move-item.button"/>"/>
                        </form>
                        <%
                            }
                        %>
                    </td>
                </tr>
                <tr>
                    <%-- <td class="submitFormLabel">Handle:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.handle"/></td>
                    <td class="standard"><%= (handle == null ? "None" : handle)%></td>
                </tr>
                <tr>
                    <%-- <td class="submitFormLabel">Last modified:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.modified"/></td>
                    <td class="standard"><dspace:date date="<%= new DCDate(item.getLastModified())%>" /></td>
                </tr>
                <tr>
                    <%-- <td class="submitFormLabel">In Collections:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.collections"/></td>
                    <td class="standard">
                        <%  for (int i = 0; i < collections.length; i++) {%>
                        <%= collections[i].getMetadata("name")%><br/>
                        <%  }%>
                    </td>
                </tr>

                <tr>
                    <%-- <td class="submitFormLabel">Item page:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.itempage"/></td>
                    <td class="standard">
                        <%  if (handle == null) {%>
                        <em><fmt:message key="jsp.tools.edit-item-form.na"/></em>
                        <%  } else {
                                      String url = ConfigurationManager.getProperty("dspace.url") + "/handle/" + handle;%>
                        <a target="_blank" href="<%= url%>"><%= url%></a>
                        <%  }%>
                    </td>
                </tr>
                <%
                    if (bPolicy) {
                %>
                <%-- ===========================================================
                     Edit item's policies
                     =========================================================== --%>
                <tr>
                    <%-- <td class="submitFormLabel">Item's Authorizations:</td> --%>
                    <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.item"/></td>
                    <td>
                        <form method="post" action="<%= request.getContextPath()%>/tools/authorize">
                            <input type="hidden" name="handle" value="<%= ConfigurationManager.getProperty("handle.prefix")%>" />
                            <input type="hidden" name="item_id" value="<%= item.getID()%>" />
                            <%-- <input type="submit" name="submit_item_select" value="Edit..."> --%>
                            <input type="submit" name="submit_item_select" value="<fmt:message key="jsp.tools.general.edit"/>"/>
                        </form>
                    </td>
                </tr>
                <%
                    }
                %>
            </table>
        </center>

        <%

            if (item.isWithdrawn()) {
        %>
        <%-- <p align="center"><strong>This item was withdrawn from DSpace</strong></p> --%>
        <p align="center"><strong><fmt:message key="jsp.tools.edit-item-form.msg"/></strong></p>
        <%    }
        %>
        <p>&nbsp;</p>




        <%
            String doctype;
            DCValue[] values = item.getMetadata("dc", "type", null, Item.ANY);

            if (values.length > 0) {
                doctype = values[0].value; // item already has a type

                DCInputsReaderExt inputsReader = new DCInputsReaderExt();
                String collectionHandle = "";
                if (collections.length > 0) {
                    collectionHandle = collections[0].getHandle();
                }

                DCInputSetExt inputSet = inputsReader.getInputs(collectionHandle, doctype);

        %>

        <form id="change_doctype" name="change_doctype" method="post" action="<%= request.getContextPath()%>/tools/edit-item">
            <div class="metadataFieldgroup"> <!-- doc type selector -->
                <div class="metadataTitlegroup">
                    <h3 class="metadataFieldgroupTitle">
                        <fmt:message key="jsp.submit.edit-metadata.doctype" />
                    </h3>
                    <p class="metadataFieldgroupHint"></p>
                </div>
                <div class="metadataFieldrow">
                    <div class="metadataField">
                        <div class="metadataLabel"></div>
                        <table>
                            <tr>
                                <td>&nbsp;</td>
                                <td colspan="2">
                                    <div class="metadataType">
                                        <!-- selection of submission document type -->
                                        <select name="select_doctype" id="select_doctype" onchange="updatedoctype()">
                                            <%
                                                List<String> types = new ArrayList<String>();
                                                if (collections.length > 0) {
                                                    types = inputsReader.getTypesListforCollection(collections[0].getHandle());
                                                }
                                                StringBuffer sb = new StringBuffer();
                                                String isselected = "";
                                                if (!types.isEmpty()) {
                                                    for (int i = 0; i < types.size(); i++) {
                                                        isselected = types.get(i).equals(doctype) ? "selected=\"selected\"" : "";
                                                        sb.append("<option value=\"" + types.get(i) + "\"" + isselected + ">" + types.get(i) + "</option>");
                                                    }
                                                    out.println(sb);
                                                }
                                            %>
                                        </select>
                                    </div>
                                </td>
                                <td>&nbsp;</td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div> <!-- doc type selector -->
            <input type="hidden" name="item_id" value="<%= item.getID()%>" />
            <input type="hidden" name="action" value="<%= EditItemServlet.CHANGE_DOCTYPE%>" />
        </form>

        <form id="edit_metadata" name="edit_metadata" method="post" action="<%= request.getContextPath()%>/tools/edit-item">
            <%


                /*
                 * we need to add after all non empty fields (as
                 * simple input box) which are not in inputset
                 */

                String tempField = "";

                for (DCValue dcv : allItemsFields) {
                    if ((dcv.value != null) && (!"".equals(dcv.value))) {
                        tempField = dcv.schema + "." + dcv.element + (dcv.qualifier == null || "".equals(dcv.qualifier) ? "" : ("." + dcv.qualifier));
                        if (!tobeAdded.contains(tempField) && (!tempField.equals("dc.type"))) {
                            tobeAdded.add(tempField);
                        }
                    }
                }

                for (List<DCInputGroup> iGroups : inputSet.getPages().values()) {

                    String gLabel = null;
                    String gHint = null;
                    DCInputGroup iG = null;

                    for (int i = 0; i < iGroups.size(); i++) {
                        iG = iGroups.get(i);
                        gLabel = iG.getLabel();
                        gHint = iG.getHint();
            %>
            <div class="metadataFieldgroup">
                <div class="metadataTitlegroup">
                    <h3 class="metadataFieldgroupTitle">
                        <%out.print(gLabel);%>
                    </h3>
                    <p class="metadataFieldgroupHint">
                        <%out.print("- " + gHint);%>
                    </p>
                </div>
                <!-- fields rows -->
                <%
                    for (int g = 0; g < iG.getRowsCount(); g++) {
                %>
                <div class="metadataFieldrow">
                    <%
                        List<DCInput> iRow = iG.getRow(g);
                        for (int j = 0; j < iRow.size(); j++) {
                            DCInput iF = iRow.get(j);
                    %>
                    <div class="metadataField">
                        <div class="metadataLabel"><%=iF.getLabel()%></div>
                        <table>
                            <%
                                String dcElement = iF.getElement();
                                String dcQualifier = iF.getQualifier();
                                String dcSchema = iF.getSchema();
                                String fieldName;

                                int inputsize = iF.getSize();
                                boolean repeatable = iF.getRepeatable();
                                String vocabulary = iF.getVocabulary();
                                boolean askLang = iF.getAskLanguage();
                                boolean readonly = false;

                                if (dcQualifier != null && !dcQualifier.equals("*")) {
                                    fieldName = dcSchema + "_" + dcElement + '_' + dcQualifier;

                                } else {
                                    fieldName = dcSchema + "_" + dcElement;
                                }

                                if (tobeAdded.contains(iF.getFullQualName())) {
                                    tobeAdded.remove(iF.getFullQualName());
                                }

                                int fieldCountIncr = 0;

                                if (repeatable && !readonly) {
                                    //if(request.getAttribute("moreInputs") != null)
                                    fieldCountIncr = 1;
                                }

                                String inputType = iF.getInputType();
                                String label = iF.getLabel();

                                boolean closedVocabulary = iF.isClosedVocabulary();
                                out.write("<tr>");

                                if (iF.isAuthority()) {
                                    doAuthorityField(out, item, fieldName, iF, fieldCountIncr, readonly, pageContext, request.getContextPath());

                                } else if (inputType.equals("name")) {
                                    doPersonalName(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, fieldCountIncr, label, pageContext, collectionID, inputsize, askLang, request.getContextPath());

                                } else if (isSelectable(fieldName)) {
                                    doChoiceSelect(out, pageContext, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, iF.getPairs(), label, collectionID, request.getContextPath());

                                } else if (inputType.equals("date")) {
                                    doDate(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, fieldCountIncr, label, pageContext, request);

                                } else if (inputType.equals("series")) {
                                    doSeriesNumber(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, fieldCountIncr, label, pageContext, inputsize);

                                } else if (inputType.equals("qualdrop_value")) {
                                    doQualdropValue(out, item, fieldName, dcSchema, dcElement, inputSet, repeatable,
                                            readonly, fieldCountIncr, iF.getPairs(), label, pageContext, inputsize, askLang);

                                } else if (inputType.equals("textarea")) {
                                    doTextArea(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, fieldCountIncr, label, pageContext, vocabulary,
                                            closedVocabulary, collectionID, inputsize, askLang, request.getContextPath());

                                } else if (inputType.equals("dropdown")) {
                                    doDropDown(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, iF.getPairs(), label, inputsize, askLang);

                                } else if (inputType.equals("twobox")) {
                                    doTwoBox(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, fieldCountIncr, label, pageContext, vocabulary,
                                            closedVocabulary, inputsize, askLang);

                                } else if (inputType.equals("list")) {
                                    doList(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, iF.getPairs(), label, askLang);

                                } else {
                                    doOneBox(out, item, fieldName, dcSchema, dcElement, dcQualifier,
                                            repeatable, readonly, fieldCountIncr, label, pageContext, vocabulary,
                                            closedVocabulary, collectionID, inputsize, askLang, request.getContextPath());
                                }
                                if (hasVocabulary(vocabulary) && !readonly) {
                            %>

                            <tr>
                                <td>&nbsp;</td>
                                <td colspan="3" class="submitFormHelpControlledVocabularies">
                                    <dspace:popup page="/help/index.html#controlledvocabulary"><fmt:message key="jsp.controlledvocabulary.controlledvocabulary.help-link"/></dspace:popup>
                                    </td>
                                </tr>

                            <%                                } //hasVocabulary
%>
                        </table>
                    </div> <!-- field -->
                    <%
                                  } //row FOR loop
%>
                </div> <!-- row -->
                <% }%><!-- end fields rows -->
            </div> <!--metadataFieldGroup-->
            <%
                        } // field group for loop
                    }// pages loop
                } //if doctype is not empty
            %>

            <div class="metadataFieldgroup">
                <div class="metadataTitlegroup">
                    <h3 class="metadataFieldgroupTitle">
                        <fmt:message key="jsp.edit-mode.others-title"/>
                    </h3>
                    <p class="metadataFieldgroupHint">
                        <fmt:message key="jsp.edit-mode.others-hint"/>
                    </p>
                </div>
                <!-- fields rows -->
                <%
                    String dc = "";
                    String parm = "", parmVal = "", parmLang = "";
                    for (int j = 0; j < tobeAdded.size(); j++) {
                        dc = tobeAdded.get(j);
                        DCValue dcva[] = item.getMetadata(dc);
                        for (int i = 0; i < dcva.length; i++) {
                            if ((dcva[i].value != null) && (!"".equals(dcva[i].value))) {
                                parm = "other_" + i + "_" + dc;
                                parmVal = dcva[i].value;
                                parmLang = dcva[i].language;
                %>
                <div class="metadataFieldrow">
                    <div class="metadataField">
                        <div class="metadataLabel"><%=dc%></div>
                        <table>
                            <tr>
                                <td><input type="text" name="<%=parm%>" value="<%=parmVal%>" size="50"/></td>
                            </tr>
                        </table>
                    </div> <!-- field -->
                    <div class="metadataField">
                        <div class="metadataLabel"><fmt:message key="jsp.edit-mode.others-lang"/></div>
                        <table>
                            <tr>
                                <td><input type="text" name="<%=parm + "_lang"%>" value="<%=parmLang%>" size="5"/></td>
                            </tr>
                        </table>
                    </div>
                </div> <!-- field row-->
                <%
                            }
                        }
                    }
                %>
            </div> <!-- group -->

            <p>&nbsp;</p>

            <div class="metadataFieldgroup">
                <div class="metadataTitlegroup">
                    <h3 class="metadataFieldgroupTitle">
                        <fmt:message key="jsp.edit-mode.add-dc-title"/>
                    </h3>
                    <p class="metadataFieldgroupHint">
                        <fmt:message key="jsp.edit-mode.add-dc-hint"/>
                    </p>
                </div>
                <div class="metadataFieldrow">
                    <div class="metadataField">
                        <!-- <div class="metadataLabel"></div> -->
                        <table>
                            <tr>
                                <td headers="t1" colspan="3">
                                    <select name="addfield_dctype">
                                        <%  for (int i = 0; i < dcTypes.length; i++) {
                                                Integer fieldID = new Integer(dcTypes[i].getFieldID());
                                                String displayName = (String) metadataFields.get(fieldID);
                                        %>
                                        <option value="<%= fieldID.intValue()%>"><%= displayName%></option>
                                        <%  }%>
                                    </select>
                                </td>
                                <td headers="t3">
                                    <textarea name="addfield_value" rows="2" cols="30"></textarea>
                                </td>
                                <td headers="t4">
                                    <input type="text" name="addfield_lang" size="5"/>
                                </td>
                                <td headers="t5">
                                    <%-- <input type="submit" name="submit_addfield" value="Add"> --%>
                                    <input type="submit" name="submit_addfield" value="<fmt:message key="jsp.tools.general.add"/>"/>
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>

            <p>&nbsp;</p>

            <div class="metadataFieldgroup">
                <div class="metadataTitlegroup">
                    <h3 class="metadataFieldgroupTitle">
                        <fmt:message key="jsp.edit-mode.bitstream-title"/>
                    </h3>
                    <p class="metadataFieldgroupHint">
                        <fmt:message key="jsp.tools.edit-item-form.note3"/>
                    </p>
                </div>


                <%
                    Bundle[] bundles = item.getBundles();

                    for (int i = 0; i < bundles.length; i++) {
                        Bitstream[] bitstreams = bundles[i].getBitstreams();
                        for (int j = 0; j < bitstreams.length; j++) {
                            // Parameter names will include the bundle and bitstream ID
                            // e.g. "bitstream_14_18_desc" is the description of bitstream 18 in bundle 14
                            String key = bundles[i].getID() + "_" + bitstreams[j].getID();
                            BitstreamFormat bf = bitstreams[j].getFormat();
                %>
                <div class="metadataFieldgroup">
                    <div class="metadataFieldrow">

                        <div class="metadataField">
                            <% if (bundles[i].getName().equals("ORIGINAL")) {%>
                            <div class="metadataField">
                                <input type="radio" name="<%= bundles[i].getID()%>_primary_bitstream_id" value="<%= bitstreams[j].getID()%>"
                                       <% if (bundles[i].getPrimaryBitstreamID() == bitstreams[j].getID()) {%>
                                       checked="<%="checked"%>"
                                       <% }%> />
                            </div>
                            <div class="editBitstreamTH"><fmt:message key="jsp.tools.edit-item-form.elem5"/></div>
                            <% }%>
                        </div>

                        <div class="metadataField">
                            <div class="editBitstreamTH"><fmt:message key="jsp.tools.edit-item-form.elem7"/></div>
                            <div class="metadataField">
                                <input type="text" name="bitstream_name_<%= key%>" value="<%= (bitstreams[j].getName() == null ? "" : Utils.addEntities(bitstreams[j].getName()))%>"/>
                            </div>
                            <div class="editBitstreamTH"><fmt:message key="jsp.tools.edit-item-form.elem8"/></div>
                            <div class="metadataField">
                                <input type="text" name="bitstream_source_<%= key%>" value="<%= (bitstreams[j].getSource() == null ? "" : bitstreams[j].getSource())%>" size="25"/>
                            </div>

                        </div>

                        <div class="metadataField">
                            <div class="editBitstreamTH"><fmt:message key="jsp.tools.edit-item-form.elem9"/></div>
                            <div class="metadataField">
                                <input type="text" name="bitstream_description_<%= key%>" value="<%= (bitstreams[j].getDescription() == null ? "" : Utils.addEntities(bitstreams[j].getDescription()))%>" size="64"/>
                            </div>
                        </div>

                        <div class="metadataField">
                            <div class="editBitstreamTH"><fmt:message key="jsp.tools.edit-item-form.elem10"/></div>
                            <div class="metadataField">
                                <input type="text" name="bitstream_format_id_<%= key%>" value="<%= bf.getID()%>" size="4"/> (<%= Utils.addEntities(bf.getShortDescription())%>)
                            </div>


                            <div class="editBitstreamTH"><fmt:message key="jsp.tools.edit-item-form.elem11"/></div>
                            <div class="metadataField">
                                <input type="text" name="bitstream_user_format_description_<%= key%>" value="<%= (bitstreams[j].getUserFormatDescription() == null ? "" : Utils.addEntities(bitstreams[j].getUserFormatDescription()))%>"/>
                            </div>
                        </div>
                        <div class="metadataField">
                            <%-- <a target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bitstreams[j].getID() %>">View</a>&nbsp;<input type="submit" name="submit_delete_bitstream_<%= key %>" value="Remove"> --%>
                            <a target="_blank" href="<%= request.getContextPath()%>/retrieve/<%= bitstreams[j].getID()%>"><fmt:message key="jsp.tools.general.view"/></a>&nbsp;
                            <% if (bRemoveBits) {%>
                            <input type="submit" name="submit_delete_bitstream_<%= key%>" value="<fmt:message key="jsp.tools.general.remove"/>" />
                            <% }%>
                        </div>
                    </div>
                </div>
                <%
                        }
                    }
                %>

                <div class="metadataFieldgroup">
                    <table>
                        <tr>
                            <td>
                                <%
                                    if (bCreateBits) {
                                %>
                                <input type="submit" name="submit_addbitstream" value="<fmt:message key="jsp.tools.edit-item-form.addbit.button"/>"/>
                                <%  }

                                    if (ConfigurationManager.getBooleanProperty("webui.submit.enable-cc") && bccLicense) {
                                        String s;
                                        Bundle[] ccBundle = item.getBundles("CC-LICENSE");
                                        s = ccBundle.length > 0 ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.replacecc.button") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.addcc.button");
                                %>
                                <input type="submit" name="submit_addcc" value="<%= s%>" />
                                <input type="hidden" name="handle" value="<%= ConfigurationManager.getProperty("handle.prefix")%>"/>
                                <input type="hidden" name="item_id" value="<%= item.getID()%>"/>
                                <%
                                    }
                                %>
                            </td>
                        </tr>
                    </table>

                </div>
            </div>
            <div>&nbsp;</div>
            <input type="hidden" name="item_id" value="<%= item.getID()%>"/>
            <input type="hidden" name="action" value="<%= EditItemServlet.UPDATE_ITEM%>"/>
            <center>
                <table width="70%">
                    <tr>
                        <td align="left">
                            <%-- <input type="submit" name="submit" value="Update" /> --%>
                            <input type="submit" name="submit" value="<fmt:message key="jsp.tools.general.update"/>" />
                        </td>
                        <td align="right">

                            <%-- <input type="submit" name="submit_cancel" value="Cancel" /> --%>
                            <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
                        </td>
                    </tr>
                </table>
            </center>

    </div> <!--  MetadataForm -->
</form>
</dspace:layout>
