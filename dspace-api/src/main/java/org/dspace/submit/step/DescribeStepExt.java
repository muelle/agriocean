/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;
import proj.oceandocs.submission.DCInputSetExt;
import proj.oceandocs.submission.DCInputsReaderExt;

/**
 * Describe step for DSpace submission process. Handles the gathering of
 * descriptive information (i.e. metadata) for an item being submitted into
 * DSpace. <P> This class performs all the behind-the-scenes processing that
 * this particular step requires. This class's methods are utilized by both the
 * JSP-UI and the Manakin XML-UI <P>
 *
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 *
 * @author Tim Donohue
 * @version $Revision: 5844 $
 */
public class DescribeStepExt extends AbstractProcessingStep {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(DescribeStep.class);
    /**
     * hash of all submission forms details
     */
    private static DCInputsReaderExt inputsReader = null;
    /**
     * *************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     *
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the JSPStepManager class)
     * ************************************************************************
     */
    // user requested an extra input field to be displayed
    public static final int STATUS_MORE_INPUT_REQUESTED = 1;
    // there were required fields that were not filled out
    public static final int STATUS_MISSING_REQUIRED_FIELDS = 2;
    // the metadata language qualifier
    public static final String LANGUAGE_QUALIFIER = getDefaultLanguageQualifier();
    public static final String DOCTYPE_BUTTON = "select_doctype";

    /**
     * Constructor
     */
    public DescribeStepExt() throws ServletException {
        //load the DCInputsReader
        getInputsReader();
    }

    /**
     * Do any processing of the information input by the user, and/or perform
     * step processing (if no user interaction required) <P> It is this method's
     * job to save any data to the underlying database, as necessary, and return
     * error messages (if any) which can then be processed by the appropriate
     * user interface (JSP-UI or XML-UI) <P> NOTE: If this step is a
     * non-interactive step (i.e. requires no UI), then it should perform *all*
     * of its processing in this method!
     *
     * @param context current DSpace context
     * @param request current servlet request object
     * @param response current servlet response object
     * @param subInfo submission info object
     * @return Status or error flag which will be processed by
     * doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned, no errors
     * occurred!)
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException {
        boolean newDocType = false;
        // check what submit button was pressed in User Interface
        String buttonPressed = Util.getSubmitButton(request, DOCTYPE_BUTTON);
        Collection c = subInfo.getSubmissionItem().getCollection();
        // get the item and current page
        Item item = subInfo.getSubmissionItem().getItem();
        int currentPage = getCurrentPage(request);
        String doctype = request.getParameter(DOCTYPE_BUTTON);
        String formerDocType;


        if (DOCTYPE_BUTTON.equals(buttonPressed)) {
            newDocType = true;
        }

        if (doctype == null) {
            doctype = getCurrentDocType(item, c);
            // the document type did not change
            formerDocType = doctype;
        } else {
            // the document type has changed. Save former document type.
            formerDocType = getCurrentDocType(item, c); // the type returned is the type stored in the DB, i.e., the type before it changed
            item.clearMetadata("dc", "type", null, Item.ANY);
            item.addMetadata("dc", "type", null, Item.ANY, doctype); // set the new doctype
            item.update();
        }
        // lookup applicable inputs
        List<DCInput> inputs = null;
        try {
            // the inputs on the current page of the FORMER document type
            // (possibly the same as the current one) must be updated!
            DCInputSetExt inset = inputsReader.getInputs(c.getHandle(), formerDocType);
            if (inset != null) {
                inputs = inset.getPageInputs(currentPage);
            } else {
                DCInputsReaderException e = new DCInputsReaderException("Couldn't get input fields for specified type of document: " + doctype
                        + " and collection " + c.getName() + " (handle = " + c.getHandle() + ")");
                log.error(e.getLocalizedMessage());
                throw new ServletException(e);
            }
        } catch (DCInputsReaderException e) {
            throw new ServletException(e);
        }

        // Step 1:
        // clear out all item metadata defined on this page
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                    : DCInput.SUBMISSION_SCOPE)) {
                continue;
            }
            String qualifier = inputs.get(i).getQualifier();
            if (qualifier == null
                    && inputs.get(i).getInputType().equals("qualdrop_value")) {
                qualifier = Item.ANY;
            }
            item.clearMetadata(inputs.get(i).getSchema(), inputs.get(i).getElement(),
                    qualifier, Item.ANY);
        }

        // Clear required-field errors first since missing authority
        // values can add them too.
        clearErrorFields(request);

        // Step 2:
        // now update the item metadata.
        String fieldName;
        boolean moreInput = false;
        for (int j = 0; j < inputs.size(); j++) {
            if (!inputs.get(j).isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                    : DCInput.SUBMISSION_SCOPE)) {
                continue;
            }
            String element = inputs.get(j).getElement();
            String qualifier = inputs.get(j).getQualifier();
            String schema = inputs.get(j).getSchema();
            boolean isAuthorityControlled = inputs.get(j).isAuthority();
            
            if (qualifier != null && !qualifier.equals(Item.ANY)) {
                fieldName = schema + "_" + element + '_' + qualifier;
            } else {
                fieldName = schema + "_" + element;
            }

            String language_qual = request.getParameter(fieldName + "_lang");

            String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
            ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
            String inputType = inputs.get(j).getInputType();
            if (inputType.equals("name")) {
                readNames(request, item, schema, element, qualifier, inputs.get(j).getRepeatable(), isAuthorityControlled);
            } else if (inputType.equals("date")) {
                readDate(request, item, schema, element, qualifier);
            } // choice-controlled input with "select" presentation type is
            // always rendered as a dropdown menu
            else if (inputType.equals("dropdown") || inputType.equals("list")
                    || (cmgr.isChoicesConfigured(fieldKey)
                    && "select".equals(cmgr.getPresentation(fieldKey)))) {
                String[] vals = request.getParameterValues(fieldName);
                if (vals != null) {
                    for (int z = 0; z < vals.length; z++) {
                        if (!vals[z].equals("")) {
                            item.addMetadata(schema, element, qualifier, language_qual == null ? LANGUAGE_QUALIFIER : language_qual,
                                    vals[z]);
                        }
                    }
                }
            } else if (inputType.equals("series")) {
                readSeriesNumbers(request, item, schema, element, qualifier,
                        inputs.get(j).getRepeatable());
            } else if (inputType.equals("qualdrop_value")) {
                List<String> quals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_qualifier");
                List<String> vals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_value");
                for (int z = 0; z < vals.size(); z++) {
                    String thisQual = quals.get(z);
                    if ("".equals(thisQual)) {
                        thisQual = null;
                    }
                    String thisVal = vals.get(z);
                    if (!buttonPressed.equals("submit_" + schema + "_"
                            + element + "_remove_" + z)
                            && !thisVal.equals("")) {
                        item.addMetadata(schema, element, thisQual, null,
                                thisVal);
                    }
                }
            } else if ((inputType.equals("onebox"))
                    || (inputType.equals("twobox"))
                    || (inputType.equals("textarea"))) {
                readText(request, item, schema, element, qualifier, inputs.get(j).getRepeatable(), language_qual == null ? LANGUAGE_QUALIFIER : language_qual, isAuthorityControlled);
            } else {
                throw new ServletException("Field " + fieldName
                        + " has an unknown input type: " + inputType);
            }

            // determine if more input fields were requested
            if (!moreInput
                    && buttonPressed.equals("submit_" + fieldName + "_add")) {
                subInfo.setMoreBoxesFor(fieldName);
                subInfo.setJumpToField(fieldName);
                moreInput = true;
            } // was XMLUI's "remove" button pushed?
            else if (buttonPressed.equals("submit_" + fieldName + "_delete")) {
                subInfo.setJumpToField(fieldName);
            }
        }

        // Step 3:
        // Check to see if any fields are missing
        // Only check for required fields if user clicked the "next", the "previous" or the "progress bar" button
        if (buttonPressed.equals(NEXT_BUTTON)
                || buttonPressed.startsWith(PROGRESS_BAR_PREFIX)
                || buttonPressed.equals(PREVIOUS_BUTTON)
                || buttonPressed.equals(CANCEL_BUTTON)) {
            for (int i = 0; i < inputs.size(); i++) {
                DCValue[] values = item.getMetadata(inputs.get(i).getSchema(),
                        inputs.get(i).getElement(), inputs.get(i).getQualifier(), Item.ANY);

                if (inputs.get(i).isRequired() && values.length == 0) {
                    // since this field is missing add to list of error fields
                    addErrorField(request, getFieldName(inputs.get(i)));
                }
            }
        }

        // Step 4:
        // Save changes to database
        subInfo.getSubmissionItem().update();

        // commit changes
        context.commit();

        // check for request for more input fields, first
        if (moreInput) {
            return STATUS_MORE_INPUT_REQUESTED;
        } // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0) {
            return STATUS_MISSING_REQUIRED_FIELDS;
        } else if (newDocType) {
            return NEW_DOC_TYPE;
        }

        // completed without errors
        return STATUS_COMPLETE;
    }

    /**
     * Gets the document type (value for dc.type) for the given item, or a
     * default one if type absent
     *
     * @param item the item whose type to return
     * @param c the collection the item belongs to
     * @return the document type of the item, or a default one for the given
     * collection
     */
    private String getCurrentDocType(Item item, Collection c) {
        String doctype = null;
        DCValue[] itemsTypes = item.getMetadata("dc", "type", null, Item.ANY);
        if (itemsTypes.length > 0) {
            doctype = itemsTypes[0].value;
        } else {
            List<String> types = inputsReader.getTypesListforCollection(c.getHandle());
            if (types.size() > 0) {
                if (!types.contains(doctype)) {
                    doctype = types.get(0);
                }
            } else {
                doctype = "";
            }
        }
        return doctype;
    }

    /**
     * Retrieves the number of pages that this "step" extends over. This method
     * is used to build the progress bar. <P> This method may just return 1 for
     * most steps (since most steps consist of a single page). But, it should
     * return a number greater than 1 for any "step" which spans across a number
     * of HTML pages. For example, the configurable "Describe" step (configured
     * using input-forms.xml) overrides this method to return the number of
     * pages that are defined by its configuration file. <P> Steps which are
     * non-interactive (i.e. they do not display an interface to the user)
     * should return a value of 1, so that they are only processed once!
     *
     * @param request The HTTP Request
     * @param subInfo The current submission information object
     *
     * @return the number of pages in this step
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException {
        // by default, use the "default" collection handle
        String collectionHandle = DCInputsReaderExt.DEFAULT_COLLECTION;

        if (subInfo.getSubmissionItem() != null) {
            collectionHandle = subInfo.getSubmissionItem().getCollection().getHandle();
        }

        // get number of input pages (i.e. "Describe" pages)
        try {
            String documentType = (String) request.getAttribute("submission.doctype");

            if (documentType == null) {
                documentType = request.getParameter(DOCTYPE_BUTTON);

                if (documentType == null) {
                    List documentsList = inputsReader.getTypesListforCollection(collectionHandle);
                    if (documentsList.size() > 0) {
                        documentType = (String) documentsList.get(0);
                    }
                }
            }
            return inputsReader.getNumberInputPages(collectionHandle, documentType);
        } catch (DCInputsReaderException e) {
            throw new ServletException(e);
        }
    }

    /**
     *
     * @return the current DCInputsReader
     */
    public static DCInputsReaderExt getInputsReader() throws ServletException {
        // load inputsReader only the first time
        if (inputsReader == null) {
            // read configurable submissions forms data
            try {
                inputsReader = new DCInputsReaderExt();
            } catch (DCInputsReaderException e) {
                throw new ServletException(e);
            }
        }

        return inputsReader;
    }

    /**
     * @param filename file to get the input reader for
     * @return the current DCInputsReader
     */
    public static DCInputsReaderExt getInputsReader(String filename) throws ServletException {
        try {
            inputsReader = new DCInputsReaderExt(filename);
        } catch (DCInputsReaderException e) {
            throw new ServletException(e);
        }
        return inputsReader;
    }

    /**
     * @return the default language qualifier for metadata
     */
    public static String getDefaultLanguageQualifier() {
        String language = "";
        language = ConfigurationManager.getProperty("default.language");
        if (StringUtils.isEmpty(language)) {
            language = "en";
        }
        return language;
    }

    // ****************************************************************
    // ****************************************************************
    // METHODS FOR FILLING DC FIELDS FROM METADATA FORMS
    // ****************************************************************
    // ****************************************************************
    /**
     * Set relevant metadata fields in an item from name values in the form.
     * Some fields are repeatable in the form. If this is the case, and the
     * field is "dc.contributor.author", the names in the request will be from
     * the fields as follows:
     *
     * dc_contributor_author_last -> last name of first author
     * dc_contributor_author_first -> first name(s) of first author
     * dc_contributor_author_last_1 -> last name of second author
     * dc_contributor_author_first_1 -> first name(s) of second author
     *
     * and so on. If the field is unqualified:
     *
     * dc_contributor_last -> last name of first contributor
     * dc_contributor_first -> first name(s) of first contributor
     *
     * If the parameter "submit_dc_contributor_author_remove_n" is set, that
     * value is removed.
     *
     * Otherwise the parameters are of the form:
     *
     * dc_contributor_author_last dc_contributor_author_first
     *
     * The values will be put in separate DCValues, in the form "last name,
     * first name(s)", ordered as they appear in the list. These will replace
     * any existing values.
     *
     * @param request the request object
     * @param item the item to update
     * @param schema the metadata schema
     * @param element the metadata element
     * @param qualifier the metadata qualifier, or null if unqualified
     * @param repeated set to true if the field is repeatable on the form
     */
    protected void readNames(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated, boolean isAuthorityControlled) {
        String metadataField = MetadataField.formKey(schema, element, qualifier);

        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        //boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);

        // Names to add
        List<String> firsts = new LinkedList<String>();
        List<String> lasts = new LinkedList<String>();
        List<String> auths = new LinkedList<String>();
        List<String> confs = new LinkedList<String>();

        List<String> langs = new LinkedList<String>();

        if (repeated) {
            firsts = getRepeatedParameter(request, metadataField, metadataField
                    + "_first");
            lasts = getRepeatedParameter(request, metadataField, metadataField
                    + "_last");
            langs = getRepeatedParameter(request, metadataField, metadataField + "_lang");

            if (isAuthorityControlled) {
                auths = getRepeatedParameter(request, metadataField, metadataField
                        + "_authority");
                confs = getRepeatedParameter(request, metadataField, metadataField
                        + "_confidence");
            }

            // Find out if the relevant "remove" button was pressed
            // TODO: These separate remove buttons are only relevant
            // for DSpace JSP UI, and the code below can be removed
            // once the DSpace JSP UI is obsolete!
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + metadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton)) {
                int valToRemove = Integer.parseInt(buttonPressed.substring(removeButton.length()));

                firsts.remove(valToRemove);
                lasts.remove(valToRemove);
                if (valToRemove < langs.size()) {
                    langs.remove(valToRemove);
                }
                if (isAuthorityControlled) {
                    auths.remove(valToRemove);
                    confs.remove(valToRemove);
                }
            }
        } else {
            // Just a single name
            String lastName = request.getParameter(metadataField + "_last");
            String firstNames = request.getParameter(metadataField + "_first");
            String nameLang = request.getParameter(metadataField + "_lang");

            String authority = request.getParameter(metadataField + "_authority");
            String confidence = request.getParameter(metadataField + "_confidence");

            if (lastName != null) {
                lasts.add(lastName);
            }
            if (firstNames != null) {
                firsts.add(firstNames);
            }
            if (nameLang != null) {
                langs.add(nameLang);
            }
            auths.add(authority == null ? "" : authority);
            confs.add(confidence == null ? "" : confidence);
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < lasts.size(); i++) {
            String f = firsts.get(i);
            String l = lasts.get(i);
            String ll = "*";
            if (i < langs.size()) {
                ll = langs.get(i);
            }

            // only add if lastname is non-empty
            if ((l != null) && !((l.trim()).equals(""))) {
                // Ensure first name non-null
                if (f == null) {
                    f = "";
                }

                // If there is a comma in the last name, we take everything
                // after that comma, and add it to the right of the
                // first name
                int comma = l.indexOf(',');

                if (comma >= 0) {
                    f = f + l.substring(comma + 1);
                    l = l.substring(0, comma);

                    // Remove leading whitespace from first name
                    while (f.startsWith(" ")) {
                        f = f.substring(1);
                    }
                }

                // Add to the database -- unless required authority is missing
                if (isAuthorityControlled) {
                    String authKey = auths.size() > i ? auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? confs.get(i) : null;
                    if (MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey)
                            && (authKey == null || authKey.length() == 0)) {
                        log.warn("Skipping value of " + metadataField + " because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    } else {
                        item.addMetadata(schema, element, qualifier, ll,
                                new DCPersonName(l, f).toString(), authKey,
                                (sconf != null && sconf.length() > 0)
                                ? Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED);
                    }
                } else {
                    item.addMetadata(schema, element, qualifier, ll,
                            new DCPersonName(l, f).toString());
                }
            }
        }
    }

    /**
     * Fill out an item's metadata values from a plain standard text field. If
     * the field isn't repeatable, the input field name is called:
     *
     * element_qualifier
     *
     * or for an unqualified element:
     *
     * element
     *
     * Repeated elements are appended with an underscore then an integer. e.g.:
     *
     * dc_title_alternative dc_title_alternative_1
     *
     * The values will be put in separate DCValues, ordered as they appear in
     * the list. These will replace any existing values.
     *
     * @param request the request object
     * @param item the item to update
     * @param schema the short schema name
     * @param element the metadata element
     * @param qualifier the metadata qualifier, or null if unqualified
     * @param repeated set to true if the field is repeatable on the form
     * @param lang language to set (ISO code)
     */
    protected void readText(HttpServletRequest request, Item item, String schema,
            String element, String qualifier, boolean repeated, String lang, boolean isAuthorityControlled) {
        // FIXME: Of course, language should be part of form, or determined
        // some other way
        String metadataField = MetadataField.formKey(schema, element, qualifier);

        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        //boolean isAuthorityControlled = MetadataAuthorityManager.getManager().isAuthorityControlled(fieldKey);

        // Values to add
        List<String> vals;
        List<String> auths = null;
        List<String> confs = null;

        List<String> langs;

        if (repeated) {
            vals = getRepeatedParameter(request, metadataField, metadataField);
            langs = getRepeatedParameter(request, metadataField, metadataField + "_lang");
            if (isAuthorityControlled) {
                auths = getRepeatedParameter(request, metadataField, metadataField + "_authority");
                confs = getRepeatedParameter(request, metadataField, metadataField + "_confidence");
            }

            // Find out if the relevant "remove" button was pressed
            // TODO: These separate remove buttons are only relevant
            // for DSpace JSP UI, and the code below can be removed
            // once the DSpace JSP UI is obsolete!
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + metadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton)) {
                int valToRemove = Integer.parseInt(buttonPressed.substring(removeButton.length()));

                vals.remove(valToRemove);
                if (valToRemove < langs.size()) {
                    langs.remove(valToRemove);
                }
                if (isAuthorityControlled) {
                    auths.remove(valToRemove);
                    confs.remove(valToRemove);
                }
            }
        } else {
            // Just a single name
            vals = new LinkedList<String>();
            langs = new LinkedList<String>();
            String value = request.getParameter(metadataField);
            String ll = request.getParameter(metadataField + "_lang");
            if (value != null) {
                vals.add(value.trim());
            }
            if (ll != null) {
                langs.add(ll);
            }

            if (isAuthorityControlled) {
                auths = new LinkedList<String>();
                confs = new LinkedList<String>();
                String av = request.getParameter(metadataField + "_authority");
                String cv = request.getParameter(metadataField + "_confidence");
                auths.add(av == null ? "" : av.trim());
                confs.add(cv == null ? "" : cv.trim());
            }
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < vals.size(); i++) {
            // Add to the database if non-empty
            String s = vals.get(i);
            String l = lang;
            if (i < langs.size()) {
                l = langs.get(i);
            }

            if ((s != null) && !s.equals("")) {
                if (isAuthorityControlled) {
                    String authKey = auths.size() > i ? auths.get(i) : null;
                    String sconf = (authKey != null && confs.size() > i) ? confs.get(i) : null;
                    if (MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey)
                            && (authKey == null || authKey.length() == 0)) {
                        log.warn("Skipping value of " + metadataField + " because the required Authority key is missing or empty.");
                        addErrorField(request, metadataField);
                    } else {
                        item.addMetadata(schema, element, qualifier, l, s,
                                authKey, (sconf != null && sconf.length() > 0)
                                ? Choices.getConfidenceValue(sconf) : Choices.CF_ACCEPTED, isAuthorityControlled);
                    }
                } else {
                    item.addMetadata(schema, element, qualifier, l, s);
                }
            }
        }
    }

    /**
     * Fill out a metadata date field with the value from a form. The date is
     * taken from the three parameters:
     *
     * element_qualifier_year element_qualifier_month element_qualifier_day
     *
     * The granularity is determined by the values that are actually set. If the
     * year isn't set (or is invalid)
     *
     * @param request the request object
     * @param item the item to update
     * @param schema the metadata schema
     * @param element the metadata element
     * @param qualifier the metadata qualifier, or null if unqualified
     * @throws SQLException
     */
    protected void readDate(HttpServletRequest request, Item item, String schema,
            String element, String qualifier) throws SQLException {
        String metadataField = MetadataField.formKey(schema, element, qualifier);

        int year = Util.getIntParameter(request, metadataField + "_year");
        int month = Util.getIntParameter(request, metadataField + "_month");
        int day = Util.getIntParameter(request, metadataField + "_day");

        // FIXME: Probably should be some more validation
        // Make a standard format date
        DCDate d = new DCDate(year, month, day, -1, -1, -1);

        // already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        if (year > 0) {
            // Only put in date if there is one!
            item.addMetadata(schema, element, qualifier, null, d.toString());
        }
    }

    /**
     * Set relevant metadata fields in an item from series/number values in the
     * form. Some fields are repeatable in the form. If this is the case, and
     * the field is "relation.ispartof", the names in the request will be from
     * the fields as follows:
     *
     * dc_relation_ispartof_series dc_relation_ispartof_number
     * dc_relation_ispartof_series_1 dc_relation_ispartof_number_1
     *
     * and so on. If the field is unqualified:
     *
     * dc_relation_series dc_relation_number
     *
     * Otherwise the parameters are of the form:
     *
     * dc_relation_ispartof_series dc_relation_ispartof_number
     *
     * The values will be put in separate DCValues, in the form "last name,
     * first name(s)", ordered as they appear in the list. These will replace
     * any existing values.
     *
     * @param request the request object
     * @param item the item to update
     * @param schema the metadata schema
     * @param element the metadata element
     * @param qualifier the metadata qualifier, or null if unqualified
     * @param repeated set to true if the field is repeatable on the form
     */
    protected void readSeriesNumbers(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated) {
        String metadataField = MetadataField.formKey(schema, element, qualifier);

        // Names to add
        List<String> series = new LinkedList<String>();
        List<String> numbers = new LinkedList<String>();

        if (repeated) {
            series = getRepeatedParameter(request, metadataField, metadataField
                    + "_series");
            numbers = getRepeatedParameter(request, metadataField,
                    metadataField + "_number");

            // Find out if the relevant "remove" button was pressed
            String buttonPressed = Util.getSubmitButton(request, "");
            String removeButton = "submit_" + metadataField + "_remove_";

            if (buttonPressed.startsWith(removeButton)) {
                int valToRemove = Integer.parseInt(buttonPressed.substring(removeButton.length()));

                series.remove(valToRemove);
                numbers.remove(valToRemove);
            }
        } else {
            // Just a single name
            String s = request.getParameter(metadataField + "_series");
            String n = request.getParameter(metadataField + "_number");

            // Only put it in if there was a name present
            if ((s != null) && !s.equals("")) {
                // if number is null, just set to a nullstring
                if (n == null) {
                    n = "";
                }

                series.add(s);
                numbers.add(n);
            }
        }

        // Remove existing values, already done in doProcessing see also bug DS-203
        // item.clearMetadata(schema, element, qualifier, Item.ANY);

        // Put the names in the correct form
        for (int i = 0; i < series.size(); i++) {
            String s = (series.get(i)).trim();
            String n = (numbers.get(i)).trim();

            // Only add non-empty
            if (!s.equals("") || !n.equals("")) {
                item.addMetadata(schema, element, qualifier, null,
                        new DCSeriesNumber(s, n).toString());
            }
        }
    }

    /**
     * Get repeated values from a form. If "foo" is passed in as the parameter,
     * values in the form of parameters "foo", "foo_1", "foo_2", etc. are
     * returned. <P> This method can also handle "composite fields" (metadata
     * fields which may require multiple params, etc. a first name and last
     * name).
     *
     * @param request the HTTP request containing the form information
     * @param metadataField the metadata field which can store repeated values
     * @param param the repeated parameter on the page (used to fill out the
     * metadataField)
     *
     * @return a List of Strings
     */
    protected List<String> getRepeatedParameter(HttpServletRequest request,
            String metadataField, String param) {
        List<String> vals = new LinkedList<String>();

        int i = 1;    //start index at the first of the previously entered values
        boolean foundLast = false;

        // Iterate through the values in the form.
        while (!foundLast) {
            String s = null;

            //First, add the previously entered values.
            // This ensures we preserve the order that these values were entered
            s = request.getParameter(param + "_" + i);

            // If there are no more previously entered values,
            // see if there's a new value entered in textbox
            if (s == null) {
                s = request.getParameter(param);
                //this will be the last value added
                foundLast = true;
            }

            // We're only going to add non-null values
            if (s != null) {
                boolean addValue = true;

                // Check to make sure that this value was not selected to be
                // removed.
                // (This is for the "remove multiple" option available in
                // Manakin)
                String[] selected = request.getParameterValues(metadataField
                        + "_selected");

                if (selected != null) {
                    for (int j = 0; j < selected.length; j++) {
                        if (selected[j].equals(metadataField + "_" + i)) {
                            addValue = false;
                        }
                    }
                }

                if (addValue) {
                    vals.add(s.trim());
                }
            }

            i++;
        }

        log.debug("getRepeatedParameter: metadataField=" + metadataField
                + " param=" + metadataField + ", return count = " + vals.size());

        return vals;
    }

    /**
     * Return the HTML / DRI field name for the given input.
     *
     * @param input
     * @return
     */
    public static String getFieldName(DCInput input) {
        String dcSchema = input.getSchema();
        String dcElement = input.getElement();
        String dcQualifier = input.getQualifier();
        if (dcQualifier != null && !dcQualifier.equals(Item.ANY)) {
            return dcSchema + "_" + dcElement + '_' + dcQualifier;
        } else {
            return dcSchema + "_" + dcElement;
        }

    }
}
