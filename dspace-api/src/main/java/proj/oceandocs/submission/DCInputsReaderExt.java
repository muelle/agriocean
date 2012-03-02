/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.submission;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.xml.sax.SAXException;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.ConfigurationManager;

/**
 * Submission form generator for DSpace. Reads and parses the installation form
 * definitions file, input-forms.xml, from the configuration directory. A forms
 * definition details the page and field layout of the metadata collection pages
 * used by the submission process. Each forms definition starts with a unique
 * name that gets associated with that form set.
 *
 * The file also specifies which collections use which form sets. At a minimum,
 * the definitions file must define a default mapping from the placeholder
 * collection #0 to the distinguished form 'default'. Any collections that use a
 * custom form set are listed paired with the name of the form set they use.
 *
 * The definitions file also may contain sets of value pairs. Each value pair
 * will contain one string that the user reads, and a paired string that will
 * supply the value stored in the database if its sibling display value gets
 * selected from a choice list.
 *
 * @author Brian S. Hughes
 * @version $Revision: 4365 $
 */
public class DCInputsReaderExt {

    /**
     * The ID of the default collection. Will never be the ID of a named
     * collection
     */
    public static final String DEFAULT_COLLECTION = "default";
    /**
     * Name of the extended form definition XML file (AgriOceanDSpace project)
     */
    static final String FORM_DEF_FILE = "input-forms-extended.xml";
    /**
     * Keyname for storing dropdown value-pair set name
     */
    static final String PAIR_TYPE_NAME = "value-pairs-name";
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(DCInputsReaderExt.class);
    /**
     * The fully qualified pathname of the form definition XML file
     */
    private String defsFile = ConfigurationManager.getProperty("dspace.dir")
            + File.separator + "config" + File.separator + FORM_DEF_FILE;
    /**
     * Reference to the types to forms map, computed from the forms definition
     * file. One form can be used for different types, but not vice versa. (form
     * name, type name)
     */
    private HashMap<String, String> type2Forms = null;
    /**
     * Reference to the collections to types map, computed from the forms
     * definition file (handle, (type, type ...))
     */
    private HashMap<String, List<String>> col2Types = null;
    /**
     * Reference to the forms definitions map, computed from the forms
     * definition file
     */
    private HashMap<String, DCInputSetExt> formDefns = null;
    /**
     * Reference to the value-pairs map, computed from the forms defition file
     */
    private HashMap valuePairs = null;    // Holds display/storage pairs

    /**
     * Parse an XML encoded submission forms template file, and create a hashmap
     * containing all the form information. This hashmap will contain four top
     * level structures: a map between collections and document types, a map
     * between document types and forms ,the definition for each page of each
     * form, and lists of pairs of values that populate selection boxes.
     */
    public DCInputsReaderExt()
            throws DCInputsReaderException {
        type2Forms = new HashMap<String, String>();
        col2Types = new HashMap<String, List<String>>();
        formDefns = new HashMap<String, DCInputSetExt>();
        valuePairs = new HashMap();

        buildInputs(defsFile);
    }

    public DCInputsReaderExt(String fileName)
            throws DCInputsReaderException {
        type2Forms = new HashMap<String, String>();
        col2Types = new HashMap<String, List<String>>();
        formDefns = new HashMap<String, DCInputSetExt>();
        valuePairs = new HashMap();

        buildInputs(fileName);
    }

    private void buildInputs(String fileName) throws DCInputsReaderException {
        String uri = "file:" + new File(fileName).getAbsolutePath();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder db = factory.newDocumentBuilder();
            db.setErrorHandler(new XMLReadErrorHandler(log, uri));
            Document doc = db.parse(uri);

            XPathFactory xfactory = XPathFactory.newInstance();
            XPath xPath = xfactory.newXPath();

            processColMap((Node) xPath.evaluate("/input-forms/collection-type", doc, XPathConstants.NODE));
            processTypeMap((Node) xPath.evaluate("/input-forms/type-form", doc, XPathConstants.NODE));
            processValuePairs((Node) xPath.evaluate("/input-forms/form-value-pairs", doc, XPathConstants.NODE));
            processForms(doc);
        } catch (FactoryConfigurationError fe) {
            throw new DCInputsReaderException("Cannot create Submission form parser", fe);
        } catch (Exception e) {
            throw new DCInputsReaderException("Error creating submission forms: " + e);
        }
    }

    /**
     * Process the form-definitions section of the XML file. Each element is
     * formed thusly: <form name="..." baseForm="...">...pages...</form> Each
     * pages subsection is formed: <page number="#"> ...groups... </page> Each
     * group is formed from field rows and each field row is formed from field.
     * Each field is formed from: dc-element, dc-qualifier, label, hint,
     * input-type name, required text, and repeatable flag.
     */
    private void processForms(Document doc)
            throws XPathExpressionException, SAXException, DCInputsReaderException {
        XPathFactory xfactory = XPathFactory.newInstance();
        XPath xPath = xfactory.newXPath();

        NodeList forms = (NodeList) xPath.evaluate("/input-forms/form-definitions/form", doc, XPathConstants.NODESET);
        for (int i = 0; i < forms.getLength(); i++) {
            processDefinition(forms.item(i), doc);
        }
    }

    private void processDefinition(Node e, Document doc)
            throws SAXException, XPathExpressionException, DCInputsReaderException {
        if (e != null) {
            String formName = getAttribute(e, "name");
            String baseForm = getAttribute(e, "baseForm");

            if (formName == null) {
                throw new SAXException("form element has no name attribute");
            }

            /*
             * if base form defined we check is it processed already, if not -
             * call processDefinition for base form first (recursion).
             */
            if (baseForm != null && !formDefns.containsKey(baseForm)) {
                XPathFactory xfactory = XPathFactory.newInstance();
                XPath xPath = xfactory.newXPath();

                Node baseFormNode = (Node) xPath.evaluate("/input-forms/form-definitions/form[@name='" + baseForm + "']", doc, XPathConstants.NODE);
                if (baseFormNode != null) {
                    processDefinition(baseFormNode, doc);
                } else {
                    throw new DCInputsReaderException("base form definition is missing. baseForm='" + baseForm + "'");
                }
                /*
                 * Now we have baseForm processed and can merge input
                 * pages/groups on the fly
                 */
            }
            String formHint = null;

            HashMap<Integer, List<DCInputGroup>> pages = new HashMap<Integer, List<DCInputGroup>>();

            if (baseForm != null) {
                if (formDefns.containsKey(baseForm)) {
                    pages = formDefns.get(baseForm).copyAllPages();
                } else {
                    throw new DCInputsReaderException("base form definition is missing. baseForm='" + baseForm + "'");
                }
            }

            NodeList pl = e.getChildNodes();
            int lenpg = pl.getLength();

            for (int j = 0; j < lenpg; j++) {
                Node npg = pl.item(j);
                //get form hint if provided
                if (npg.getNodeName().equals("hint")) {
                    formHint = getValue(npg);
                }
                // process each page definition
                if (npg.getNodeName().equals("page")) {
                    String pgNum = getAttribute(npg, "number");
                    int pgNumInt = -1;
                    if (pgNum == null) {
                        throw new SAXException("Form " + formName + " has no identified pages");
                    } else {
                        try {
                            pgNumInt = Integer.parseInt(pgNum);
                        } catch (Exception ex) {
                            throw new SAXException("Form " + formName + " has no non integer page number " + pgNum);
                        }
                    }
                    /*
                     * Check if page with number pgNumInt not in pages then add
                     */
                    if (!pages.containsKey(pgNumInt)) {
                        pages.put(pgNumInt, new ArrayList<DCInputGroup>());
                    }

                    // process each fields group on the given page
                    NodeList gl = npg.getChildNodes();
                    int lengl = gl.getLength();
                    for (int g = 0; g < lengl; g++) {
                        Node ng = gl.item(g); // get fieldgroup node
                        if (ng.getNodeName().equals("fieldgroup")) {
                            String groupName = getAttribute(ng, "name");
                            // go inside <fieldgroup> element
                            DCInputGroup group = new DCInputGroup(groupName);

                            NodeList frl = ng.getChildNodes();
                            int lenfrl = frl.getLength();
                            for (int f = 0; f < lenfrl; f++) {
                                Node nfr = frl.item(f);
                                if (nfr.getNodeName().equals("label")) {
                                    group.setLabel(getValue(nfr));
                                }
                                if (nfr.getNodeName().equals("hint")) {
                                    group.setHint(getValue(nfr));
                                }

                                if (nfr.getNodeName().equals("fieldrow")) {
                                    ArrayList<DCInput> row = new ArrayList<DCInput>();
                                    // process each row of fields, at last
                                    NodeList fl = nfr.getChildNodes();
                                    int lenfl = fl.getLength();
                                    for (int l = 0; l < lenfl; l++) {
                                        Node nfld = fl.item(l);
                                        if (nfld.getNodeName().equals("field")) {
                                            // process each field definition
                                            DCInput curField = processField(formName, pgNum, nfld);
                                            row.add(curField);
                                        }
                                    }
                                    group.setRow(row);
                                }
                            }
                            /*
                             * We have to check if group with groupName already
                             * present then replace it with just created one
                             * else just add
                             */
                            if (getDuplicateGroup(pages.get(pgNumInt), group) != null) {
                                pages.get(pgNumInt).remove(group);
                            }
                            pages.get(pgNumInt).add(group);
                        }
                    }
                }
            }

            // sanity check number of pages
            if (pages.size() < 1) {
                throw new DCInputsReaderException("Form " + formName + " has no pages");
            }
            DCInputSetExt form = new DCInputSetExt(formName, formHint, baseForm, pages);
            formDefns.put(formName, form);
        }

    }

    /**
     * Process the collection-type section of the XML file. Each element looks
     * like: <collection handle="default"> <type name="Article - published" />
     * ... </collection> Extract the collection handle and type name, put name
     * in hashmap list keyed by the collection handle.
     *
     * @see DCInputsReader#col2Types
     */
    private void processColMap(Node e)
            throws SAXException {
        NodeList nl = e.getChildNodes(); //<collection handle="...">
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            String nodeName = nl.item(i).getNodeName();
            if (nodeName.equals("collection")) {
                String colhandle = getAttribute(nl.item(i), "handle");
                if (colhandle == null) {
                    throw new SAXException("collection element is missing handle attribute");
                }
                NodeList cols = nl.item(i).getChildNodes(); //<type name="..." />
                int colsnum = cols.getLength();
                for (int j = 0; j < colsnum; j++) {
                    Node nd = cols.item(j);
                    if (nd.getNodeName().equals("type")) {
                        String typename = getAttribute(nd, "name");

                        if (typename == null) {
                            throw new SAXException("type element is missing name attribute");
                        }
                        if (col2Types.containsKey(colhandle)) {
                            col2Types.get(colhandle).add(typename);
                        } else {
                            ArrayList<String> types = new ArrayList<String>();
                            types.add(typename);
                            col2Types.put(colhandle, types);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process the type-form section of the XML file. Each element looks like:
     * <type name="type name" form="form definition name"/> Extract the type
     * name and form name, put form name in hashmap keyed by the type name.
     */
    private void processTypeMap(Node e)
            throws SAXException {
        NodeList nl = e.getChildNodes(); //<type name="" form=""/>
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            if (nl.item(i).getNodeName().equals("type")) {
                String name = getAttribute(nl.item(i), "name");
                String form = getAttribute(nl.item(i), "form");

                if (name == null) {
                    throw new SAXException("type element is missing name attribute");
                }
                if (form == null) {
                    throw new SAXException("type element is missing form attribute");
                }

                if (type2Forms.containsKey(name)) {
                    type2Forms.remove(name);
                    type2Forms.put(name, form);
                } else {
                    type2Forms.put(name, form);
                }
            }
        }
    }

    /**
     * Process parts of a field At the end, make sure that input-types
     * 'qualdrop_value' and 'twobox' are marked repeatable. Complain if
     * dc-element, label, or input-type are missing.
     */
    private DCInput processField(String formName, String page, Node n)
            throws SAXException {
        HashMap field = new HashMap();
        Integer inputSize = 0;
        NodeList nl = n.getChildNodes();
        int len = nl.getLength();
        for (int i = 0; i < len; i++) {
            Node nd = nl.item(i);
            if (!isEmptyTextNode(nd)) {
                String tagName = nd.getNodeName();
                String value = getValue(nd);
                field.put(tagName, value);
                if (tagName.equals("input-type")) {
                    try {
                        String size = getAttribute(nd, "size");

                        inputSize = size != null ? Integer.parseInt(size) : 0;

                    } catch (NumberFormatException e) {
                        inputSize = 0;
                        log.info("invalid value of size attribute = "
                                + getAttribute(nd, "size") + " of <input-type> "
                                + "element in the " + FORM_DEF_FILE + "\n" + e.getMessage());
                    }

                    field.put("size", inputSize.toString());

                    if (value.equals("dropdown")
                            || value.equals("qualdrop_value")
                            || value.equals("list")) {
                        String pairTypeName = getAttribute(nd, PAIR_TYPE_NAME);
                        if (pairTypeName == null) {
                            throw new SAXException("Form " + formName + ", field "
                                    + field.get("dc-element")
                                    + "." + field.get("dc-qualifier")
                                    + " has no name attribute");
                        } else if (!valuePairs.containsKey(pairTypeName)) {
                            throw new SAXException("Form " + formName + ", field "
                                    + field.get("dc-element")
                                    + "." + field.get("dc-qualifier")
                                    + " has wrong name attribute. "
                                    + "Such value-pairs is not defined.");
                        } else {
                            field.put(PAIR_TYPE_NAME, pairTypeName);
                        }
                    }
                } else if (tagName.equals("vocabulary")) {
                    String closedVocabularyString = getAttribute(nd, "closed");
                    field.put("closedVocabulary", closedVocabularyString);
                } else if (tagName.equals("ask-language")) {
                    String flag = getValue(nd);
                    if (flag.toLowerCase().equals("true") || flag.toLowerCase().equals("yes")) {
                        field.put("asklang", "true");
                    } else {
                        field.put("asklang", "false");
                    }
                } else if (tagName.equals("authority")) {
                    String suffix = getValue(nd);
                    String presentation = getAttribute(nd, "presentation");
                    String limit = getAttribute(nd, "limit");
                    String editable = getAttribute(nd, "editable");
                    String closed = getAttribute(nd, "closed");

                    field.put("authority", "true");
                    field.put("aclosed", closed);
                    field.put("aeditable", editable);
                    field.put("choices", limit);
                    field.put("authURL", suffix);
                    field.put("presentation", presentation);
                }
            }
        }
        String missing = null;
        if (field.get("dc-element") == null) {
            missing = "dc-element";
        }
        if (field.get("label") == null) {
            missing = "label";
        }
        if (field.get("input-type") == null) {
            missing = "input-type";
        }
        if (missing != null) {
            String msg = "Required field " + missing + " missing on page " + page + " of form " + formName;
            throw new SAXException(msg);
        }
        String type = (String) field.get("input-type");
        if (type.equals("twobox") || type.equals("qualdrop_value")) {
            String rpt = (String) field.get("repeatable");
            if ((rpt == null)
                    || ((!rpt.equalsIgnoreCase("yes"))
                    && (!rpt.equalsIgnoreCase("true")))) {
                String msg = "The field \'" + field.get("label") + "\' must be repeatable";
                throw new SAXException(msg);
            }
        }
        return new DCInput(field, valuePairs);
    }

    /**
     * Check that this is the only field with the name dc-element.dc-qualifier
     * If there is a duplicate, return an error message, else return null;
     */
//    private boolean checkForDups() {
//        boolean err = true;
//        for (String formName : formDefns.keySet()) {
//            DCInputSetExt form = formDefns.get(formName);
//            if (hasDuplicate(form.getAllFieldsQual())) {
//                log.error("Duplicate field detected in form " + formName + ": ");
//                for (DCInput dd : (Collection<DCInput>) getDuplicate(form.getAllFields())) {
//                    log.error(dd.getFullQualName() + ",");
//                }
//                log.error("\n");
//                err = false;
//            }
//        }
//        return err;
//    }

    /*
     * Returns list of submission types defined for given collection. If no
     * specific rules defined for given collection, list of submission types for
     * default collection will be returned.
     */
    public List<String> getTypesListforCollection(String collectionHandle) {
        if (col2Types.containsKey(collectionHandle)) {
            return col2Types.get(collectionHandle);
        } else {
            return col2Types.get(DEFAULT_COLLECTION);
        }
    }

    /**
     * Returns the set of DC inputs used for a particular collection, or the
     * default set if no inputs defined for the collection
     *
     * @param collectionHandle collection's unique Handle
     * @return DC input set extended
     * @throws DCInputsReaderException if no default set defined
     * @see DCInputSet
     */
    public DCInputSetExt getInputs(String collectionHandle, String documentType)
            throws DCInputsReaderException {
        String ch = (collectionHandle != null && !collectionHandle.equals("")) ? collectionHandle : DEFAULT_COLLECTION;
        if (documentType != null && !"".equals(documentType)) {
            String formName;
            if (getTypesListforCollection(ch).contains(documentType)) {
                formName = type2Forms.get(documentType);
            } else {

                if (!getTypesListforCollection(ch).isEmpty()) {
                    documentType = getTypesListforCollection(ch).get(0);
                    formName = type2Forms.get(documentType);
                } else {
                    throw new DCInputsReaderException(documentType + " is not allowed for collection and no default document type is specified "
                            + collectionHandle + " check [dspace]/config/input-forms-extended.xml");
                }
            }
            if (formName == null) {
                if (type2Forms.size() > 0) {
                    formName = (String) type2Forms.values().toArray()[0];
                } else {
                    throw new DCInputsReaderException("No form designated as default");
                }
            }

            if (formDefns.containsKey(formName)) {
                return formDefns.get(formName);
            } else {
                throw new DCInputsReaderException("Form definition is missing for " + formName);
            }

        } else {
            return null;
        }
    }

    public DCInputSetExt getInputs(String documentType)
            throws DCInputsReaderException {
        String formName = type2Forms.get(documentType);
        if (formName == null) {
            throw new DCInputsReaderException("No form designated as default");
        }

        if (formDefns.containsKey(formName)) {
            return formDefns.get(formName);
        } else {
            throw new DCInputsReaderException("Form definition is missing for " + formName);
        }
    }

    /**
     * Return the number of pages the inputs span for a desginated collection
     *
     * @param collectionHandle collection's unique Handle
     * @return number of pages of input
     * @throws DCInputsReaderException if no default set defined
     */
    public int getNumberInputPages(String collectionHandle, String docType)
            throws DCInputsReaderException {
        if (getInputs(collectionHandle, docType) != null) {
            return getInputs(collectionHandle, docType).getNumberPages();
        } else {
            return 0;
        }
    }

    public Iterator getPairsNameIterator() {
        return valuePairs.keySet().iterator();
    }

    public List getPairs(String name) {
        return (List) valuePairs.get(name);
    }

    /**
     * Process the form-value-pairs section of the XML file. Each element is
     * formed thusly: <value-pairs name="..." dc-term="..."> <pair>
     * <display>displayed name-</display> <storage>stored name</storage> </pair>
     * For each value-pairs element, create a new vector, and extract all the
     * pairs contained within it. Put the display and storage values,
     * respectively, in the next slots in the vector. Store the vector in the
     * passed in hashmap.
     */
    private void processValuePairs(Node e)
            throws SAXException {
        NodeList nl = e.getChildNodes();
        if (nl != null) {
            int len = nl.getLength();
            for (int i = 0; i < len; i++) {
                Node nd = nl.item(i);
                String tagName = nd.getNodeName();

                // process each value-pairs set
                if (tagName.equals("value-pairs")) {
                    String pairsName = getAttribute(nd, PAIR_TYPE_NAME);
                    String dcTerm = getAttribute(nd, "dc-term");
                    if (pairsName == null) {
                        String errString =
                                "Missing name attribute for value-pairs for DC term " + dcTerm;
                        throw new SAXException(errString);

                    }
                    ArrayList pairs = new ArrayList();
                    valuePairs.put(pairsName, pairs);
                    NodeList cl = nd.getChildNodes();
                    int lench = cl.getLength();
                    for (int j = 0; j < lench; j++) {
                        Node nch = cl.item(j);
                        String display = null;
                        String storage = null;

                        if (nch.getNodeName().equals("pair")) {
                            NodeList pl = nch.getChildNodes();
                            int plen = pl.getLength();
                            for (int k = 0; k < plen; k++) {
                                Node vn = pl.item(k);
                                String vName = vn.getNodeName();
                                if (vName.equals("displayed-value")) {
                                    display = getValue(vn);
                                } else if (vName.equals("stored-value")) {
                                    storage = getValue(vn);
                                    if (storage == null) {
                                        storage = "";
                                    }
                                } // ignore any children that aren't 'display' or 'storage'
                            }
                            pairs.add(display);
                            pairs.add(storage);
                        } // ignore any children that aren't a 'pair'
                    }
                } // ignore any children that aren't a 'value-pair'
            }
        }
    }

    private Node getElement(Node nd) {
        NodeList nl = nd.getChildNodes();
        if (nl != null) {
            int len = nl.getLength();
            for (int i = 0; i < len; i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    return n;
                }
            }
        }
        return null;
    }

    private boolean isEmptyTextNode(Node nd) {
        boolean isEmpty = false;
        if (nd.getNodeType() == Node.TEXT_NODE) {
            String text = nd.getNodeValue().trim();
            if (text.length() == 0) {
                isEmpty = true;
            }
        }
        return isEmpty;
    }

    /**
     * Returns the value of the node's attribute named <name>
     */
    private String getAttribute(Node e, String name) {
        NamedNodeMap attrs = e.getAttributes();
        if (attrs != null) {
            int len = attrs.getLength();
            if (len > 0) {
                int i;
                for (i = 0; i < len; i++) {
                    Node attr = attrs.item(i);
                    if (name.equals(attr.getNodeName())) {
                        return attr.getNodeValue().trim();
                    }
                }
            }
        }
        //no such attribute
        return null;
    }

    /**
     * Returns the value found in the Text node (if any) in the node list that's
     * passed in.
     */
    private String getValue(Node nd) {
        NodeList nl = nd.getChildNodes();
        if (nl != null) {
            int len = nl.getLength();
            for (int i = 0; i < len; i++) {
                Node n = nl.item(i);
                short type = n.getNodeType();
                if (type == Node.TEXT_NODE) {
                    return n.getNodeValue().trim();
                }
            }
        }
        // Didn't find a text node
        return null;
    }

    private static <T> List getDuplicate(Collection<T> list) {
        final List<T> duplicatedObjects = new ArrayList<T>();
        Set<T> set = new HashSet<T>() {

            @Override
            public boolean add(T e) {
                if (contains(e)) {
                    duplicatedObjects.add(e);
                }
                return super.add(e);
            }
        };
        for (T t : list) {
            set.add(t);
        }
        return duplicatedObjects;
    }

    private static <T> boolean hasDuplicate(Collection<T> list) {
        if (getDuplicate(list).isEmpty()) {
            return false;
        }
        return true;
    }

    private DCInputGroup getDuplicateGroup(List<DCInputGroup> groups, DCInputGroup g) {
        if (!"".equals(g.getName())) {
            for (DCInputGroup group : groups) {
                if (group.getName().equals(g.getName())) {
                    return group;
                }
            }

            return null;
        } else {
            return null;
        }
    }
}
