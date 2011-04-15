/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dspace.app.util.DCInput;

/**
 * This class represents group of DCInput fields grouped in rows.
 * @author Denys Slipetskyy
 * @version $Revision: 1 $
 * @since 1.6
 * @see DCInput
 * @see DCIputSet
 * @see DCInputsReader
 */
public class DCInputGroup {

    /**
     * user hint
     */
    private String hint = null;

    /**
     * Label of the group 
     */
    private String label = null;

    /**
     * Name of the group - it is like ID of the group, but not necessary unique.
     * In situation then some group on the same page of basic form have the same
     * name, it will be replaced completely by this group.
     * @see [dspace]/config/input-forms-extended.xml
     */
    private String name = null;

    /**
     * Rows of fields. Each group can contain zero or more rows. Row in this
     * case is a combination of DCInput fields placed in one row  on the web page.
     * key - integer number of the row, value - List of DCInput objects;
     * It is logical organization for UI only.
     * @see [dspace]/config/input-forms-extended.xml
     */
    private HashMap<Integer, List<DCInput>> rows;


    //=============== Methods =======================================
    
    public DCInputGroup (String name)
    {
        if (name == null)
            this.name="";
        else
            this.name=name;
        
        rows = new HashMap<Integer, List<DCInput>>();
    }

    /**
     * @return the hint for the group
     */
    public String getHint() {
        return hint;
    }

    /**
     * @param set the hint for the group
     */
    public void setHint(String hint) {
        this.hint = hint == null ? "": hint;
    }

    /**
     * @return the group label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param set group label
     */
    public void setLabel(String label) {
        this.label = label == null ?"": label;
    }

    /**
     * @return the group name
     */
    public String getName() {
        return name;
    }

    /**
     * @param set the group name
     */
    public void setName(String name) {
        this.name = name;
    }

    public int getRowsCount()
    {
        return rows.size();
    }

    /**
     * @return the rows of the group. List of DCInput objects.
     */
    public HashMap<Integer, List<DCInput>> getRows() {
        return rows;
    }

    public List<DCInput> getAllInputs()
    {
        List<DCInput> result = new ArrayList<DCInput>();

        for(Map.Entry row: rows.entrySet())
        {
            result.addAll((List<DCInput>)row.getValue());
        }

        return result;
    }

    /**
     * @param the rows of DCInput fields to set
     */
    public void setRows(HashMap<Integer, List<DCInput>> rows) {
        this.rows = rows;
    }

    public void mergeRows(HashMap<Integer, List<DCInput>> rows)
    {
        int i = this.rows.size();

        for(Map.Entry row: rows.entrySet())
        {
            this.rows.put(i, (List<DCInput>)row.getValue());
            ++i;
        }
    }

    public boolean hasRows ()
    {
        return rows.isEmpty();
    }

    /**
     * Returns a List of DCInput objects for row with specified index. If index
     * is out of range returns null.
     * @param index of required row
     * @return List of DCInput objects
     */
    public List<DCInput> getRow(int index)
    {
        return index < rows.size() ? rows.get(index): null;
    }

    /**
     * Puts a new row into the map
     * @param newRow - List of DCInput objects
     */
    public void setRow (List<DCInput> newRow)
    {
        rows.put(rows.size(), newRow);
    }
}
