/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;

/**
 * Utility class to read and write CSV files
 *
 * **************
 * Important Note
 * **************
 *
 * This class has been made serializable, as it is stored in a Session.
 * Is it wise to:
 *    a) be putting this into a user's session?
 *    b) holding an entire CSV upload in memory?
 *
 * @author Stuart Lewis
 */
public class DSpaceCSV implements Serializable
{
    /** The headings of the CSV file */
    private List<String> headings;

    /** An array list of CSV lines */
    private List<DSpaceCSVLine> lines;

    /** A counter of how many CSV lines this object holds */
    private int counter;

    /** The value separator (defaults to double pipe '||') */
    protected static String valueSeparator;

    /** The value separator in an escaped form for using in regexs */
    protected static String escapedValueSeparator;

    /** The field separator (defaults to comma) */
    protected static String fieldSeparator;

    /** The field separator in an escaped form for using in regexs */
    protected static String escapedFieldSeparator;

    /** Whether to export all metadata such as handles and provenance information */
    private boolean exportAll;

    /** A list of metadata elements to ignore */
    private Map<String, String> ignore;


    /**
     * Create a new instance of a CSV line holder
     *
     * @param exportAll Whether to export all metadata such as handles and provenance information
     */
    public DSpaceCSV(boolean exportAll)
    {
        // Initialise the class
        init();

        // Store the exportAll setting
        this.exportAll = exportAll;
    }

    /**
     * Create a new instance, reading the lines in from file
     *
     * @param f The file to read from
     * @param c The DSpace Context
     *
     * @throws Exception thrown if there is an error reading or processing the file
     */
    public DSpaceCSV(File f, Context c) throws Exception
    {
        // Initialise the class
        init();

        // Open the CSV file
        BufferedReader input = null;
        try
        {
            input = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));

            // Read the heading line
            String head = input.readLine();
            String[] headingElements = head.split(escapedFieldSeparator);
            for (String element : headingElements)
            {
                // Remove surrounding quotes if there are any
                if ((element.startsWith("\"")) && (element.endsWith("\"")))
                {
                    element = element.substring(1, element.length() - 1);
                }

                // Store the heading
                if ("collection".equals(element))
                {
                    // Store the heading
                    headings.add(element);
                }
                else if (!"id".equals(element))
                {
                    // Verify that the heading is valid in the metadata registry
                    String[] clean = element.split("\\[");
                    String[] parts = clean[0].split("\\.");
                    String metadataSchema = parts[0];
                    String metadataElement = parts[1];
                    String metadataQualifier = null;
                    if (parts.length > 2) {
                        metadataQualifier = parts[2];
                    }

                    // Check that the scheme exists
                    MetadataSchema foundSchema = MetadataSchema.find(c, metadataSchema);
                    if (foundSchema == null) {
                        throw new MetadataImportInvalidHeadingException(clean[0],
                                                                        MetadataImportInvalidHeadingException.SCHEMA);
                    }

                    // Check that the metadata element exists in the schema
                    int schemaID = foundSchema.getSchemaID();
                    MetadataField foundField = MetadataField.findByElement(c, schemaID, metadataElement, metadataQualifier);
                    if (foundField == null) {
                        throw new MetadataImportInvalidHeadingException(clean[0],
                                                                        MetadataImportInvalidHeadingException.ELEMENT);
                    }

                    // Store the heading
                    headings.add(element);
                }
            }

            // Read each subsequent line
            StringBuilder lineBuilder = new StringBuilder();
            String lineRead;

            while ((lineRead = input.readLine()) != null)
            {
                if (lineBuilder.length() > 0) {
                    // Already have a previously read value - add this line
                    lineBuilder.append("\n").append(lineRead);

                    // Count the number of quotes in the buffer
                    int quoteCount = 0;
                    for (int pos = 0; pos < lineBuilder.length(); pos++) {
                        if (lineBuilder.charAt(pos) == '"') {
                            quoteCount++;
                        }
                    }

                    if (quoteCount % 2 == 0) {
                        // Number of quotes is a multiple of 2, add the item
                        addItem(lineBuilder.toString());
                        lineBuilder = new StringBuilder();
                    }
                } else if (lineRead.indexOf('"') > -1) {
                    // Get the number of quotes in the line
                    int quoteCount = 0;
                    for (int pos = 0; pos < lineRead.length(); pos++) {
                        if (lineRead.charAt(pos) == '"') {
                            quoteCount++;
                        }
                    }

                    if (quoteCount % 2 == 0) {
                        // Number of quotes is a multiple of 2, add the item
                        addItem(lineRead);
                    } else {
                        // Uneven quotes - add to the buffer and leave for later
                        lineBuilder.append(lineRead);
                    }
                } else {
                    // No previously read line, and no quotes in the line - add item
                    addItem(lineRead);
                }
            }
        }
        finally
        {
            if (input != null)
            {
                input.close();
            }
        }
    }

    /**
     * Initialise this class with values from dspace.cfg
     */
    private void init()
    {
        // Set the value separator
        setValueSeparator();

        // Set the field separator
        setFieldSeparator();

        // Create the headings
        headings = new ArrayList<String>();

        // Create the blank list of items
        lines = new ArrayList<DSpaceCSVLine>();

        // Initialise the counter
        counter = 0;

        // Set the metadata fields to ignore
        ignore = new HashMap<String, String>();
        String toIgnore = ConfigurationManager.getProperty("bulkedit.ignore-on-export");
        if ((toIgnore == null) || ("".equals(toIgnore.trim())))
        {
            // Set a default value
            toIgnore = "dc.date.accessioned, dc.date.available, " +
                       "dc.date.updated, dc.description.provenance";
        }
        String[] toIgnoreArray = toIgnore.split(",");
        for (String toIgnoreString : toIgnoreArray)
        {
            if (!"".equals(toIgnoreString.trim()))
            {
                ignore.put(toIgnoreString.trim(), toIgnoreString.trim());
            }
        }
    }

    /**
     * Set the value separator for multiple values stored in one csv value.
     *
     * Is set in dspace.cfg as bulkedit.valueseparator
     *
     * If not set, defaults to double pipe '||'
     */
    private void setValueSeparator()
    {
        // Get the value separator
        valueSeparator = ConfigurationManager.getProperty("bulkedit.valueseparator");
        if ((valueSeparator != null) && (!"".equals(valueSeparator.trim())))
        {
            valueSeparator = valueSeparator.trim();
        }
        else
        {
            valueSeparator = "||";
        }

        // Now store the escaped version
        Pattern spchars = Pattern.compile("([\\\\*+\\[\\](){}\\$.?\\^|])");
        Matcher match = spchars.matcher(valueSeparator);
        escapedValueSeparator = match.replaceAll("\\\\$1");
    }

    /**
     * Set the field separator use to separate fields in the csv.
     *
     * Is set in dspace.cfg as bulkedit.fieldseparator
     *
     * If not set, defaults to comma ','.
     *
     * Special values are 'tab', 'hash' and 'semicolon' which will
     * get substituted from the text to the value.
     */
    private void setFieldSeparator()
    {
        // Get the value separator
        fieldSeparator = ConfigurationManager.getProperty("bulkedit.fieldseparator");
        if ((fieldSeparator != null) && (!"".equals(fieldSeparator.trim())))
        {
            fieldSeparator = fieldSeparator.trim();
            if ("tab".equals(fieldSeparator))
            {
                fieldSeparator = "\t";
            }
            else if ("semicolon".equals(fieldSeparator))
            {
                fieldSeparator = ";";
            }
            else if ("hash".equals(fieldSeparator))
            {
                fieldSeparator = "#";
            }
            else
            {
                fieldSeparator = fieldSeparator.trim();
            }
        }
        else
        {
            fieldSeparator = ",";
        }

        // Now store the escaped version
        Pattern spchars = Pattern.compile("([\\\\*+\\[\\](){}\\$.?\\^|])");
        Matcher match = spchars.matcher(fieldSeparator);
        escapedFieldSeparator = match.replaceAll("\\\\$1");
    }

    /**
     * Add a DSpace item to the CSV file
     *
     * @param i The DSpace item
     *
     * @throws Exception if something goes wrong with adding the Item
     */
    public final void addItem(Item i) throws Exception
    {
        // Create the CSV line
        DSpaceCSVLine line = new DSpaceCSVLine(i.getID());

        // Add in owning collection
        String owningCollectionHandle = i.getOwningCollection().getHandle();
        line.add("collection", owningCollectionHandle);

        // Add in any mapped collections
        Collection[] collections = i.getCollections();
        for (Collection c : collections)
        {
            // Only add if it is not the owning collection
            if (!c.getHandle().equals(owningCollectionHandle))
            {
                line.add("collection", c.getHandle());
            }
        }

        // Populate it
        DCValue md[] = i.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (DCValue value : md)
        {
            // Get the key (schema.element)
            String key = value.schema + "." + value.element;

            // Add the qualifier if there is one (schema.element.qualifier)
            if (value.qualifier != null)
            {
                key = key + "." + value.qualifier;
            }

            // Add the language if there is one (schema.element.qualifier[langauge])
            //if ((value.language != null) && (!"".equals(value.language)))
            if (value.language != null)
            {
                key = key + "[" + value.language + "]";
            }

            // Store the item
            if (exportAll || okToExport(value))
            {
                line.add(key, value.value);
                if (!headings.contains(key))
                {
                    headings.add(key);
                }
            }
        }
        lines.add(line);
        counter++;
    }

    /**
     * Add an item to the CSV file, from a CSV line of elements
     *
     * @param line The line of elements
     * @throws Exception Thrown if an error occurs when adding the item
     */
    public final void addItem(String line) throws Exception
    {
        // Check to see if the last character is a field separator, which hides the last empy column
        boolean last = false;
        if (line.endsWith(fieldSeparator))
        {
            // Add a space to the end, then remove it later
            last = true;
            line += " ";
        }

        // Split up on field separator
        String[] parts = line.split(escapedFieldSeparator);
        ArrayList<String> bits = new ArrayList<String>();
        bits.addAll(Arrays.asList(parts));

        // Merge parts with embedded separators
        boolean alldone = false;
        while (!alldone)
        {
            boolean found = false;
            int i = 0;
            for (String part : bits)
            {
                int bitcounter = part.length() - part.replaceAll("\"", "").length();
                if ((part.startsWith("\"")) && ((!part.endsWith("\"")) || ((bitcounter & 1) == 1)))
                {
                    found = true;
                    String add = bits.get(i) + fieldSeparator + bits.get(i + 1);
                    bits.remove(i);
                    bits.add(i, add);
                    bits.remove(i + 1);
                    break;
                }
                i++;
            }
            alldone = !found;
        }

        // Deal with quotes around the elements
        int i = 0;
        for (String part : bits)
        {
            if ((part.startsWith("\"")) && (part.endsWith("\"")))
            {
                part = part.substring(1, part.length() - 1);
                bits.set(i, part);
            }
            i++;
        }

        // Remove embedded quotes
        i = 0;
        for (String part : bits)
        {
            if (part.contains("\"\""))
            {
                part = part.replaceAll("\"\"", "\"");
                bits.set(i, part);
            }
            i++;
        }

        // Add elements to a DSpaceCSVLine
        String id = parts[0].replaceAll("\"", "");
        DSpaceCSVLine csvLine;

        // Is this an existing item, or a new item (where id = '+')
        if ("+".equals(id))
        {
            csvLine = new DSpaceCSVLine();
        }
        else
        {
            try
            {
                csvLine = new DSpaceCSVLine(Integer.parseInt(id));
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Invalid item identifier: " + id);
                System.err.println("Please check your CSV file for information. " +
                                   "Item id must be numeric, or a '+' to add a new item");
                throw(nfe);
            }
        }

        // Add the rest of the parts
        i = 0;
        for (String part : bits)
        {
            if (i > 0)
            {
                // Is this a last empty item?
                if ((last) && (i == headings.size()))
                {
                    part = "";
                }

                // Make sure we register that this column was there
                csvLine.add(headings.get(i - 1), null);
                String[] elements = part.split(escapedValueSeparator);
                for (String element : elements)
                {
                    if ((element != null) && (!"".equals(element)))
                    {
                        csvLine.add(headings.get(i - 1), element);
                    }
                }
            }
            i++;
        }
        lines.add(csvLine);
        counter++;
    }

    /**
     * Get the lines in CSV holders
     *
     * @return The lines
     */
    public final List<DSpaceCSVLine> getCSVLines()
    {
        // Return the lines
        return lines;
    }

    /**
     * Get the CSV lines as an array of CSV formatted strings
     *
     * @return the array of CSV formatted Strings
     */
    public final String[] getCSVLinesAsStringArray()
    {
        // Create the headings line
        String[] csvLines = new String[counter + 1];
        csvLines[0] = "id" + fieldSeparator + "collection";
        Collections.sort(headings);
        for (String value : headings)
        {
            csvLines[0] = csvLines[0] + fieldSeparator + value;
        }

        Iterator<DSpaceCSVLine> i = lines.iterator();
        int c = 1;
        while (i.hasNext())
        {
            csvLines[c++] = i.next().toCSV(headings);
        }

        return csvLines;
    }

    /**
     * Save the CSV file to the given filename
     *
     * @param filename The filename to save the CSV file to
     *
     * @throws IOException Thrown if an error occurs when writing the file
     */
    public final void save(String filename) throws IOException
    {
        // Save the file
        BufferedWriter out = new BufferedWriter(
                             new OutputStreamWriter(
                             new FileOutputStream(filename), "UTF-8"));
        for (String csvLine : getCSVLinesAsStringArray()) {
            out.write(csvLine + "\n");
        }
        out.flush();
        out.close();
    }

    /**
     * Is it Ok to export this value? When exportAll is set to false, we don't export
     * some of the metadata elements.
     *
     * The list can be configured via the key bulkedit.ignore-on-export in dspace.cfg
     *
     * @param md The DCValue to examine
     * @return Whether or not it is OK to export this element
     */
    private final boolean okToExport(DCValue md)
    {
        // First check the metadata format, and K all non DC elements
        if (!"dc".equals(md.schema))
        {
            return true;
        }

        // Now compare with the list to ignore
        String key = md.schema + "." + md.element;
        if (md.qualifier != null)
        {
            key += "." + md.qualifier;
        }
        if (ignore.get(key) != null) {
            return false;
        }

        // Must be OK, so don't ignore
        return true;
    }

    /**
     * Get the headings used in this CSV file
     *
     * @return The headings
     */
    public List<String> getHeadings()
    {
        return headings;
    }

    /**
     * Return the csv file as one long formatted string
     *
     * @return The formatted String as a csv
     */
    public final String toString()
    {
        // Return the csv as one long string
        StringBuffer csvLines = new StringBuffer();
        String[] lines = this.getCSVLinesAsStringArray();
        for (String line : lines)
        {
            csvLines.append(line).append("\n");
        }
        return csvLines.toString();
    }
}
