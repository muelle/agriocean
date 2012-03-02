/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.submission;

//~--- non-JDK imports --------------------------------------------------------

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;

/**
 * Class representing all DC inputs required for a submission, organized into pages
 * field groups and rows.
 *
 * @author Denys Slipetskyy based on Brian S. Hughes, based on work by Jenny Toves, OCLC
 * @see DCInputSetExt
 * @see DCInput
 * @version $Revision: 3734 $
 */
public class DCInputSetExt {

    /** name of the base (parent) form of this one. Not required. */
    private String baseForm = null;

    /** form hint for the users to be displayed on UI JSP. */
    private String formHint = null;

    /** name of the input set */
    private String formName = null;

    /**
     * the inputs ordered by page and group
     * key - page number (unique)
     * value - List of DCInputGroup
     */
    private HashMap<Integer, List<DCInputGroup>> inputPages = null;

    /** constructor */
    public DCInputSetExt(String formName, String formHint, String baseForm, HashMap pages) {
        this.formName = formName;
        this.formHint = formHint;
        this.baseForm = baseForm;
        inputPages    = new HashMap<Integer, List<DCInputGroup>>();

        for (Map.Entry page : ((HashMap<Integer, List<DCInputGroup>>) pages).entrySet()) {
            inputPages.put((Integer) page.getKey(), (List<DCInputGroup>) page.getValue());
        }
    }

    /**
     * Return the name of the form that defines this input set
     *
     * @return formName         the name of the form
     */
    public String getFormName() {
        return formName;
    }

    public String getFormHint() {
        return this.formHint;
    }

    public String getBaseForm() {
        return this.baseForm;
    }

    /**
     * Return the number of pages in this  input set
     *
     * @return number of pages
     */
    public int getNumberPages() {
        return inputPages.size();
    }

    /**
     * Get all the groups for a page from the form definition.
     *
     * @param  pageNum  desired page within set
     * @return  an array containing the page's displayable groups
     */
    public List<DCInputGroup> getPage(Integer pageNum) {
        if (inputPages.containsKey(pageNum)) {
            return inputPages.get(pageNum);
        } else {
            return null;
        }
    }

    /**
     * Creates deep copy of inputPages.
     */
    public HashMap copyAllPages() {
        HashMap<Integer, List<DCInputGroup>> result = new HashMap<Integer, List<DCInputGroup>>();

        for (Integer pNum : inputPages.keySet()) {
            result.put(pNum, new ArrayList(inputPages.get(pNum)));
        }

        return result;
    }

    /**
     * Get all the iput fields for a page from the form definition.
     *
     * @param  pageNum  desired page within set
     * @return  an array containing the page's DCInput fields
     */
    public List<DCInput> getPageInputs(Integer pageNum) {
        List<DCInput> result = new ArrayList<DCInput>();

        for (DCInputGroup group : getPage(pageNum)) {
            result.addAll(group.getAllInputs());
        }

        return result;
    }

    /**
     * Get form's pages.
     *
     * @return all form's pages
     */
    public HashMap<Integer, List<DCInputGroup>> getPages() {
        return inputPages;
    }

    private boolean hasPage(int pageNum) {
        return inputPages.containsKey(pageNum);
    }

    /**
     *  Get list of all fields from all pages of the form
     *
     * @return <CODE>List<DCInput></CODE>
     */
    public List<DCInput> getAllFields() {
        List<DCInput> result = new ArrayList<DCInput>();

        // iterate through all pages
        for (Map.Entry page : inputPages.entrySet()) {

            // iterate through fields groups on a given page
            for (DCInputGroup group : (List<DCInputGroup>) page.getValue()) {

                // iterate through rows in a given group
                for (Map.Entry row : group.getRows().entrySet()) {
                    result.addAll((List<DCInput>) row.getValue());
                }
            }
        }

        return result;
    }

    /**
     *  Get list of all fields names
     *
     * @return <CODE>List<String></CODE>
     */
    public List<String> getAllFieldsQual() {
        List<String> result = new ArrayList<String>();

        // iterate through all pages
        for (Map.Entry page : inputPages.entrySet()) {

            // iterate through fields groups on a given page
            for (DCInputGroup group : (List<DCInputGroup>) page.getValue()) {

                // iterate through rows in a given group
                for (Map.Entry row : group.getRows().entrySet()) {
                    for (DCInput field : (List<DCInput>) row.getValue()) {
                        result.add(field.getFullQualName());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Does this set of inputs include an alternate title field?
     *
     * @return true if the current set has an alternate title field
     */
    public boolean isDefinedMultTitles() {
        return isFieldPresent("title.alternative");
    }

    /**
     * Does this set of inputs include the previously published fields?
     *
     * @return true if the current set has all the prev. published fields
     */
    public boolean isDefinedPubBefore() {
        return (isFieldPresent("date.issued") && isFieldPresent("identifier.citation")
                && isFieldPresent("publisher.null"));
    }

    /**
     * Does the current input set define the named field?
     * Scan through every field in every page of the input set
     *
     * @return true if the current set has the named field
     */
    public boolean isFieldPresent(String fieldName) {
        for (DCInput field : this.getAllFields()) {
            String fullName = field.getElement() + "." + field.getQualifier();

            if (fullName.equals(fieldName)) {
                return true;
            }
        }

        return false;
    }

    /*
     *   For DEBUGing purposes  - put structure of the form to the log
     */
    public void logInputFields() {

        // log4j logger
        Logger log = Logger.getLogger(DCInputSetExt.class);

        log.info("**********");
        log.info("Form name = " + formName);
        log.info("Base form name = " + baseForm);
        log.info("Total fields = " + this.getAllFields().size());
        log.info("");

        for (Map.Entry page : inputPages.entrySet()) {
            log.info("page = " + page.getKey().toString());

            for (DCInputGroup grp : (List<DCInputGroup>) page.getValue()) {
                log.info("group = " + grp.getName());

                for (int j = 0; j < grp.getRowsCount(); j++) {
                    log.info("row = " + j);
                    log.info("----------");

                    List<DCInput> row = grp.getRow(j);

                    for (DCInput field : row) {
                        log.info(field.getFullQualName());
                    }

                    log.info("---------");
                }
            }
        }

        log.info("**********");
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
