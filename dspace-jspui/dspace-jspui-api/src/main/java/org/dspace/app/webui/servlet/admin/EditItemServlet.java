/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.Util;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.FileUploadRequest;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.*;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.license.CreativeCommons;
import proj.oceandocs.submission.DCInputSetExt;
import proj.oceandocs.submission.DCInputsReaderExt;

/**
 * Servlet for editing and deleting (expunging) items
 *
 * @author Robert Tansley
 * @version $Revision: 6158 $
 */
public class EditItemServlet extends DSpaceServlet {

    /** User wants to delete (expunge) an item */
    public static final int START_DELETE = 1;
    /** User confirms delete (expunge) of item */
    public static final int CONFIRM_DELETE = 2;
    /** User updates item */
    public static final int UPDATE_ITEM = 3;
    /** User starts withdrawal of item */
    public static final int START_WITHDRAW = 4;
    /** User confirms withdrawal of item */
    public static final int CONFIRM_WITHDRAW = 5;
    /** User reinstates a withdrawn item */
    public static final int REINSTATE = 6;
    /** User starts the movement of an item */
    public static final int START_MOVE_ITEM = 7;
    /** User confirms the movement of the item */
    public static final int CONFIRM_MOVE_ITEM = 8;
    public static final int CHANGE_DOCTYPE = 9;
    public static final int ADD_FIELD = 10;
    public static final int REMOVE_FIELD = 11;
    public static final int NEW_FIELD = 12;
    public static final String LANGUAGE_QUALIFIER = getDefaultLanguageQualifier();
    /** Logger */
    private static Logger log = Logger.getLogger(EditItemServlet.class);
    private static DCInputsReaderExt inputsReader = null;

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException {
        /*
         * GET with no parameters displays "find by handle/id" form parameter
         * item_id -> find and edit item with internal ID item_id parameter
         * handle -> find and edit corresponding item if internal ID or Handle
         * are invalid, "find by handle/id" form is displayed again with error
         * message
         */
        int internalID = UIUtil.getIntParameter(request, "item_id");
        String handle = request.getParameter("handle");
        boolean showError = false;

        // See if an item ID or Handle was passed in
        Item itemToEdit = null;

        if (internalID > 0) {
            itemToEdit = Item.find(context, internalID);

            showError = (itemToEdit == null);
        } else if ((handle != null) && !handle.equals("")) {
            // resolve handle
            DSpaceObject dso = HandleManager.resolveToObject(context, handle.trim());

            // make sure it's an ITEM
            if ((dso != null) && (dso.getType() == Constants.ITEM)) {
                itemToEdit = (Item) dso;
                showError = false;
            } else {
                showError = true;
            }
        }

        // Show edit form if appropriate
        if (itemToEdit != null) {
            // now check to see if person can edit item
            checkEditAuthorization(context, itemToEdit);
            showEditForm(context, request, response, itemToEdit);
        } else {
            if (showError) {
                request.setAttribute("invalid.id", Boolean.TRUE);
            }

            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
        }
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException {
        // First, see if we have a multipart request (uploading a new bitstream)
        String contentType = request.getContentType();

        if ((contentType != null)
                && (contentType.indexOf("multipart/form-data") != -1)) {
            // This is a multipart request, so it's a file upload
            processUploadBitstream(context, request, response);

            return;
        }

        /*
         * Then we check for a "cancel" button - if it's been pressed, we simply
         * return to the "find by handle/id" page
         */
        if (request.getParameter("submit_cancel") != null) {
            JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");

            return;
        }

        /*
         * Respond to submitted forms. Each form includes an "action" parameter
         * indicating what needs to be done (from the constants above.)
         */
        int action = UIUtil.getIntParameter(request, "action");

        Item item = Item.find(context, UIUtil.getIntParameter(request,
                "item_id"));

        if (item == null) {
            return;
        }

        String handle = HandleManager.findHandle(context, item);

        // now check to see if person can edit item
        checkEditAuthorization(context, item);

        request.setAttribute("item", item);
        request.setAttribute("handle", handle);

        switch (action) {
            case START_DELETE:

                // Show "delete item" confirmation page
                JSPManager.showJSP(request, response,
                        "/tools/confirm-delete-item.jsp");

                break;

            case CONFIRM_DELETE:

                // Delete the item - if "cancel" was pressed this would be
                // picked up above
                // FIXME: Don't know if this does all it should - remove Handle?
                Collection[] collections = item.getCollections();

                // Remove item from all the collections it's in
                for (int i = 0; i < collections.length; i++) {
                    collections[i].removeItem(item);
                }

                // Due to "virtual" owning collection, item.getCollections() could have excluded the owning collection
                // so delete the item from it's owning collection
                if (item.getOwningCollection() != null)
                    item.getOwningCollection().removeItem(item);

                JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
                context.complete();

                break;

            case UPDATE_ITEM:
                processUpdateItem(context, request, response, item);

                break;

            case START_WITHDRAW:

                // Show "withdraw item" confirmation page
                JSPManager.showJSP(request, response,
                        "/tools/confirm-withdraw-item.jsp");

                break;

            case CONFIRM_WITHDRAW:

                // Withdraw the item
                item.withdraw();
                JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
                context.complete();

                break;

            case REINSTATE:
                item.reinstate();
                JSPManager.showJSP(request, response, "/tools/get-item-id.jsp");
                context.complete();

                break;

            case START_MOVE_ITEM:
                if (AuthorizeManager.isAdmin(context, item)) {
                    // Display move collection page with fields of collections and communities
                    Collection[] allNotLinkedCollections = item.getCollectionsNotLinked();
                    Collection[] allLinkedCollections = item.getCollections();

                    // get only the collection where the current user has the right permission
                    List<Collection> authNotLinkedCollections = new ArrayList<Collection>();
                    for (Collection c : allNotLinkedCollections) {
                        if (AuthorizeManager.authorizeActionBoolean(context, c, Constants.ADD)) {
                            authNotLinkedCollections.add(c);
                        }
                    }

                    List<Collection> authLinkedCollections = new ArrayList<Collection>();
                    for (Collection c : allLinkedCollections) {
                        if (AuthorizeManager.authorizeActionBoolean(context, c, Constants.REMOVE)) {
                            authLinkedCollections.add(c);
                        }
                    }

                    Collection[] notLinkedCollections = new Collection[authNotLinkedCollections.size()];
                    notLinkedCollections = authNotLinkedCollections.toArray(notLinkedCollections);
                    Collection[] linkedCollections = new Collection[authLinkedCollections.size()];
                    linkedCollections = authLinkedCollections.toArray(linkedCollections);

                    request.setAttribute("linkedCollections", linkedCollections);
                    request.setAttribute("notLinkedCollections", notLinkedCollections);

                    JSPManager.showJSP(request, response, "/tools/move-item.jsp");
                } else {
                    throw new ServletException("You must be an administrator to move an item");
                }

                break;

            case CONFIRM_MOVE_ITEM:
                if (AuthorizeManager.isAdmin(context, item)) {
                    Collection fromCollection = Collection.find(context, UIUtil.getIntParameter(request, "collection_from_id"));
                    Collection toCollection = Collection.find(context, UIUtil.getIntParameter(request, "collection_to_id"));

                    Boolean inheritPolicies = false;
                    if (request.getParameter("inheritpolicies") != null) {
                        inheritPolicies = true;
                    }

                    if (fromCollection == null || toCollection == null) {
                        throw new ServletException("Missing or incorrect collection IDs for moving item");
                    }

                    item.move(fromCollection, toCollection, inheritPolicies);

                    showEditForm(context, request, response, item);

                    context.complete();
                } else {
                    throw new ServletException("You must be an administrator to move an item");
                }

                break;

            case CHANGE_DOCTYPE:

                String doctype = request.getParameter("select_doctype");

                if (doctype != null && !"".equals(doctype)) {
                    item.clearMetadata("dc", "type", null, Item.ANY);
                    item.addMetadata("dc", "type", null, Item.ANY, doctype);
                    item.update();
                    showEditForm(context, request, response, item);
                    context.complete();
                }

                break;

            case ADD_FIELD:

                break;

            case REMOVE_FIELD:

                break;

            default:

                // Erm... weird action value received.
                log.warn(LogManager.getHeader(context, "integrity_error", UIUtil.getRequestLogInfo(request)));
                JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Throw an exception if user isn't authorized to edit this item
     *
     * @param c
     * @param item
     */
    private void checkEditAuthorization(Context c, Item item)
            throws AuthorizeException, java.sql.SQLException {
        if (!item.canEdit()) {
            int userID = 0;

            // first, check if userid is set
            if (c.getCurrentUser() != null) {
                userID = c.getCurrentUser().getID();
            }

            // show an error or throw an authorization exception
            throw new AuthorizeException("EditItemServlet: User " + userID
                    + " not authorized to edit item " + item.getID());
        }
    }

    /**
     * Show the item edit form for a particular item
     *
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     */
    private void showEditForm(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item) throws ServletException,
            IOException, SQLException, AuthorizeException {
//        if (request.getParameter("cc_license_url") != null) {
//            // check authorization
//            AuthorizeUtil.authorizeManageCCLicense(context, item);
//
//            // turn off auth system to allow replace also to user that can't
//            // remove/add bitstream to the item
//            context.turnOffAuthorisationSystem();
//            // set or replace existing CC license
//            CreativeCommons.setLicense(context, item,
//                    request.getParameter("cc_license_url"));
//            context.restoreAuthSystemState();
//            context.commit();
//        }

        // Get the handle, if any
        String handle = HandleManager.findHandle(context, item);

        // Collections
        Collection[] collections = item.getCollections();

        // All DC types in the registry
        MetadataField[] types = MetadataField.findAll(context);

        // Get a HashMap of metadata field ids and a field name to display
        Map<Integer, String> metadataFields = new HashMap<Integer, String>();

        // Get all existing Schemas
        MetadataSchema[] schemas = MetadataSchema.findAll(context);
        for (int i = 0; i < schemas.length; i++) {
            String schemaName = schemas[i].getName();
            // Get all fields for the given schema
            MetadataField[] fields = MetadataField.findAllInSchema(context, schemas[i].getSchemaID());
            for (int j = 0; j < fields.length; j++) {
                Integer fieldID = Integer.valueOf(fields[j].getFieldID());
                String displayName = "";
                displayName = schemaName + "." + fields[j].getElement() + (fields[j].getQualifier() == null ? "" : "." + fields[j].getQualifier());
                metadataFields.put(fieldID, displayName);
            }
        }

        request.setAttribute("admin_button", AuthorizeManager.authorizeActionBoolean(context, item, Constants.ADMIN));
        try {
            AuthorizeUtil.authorizeManageItemPolicy(context, item);
            request.setAttribute("policy_button", Boolean.TRUE);
        } catch (AuthorizeException authex) {
            request.setAttribute("policy_button", Boolean.FALSE);
        }

        if (AuthorizeManager.authorizeActionBoolean(context, item.getParentObject(), Constants.REMOVE)) {
            request.setAttribute("delete_button", Boolean.TRUE);
        } else {
            request.setAttribute("delete_button", Boolean.FALSE);
        }

        try {
            AuthorizeManager.authorizeAction(context, item, Constants.ADD);
            request.setAttribute("create_bitstream_button", Boolean.TRUE);
        } catch (AuthorizeException authex) {
            request.setAttribute("create_bitstream_button", Boolean.FALSE);
        }

        try {
            AuthorizeManager.authorizeAction(context, item, Constants.REMOVE);
            request.setAttribute("remove_bitstream_button", Boolean.TRUE);
        } catch (AuthorizeException authex) {
            request.setAttribute("remove_bitstream_button", Boolean.FALSE);
        }

        try {
            AuthorizeUtil.authorizeManageCCLicense(context, item);
            request.setAttribute("cclicense_button", Boolean.TRUE);
        } catch (AuthorizeException authex) {
            request.setAttribute("cclicense_button", Boolean.FALSE);
        }

        if (!item.isWithdrawn()) {
            try {
                AuthorizeUtil.authorizeWithdrawItem(context, item);
                request.setAttribute("withdraw_button", Boolean.TRUE);
            } catch (AuthorizeException authex) {
                request.setAttribute("withdraw_button", Boolean.FALSE);
            }
        } else {
            try {
                AuthorizeUtil.authorizeReinstateItem(context, item);
                request.setAttribute("reinstate_button", Boolean.TRUE);
            } catch (AuthorizeException authex) {
                request.setAttribute("reinstate_button", Boolean.FALSE);
            }
        }


        request.setAttribute("item", item);
        request.setAttribute("handle", handle);
        request.setAttribute("collections", collections);
        request.setAttribute("dc.types", types);
        request.setAttribute("metadataFields", metadataFields);

        JSPManager.showJSP(request, response, "/tools/edit-item-form.jsp");
    }

    /**
     * Process input from the edit item form
     *
     * @param context
     *            DSpace context
     * @param request
     *            the HTTP request containing posted info
     * @param response
     *            the HTTP response
     * @param item
     *            the item
     */
    private void processUpdateItem(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item) throws ServletException,
            IOException, SQLException, AuthorizeException {
        
        String button = UIUtil.getSubmitButton(request, "submit");

        String docType = "";
        DCValue[] doctypes = item.getMetadata("dc", "type", null, Item.ANY);
        if (doctypes.length > 0) {
            docType = doctypes[0].value;
        }


        // We'll sort the parameters by name. This ensures that DC fields
        // of the same element/qualifier are added in the correct sequence.
        // Get the parameters names
        List<String> paramNames = Collections.list(request.getParameterNames());

        List<DCInput> inputs = null;

        try {
            inputsReader = new DCInputsReaderExt();
            DCInputSetExt inset = inputsReader.getInputs(docType);
            if (inset != null) {
                inputs = inset.getAllFields();
            }
        } catch (DCInputsReaderException e) {
            throw new ServletException(e);
        }

        /*
         * "Cancel" handled above, so whatever happens, we need to update the
         * item metadata. First, we remove it all, then build it back up again.
         */
        //item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        
        // Step 1:
        // clear out all item metadata defined on this page
        for (int i = 0; i < inputs.size(); i++)
        {
            String qualifier = inputs.get(i).getQualifier();
            if (qualifier == null
                && inputs.get(i).getInputType().equals("qualdrop_value"))
            {
                qualifier = Item.ANY;
            }
            item.clearMetadata(inputs.get(i).getSchema(), inputs.get(i).getElement(),
                               qualifier, Item.ANY);
        }

        // now update the item metadata.
        String fieldName;
        boolean moreInput = false;
        for (int j = 0; j < inputs.size(); j++) {
            
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
                    if (!button.equals("submit_" + schema + "_"
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
                && button.equals("submit_" + fieldName + "_add"))
            {
                moreInput = true;
                item.addMetadata(schema, element, qualifier, LANGUAGE_QUALIFIER, "");
                item.update();
//                if(request.getAttribute("moreInputs") == null)
//                    request.setAttribute("moreInputs", true);
//                showEditForm(context, request, response, item);
            }
            // was XMLUI's "remove" button pushed?
            else if (button.equals("submit_" + fieldName + "_delete"))
            {
                showEditForm(context, request, response, item);
            }
        }

        for (String p : paramNames) {
            if (p.startsWith("bitstream_name")) {
                // We have bitstream metadata
                // First, get the bundle and bitstream ID
                // Parameter name is bitstream_name_(bundleID)_(bitstreamID)
                StringTokenizer st = new StringTokenizer(p, "_");

                // Ignore "bitstream" and "name"
                st.nextToken();
                st.nextToken();

                // Bundle ID and bitstream ID next
                int bundleID = Integer.parseInt(st.nextToken());
                int bitstreamID = Integer.parseInt(st.nextToken());

                Bundle bundle = Bundle.find(context, bundleID);
                Bitstream bitstream = Bitstream.find(context, bitstreamID);

                // Get the string "(bundleID)_(bitstreamID)" for finding other
                // parameters related to this bitstream
                String key = String.valueOf(bundleID) + "_" + bitstreamID;

                // Update bitstream metadata, or delete?
                if (button.equals("submit_delete_bitstream_" + key)) {
                    // "delete" button pressed
                    bundle.removeBitstream(bitstream);

                    // Delete bundle too, if empty
                    if (bundle.getBitstreams().length == 0) {
                        item.removeBundle(bundle);
                    }
                } else {
                    // Update the bitstream metadata
                    String name = request.getParameter(p);
                    String source = request.getParameter("bitstream_source_"
                            + key);
                    String desc = request.getParameter("bitstream_description_"
                            + key);
                    int formatID = UIUtil.getIntParameter(request,
                            "bitstream_format_id_" + key);
                    String userFormatDesc = request.getParameter("bitstream_user_format_description_"
                            + key);
                    int primaryBitstreamID = UIUtil.getIntParameter(request,
                            bundleID + "_primary_bitstream_id");

                    // Empty strings become non-null
                    if (source.equals("")) {
                        source = null;
                    }

                    if (desc.equals("")) {
                        desc = null;
                    }

                    if (userFormatDesc.equals("")) {
                        userFormatDesc = null;
                    }

                    bitstream.setName(name);
                    bitstream.setSource(source);
                    bitstream.setDescription(desc);
                    bitstream.setFormat(BitstreamFormat.find(context, formatID));

                    if (primaryBitstreamID > 0) {
                        bundle.setPrimaryBitstreamID(primaryBitstreamID);
                    }

                    if (userFormatDesc != null) {
                        bitstream.setUserFormatDescription(userFormatDesc);
                    }

                    bitstream.update();
                    bundle.update();
                }
            }

        }
        
        
        HashMap<String, String> otherFields = new HashMap<String, String>();
        List<DCValue> otherDC = new ArrayList<DCValue>();
        List<String> otherToClean = new ArrayList<String>();
        
        for(String p: paramNames)
        {
            if(p.startsWith("other_"))
                otherFields.put(p, request.getParameter(p));
        }
        
        String fname ="", fval ="", flang = "";
        String parts[];
        for(Map.Entry<String, String> kvp: otherFields.entrySet())
        {
            if(!kvp.getKey().endsWith("_lang"))
            {
                parts = kvp.getKey().split("_");
                fname = parts[2];
                fval = kvp.getValue();
                flang = otherFields.get(kvp.getKey() + "_lang");
                
                if(!otherToClean.contains(fname))
                    otherToClean.add(fname);
                
                DCValue dcv = new DCValue();
                dcv.value = fval;
                dcv.language = flang;
                dcv.setQuals(fname, ".");
                
                otherDC.add(dcv);
            }
        }
        
        for(String tc: otherToClean)
        {
            parts = tc.split("\\.");
            if(parts.length == 2)
                item.clearMetadata(parts[0], parts[1], null, Item.ANY);
            else if(parts.length == 3)
                item.clearMetadata(parts[0], parts[1], parts[2], Item.ANY);
        }
        
        for(DCValue dcv: otherDC)
        {
            // only if the value is nonempty
            if (dcv.value != null && !dcv.value.trim().equals(""))
            item.addMetadata(dcv.schema, dcv.element, dcv.qualifier, dcv.language, dcv.value);
        }

        /*
         * Now respond to button presses, other than "Remove" or "Delete" button
         * presses which were dealt with in the above loop.
         */


        if (button.equals("submit_addfield")) {
            // Adding a metadata field
            int dcTypeID = UIUtil.getIntParameter(request, "addfield_dctype");
            String value = request.getParameter("addfield_value").trim();
            String lang = request.getParameter("addfield_lang");

            // trim language and set empty string language = null
            if (lang != null) {
                lang = lang.trim();
                if (lang.equals("")) {
                    lang = null;
                }
            }

            MetadataField field = MetadataField.find(context, dcTypeID);
            MetadataSchema mschema = MetadataSchema.find(context, field.getSchemaID());
            item.addMetadata(mschema.getName(), field.getElement(), field.getQualifier(), lang, value);
            item.update();
//            showEditForm(context, request, response, item);
        }


//        if (button.equals(
//                "submit_addcc")) {
//            // Show cc-edit page
//            request.setAttribute("item", item);
//            JSPManager.showJSP(request, response, "/tools/creative-commons-edit.jsp");
//        }


        if (button.equals(
                "submit_addbitstream")) {
            // Show upload bitstream page
            request.setAttribute("item", item);
            JSPManager.showJSP(request, response, "/tools/upload-bitstream.jsp");
        } else {
            // Show edit page again
            showEditForm(context, request, response, item);
        }

        item.updateISSN();
        //item.updateCitationString();
        //item.updateSubjectFields();
        item.update();

        // Complete transaction
        context.complete();
    }

    /**
     * Process the input from the upload bitstream page
     *
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processUploadBitstream(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException {
        try {
            // Wrap multipart request to get the submission info
            FileUploadRequest wrapper = new FileUploadRequest(request);
            Bitstream b = null;
            Item item = Item.find(context, UIUtil.getIntParameter(wrapper, "item_id"));
            File temp = wrapper.getFile("file");

            // Read the temp file as logo
            InputStream is = new BufferedInputStream(new FileInputStream(temp));

            // now check to see if person can edit item
            checkEditAuthorization(context, item);

            // do we already have an ORIGINAL bundle?
            Bundle[] bundles = item.getBundles("ORIGINAL");

            if (bundles.length < 1) {
                // set bundle's name to ORIGINAL
                b = item.createSingleBitstream(is, "ORIGINAL");

                // set the permission as defined in the owning collection
                Collection owningCollection = item.getOwningCollection();
                if (owningCollection != null) {
                    Bundle bnd = b.getBundles()[0];
                    bnd.inheritCollectionDefaultPolicies(owningCollection);
                }
            } else {
                // we have a bundle already, just add bitstream
                b = bundles[0].createBitstream(is);
            }

            // Strip all but the last filename. It would be nice
            // to know which OS the file came from.
            String noPath = wrapper.getFilesystemName("file");

            while (noPath.indexOf('/') > -1) {
                noPath = noPath.substring(noPath.indexOf('/') + 1);
            }

            while (noPath.indexOf('\\') > -1) {
                noPath = noPath.substring(noPath.indexOf('\\') + 1);
            }

            b.setName(noPath);
            b.setSource(wrapper.getFilesystemName("file"));

            // Identify the format
            BitstreamFormat bf = FormatIdentifier.guessFormat(context, b);
            b.setFormat(bf);
            b.update();

            item.update();

            // Back to edit form
            showEditForm(context, request, response, item);

            // Remove temp file
            if (!temp.delete()) {
                log.error("Unable to delete temporary file");
            }

            // Update DB
            context.complete();
        } catch (FileSizeLimitExceededException ex) {
            log.warn("Upload exceeded upload.max");
            JSPManager.showFileSizeLimitExceededError(request, response, ex.getMessage(), ex.getActualSize(), ex.getPermittedSize());
        }
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
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the metadata schema
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     */
    protected void readNames(HttpServletRequest request, Item item,
            String schema, String element, String qualifier, boolean repeated, 
            boolean isAuthorityControlled) {
        String metadataField = MetadataField.formKey(schema, element, qualifier);
        String fieldKey = MetadataField.formKey(schema, element, qualifier);

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
                        //addErrorField(request, metadataField);
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
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the short schema name
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
     * @param lang
     *            language to set (ISO code)
     */
    protected void readText(HttpServletRequest request, Item item, String schema,
            String element, String qualifier, boolean repeated, String lang, 
            boolean isAuthorityControlled) {
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
                        //addErrorField(request, metadataField);
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
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the metadata schema
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
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
     * @param request
     *            the request object
     * @param item
     *            the item to update
     * @param schema
     *            the metadata schema
     * @param element
     *            the metadata element
     * @param qualifier
     *            the metadata qualifier, or null if unqualified
     * @param repeated
     *            set to true if the field is repeatable on the form
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
     * returned.
     * <P>
     * This method can also handle "composite fields" (metadata fields which may
     * require multiple params, etc. a first name and last name).
     *
     * @param request
     *            the HTTP request containing the form information
     * @param metadataField
     *            the metadata field which can store repeated values
     * @param param
     *            the repeated parameter on the page (used to fill out the
     *            metadataField)
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
}

