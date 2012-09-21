/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.content.export;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.export.LatexFieldFormatter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 *
 * @author 
 */
public class DC2BibTeXExport implements IDCTextBasedExport {
    
    private static final Logger log = Logger.getLogger(DC2BibTeXExport.class);
    
    // name of the config file in [dspace-dir]/modules that contains mapping between DC type and BibTeX entry type
    private static final String MODNAME = "export-bibtex";
    
    static
    {
        try{
            // set JabRef preferences (default)
            if (Globals.prefs == null){
                Globals.prefs = JabRefPreferences.getInstance();
            }
        } catch (Exception e){
            log.error("An error occurred while initializing the BibTeX export class.", e);
        }
    }
    
    /**
     * Translates a DSpace type - subtype pair into a target BibTeX type.
     * @param dcType the source DSpace type
     * @param dcSubtype the source DSpace subtype. If it is null, it is ignored.
     * @return the target BibTeX type
     */
    public String mappedBibTeXType(String dcType, String dcSubtype){
        if (dcType == null)
            return null;
        
        String key = "bibtextarget.of." + dcType.toLowerCase() + (dcSubtype!=null? "."+dcSubtype.toLowerCase() : "");
        String target = ConfigurationManager.getProperty(MODNAME, key);
        if (target!=null)
            return target;
            
        // if no mapping is found for (dcType,dcSubtype), the mapping for dcType is used.
        target = ConfigurationManager.getProperty(MODNAME, "bibtextarget.of." + dcType.toLowerCase());
        if (target!=null)
            return target;
        
        // if still no mapping was found, look for a default bibtex target
        log.debug("key " + key + " not found in module " + MODNAME + ". Will look for default.bibtextarget.");
        return ConfigurationManager.getProperty(MODNAME, "default.bibtextarget");
    }
    
    /**
     * Returns a list of fields needed to be filled to create a BibTeX entry of the given type.
     * @param bibtexType
     * @return 
     */
    public List<String> targetFields(String bibtexType){
        if (bibtexType==null)
            return null;

        String key = "fields.of." + bibtexType;
        
        if (ConfigurationManager.getProperty(MODNAME, key)==null){
            log.warn("No target fields for BibTeX type " + bibtexType + " could be found. Was looking for key " + 
                    key + " in the file [dspace-dir]/config/modules/" + MODNAME + ".cfg");
            return null;
        }
        
        String[] fields = ConfigurationManager.getProperty(MODNAME, key).split("\\s*,\\s*");
        if (fields==null || fields.length==0){
            log.warn("No target fields for BibTeX type " + bibtexType + " are registered. Inspected key " + 
                    key + " in the file [dspace-dir]/config/modules/" + MODNAME + ".cfg with value " +
                    ConfigurationManager.getProperty(MODNAME, key));
            return null;
        }
        else{
            log.info("Registered target fields (" + StringUtils.join(fields, "; ") + ") for BibTeX type " + bibtexType);
            return Arrays.asList(fields);
        }
        
    }

    /**
     * Returns the bibliographic reference in Bibtex format for the given item.
     * Customizable in subclasses by overriding the mappedBibTeXType(dcType) and targetFields(bibtexType) functions.
     * @param item
     * @return 
     * 
     */
    @Override
    public String export(Item item) throws NoDCTypeException, NoTargetBibtexTypeException, TargetBibtexTypeInitException, NoTargetBibtexFieldsException {
        DCValue[] typeVals = item.getMetadata("dc.type");
        if (typeVals==null || typeVals.length==0 || typeVals[0].value==null){
            log.error("item with ID=" + item.getID() + " does not have a value for dc.type. Could not export item to BibTeX.");
            throw new NoDCTypeException("item with ID=" + item.getID() + " does not have a value for dc.type. Could not export item to BibTeX.");
        }

        String dcType = item.getMetadata("dc.type")[0].value;
        String dcSubtype = getFieldsFirstValue(item, "dc.type.specified");
        String bibtexType = mappedBibTeXType(dcType, dcSubtype);
        if (bibtexType==null){
            log.error("item with ID=" + item.getID() + " and type=" + dcType + ((dcSubtype!=null)? " subtype=" +dcSubtype : "" ) + " could not be exported because no registered target BibTeX type could be found.");
            throw new NoTargetBibtexTypeException("item with ID=" + item.getID() + " and type=" + dcType + ((dcSubtype!=null)? " subtype=" +dcSubtype : "" ) + " could not be exported because no registered target BibTeX type could be found.");
        }

        BibtexEntry be = new BibtexEntry("",BibtexEntryType.getType(bibtexType));
        if (be==null){
            log.error("item with ID=" + item.getID() + " and type=" + dcType + " could not be exported because the registered target BibTeX type " + bibtexType + " could not be initialized.");
            throw new TargetBibtexTypeInitException("item with ID=" + item.getID() + " and type=" + dcType + " could not be exported because the registered target BibTeX type " + bibtexType + " could not be initialized.");
        }

        List<String> fields = targetFields(bibtexType);
        if (fields==null || fields.isEmpty()){
            log.error("item with ID=" + item.getID() + " and type=" + dcType + " could not be exported because the registered target BibTeX type " + bibtexType + " has no registered fields.");
            throw new NoTargetBibtexFieldsException("item with ID=" + item.getID() + " and type=" + dcType + " could not be exported because the registered target BibTeX type " + bibtexType + " has no registered fields.");
        }
        
        for (Iterator<String> it = fields.iterator(); it.hasNext();)
        {
            String bibtexField = it.next();
            setBibtexField(be, bibtexField, item);
        }
        StringWriter out = new StringWriter();
        try {
            be.write(out, new LatexFieldFormatter(), true);
        } catch (IOException ex) {
            log.error("item with ID=" + item.getID() + " could not be exported because of an I/O error." ,ex);
        }
        return out.toString();
            
    }
    
    /**
     * Gets the definition of a target bibtex field, relative to the source type and subtype of a DSpace item.
     * The value for the first existing key, in the order given here, is returned:
     * 1. definition.of.[bibtexField].[dcType].[dcSubtype]
     * 2. definition.of.[bibtexField].[dcType]
     * 3. definition.of.[bibtexField]
     * @param bibtexField
     * @param dcType if null, it is ignored
     * @param dcSubtype if null, it is ignored
     * @return the value of the key in the config file that defines the structure of the bibtex field
     */
    private String definitionOf(String bibtexField, String dcType, String dcSubtype){
        // keys to try
        ArrayList<String> keys = new ArrayList<String>();
        
        if (dcSubtype!=null && dcType!=null)
            keys.add("definition.of." + bibtexField + "." + dcType + "." + dcSubtype);
        if (dcType!=null)
            keys.add("definition.of." + bibtexField + "." + dcType);
        keys.add("definition.of." + bibtexField);

        for (String key : keys){
            if (ConfigurationManager.getProperty(MODNAME, key) != null)
                return ConfigurationManager.getProperty(MODNAME, key);
        }
        return null;
    }
    
    
    /**
     * sets the given field in the given bibtex entry based on the configuration file. The data is read from the given item.
     * @param be
     * @param bibtexField
     * @param item 
     */
    private void setBibtexField(BibtexEntry be, String bibtexField, Item item)
    {
        String definitionLine = definitionOf(bibtexField, getFieldsFirstValue(item, "dc.type"), getFieldsFirstValue(item, "dc.type.specified"));
        
        if (definitionLine==null){
            log.warn("left out target BibTeX field " + bibtexField + " while exporting item with ID=" + item.getID() + ". No definition for the field was found.");
            return;
        }
        
        String[] parts = definitionLine.split("\\s*@@\\s*");
        if (parts==null || parts.length>2)
        {
            log.warn("left out target BibTeX field " + bibtexField + " while exporting item with ID=" + item.getID() + ". Could not parse value for field definition.of." + bibtexField + ".");
            return;
        }
        
        if (parts.length==2){ // OPERATION and OPERAND
            String operation = parts[0];
            String operand = parts[1];

            if (operation.startsWith("JOIN")){
                Pattern p = Pattern.compile("JOIN\\((.*)\\)\\s*");
                Matcher m = p.matcher(operation);
                String joinStr = "; ";
                if (m.matches()){
                    joinStr = m.group(1);
                    log.debug("will use string '" + joinStr + "' to join values of " + operand + " to create bibtex field " + bibtexField);
                }else
                    log.debug("will use string '" + joinStr + "' to join values of " + operand + " to create bibtex field " + bibtexField);

                ArrayList<String> mdvals = new ArrayList<String>();
                for (DCValue val: item.getMetadata(operand))
                    mdvals.add(val.value);
                if (mdvals.isEmpty())
                {
                    log.info("could not find values for DC field " + operand + " to create bibtex field " + bibtexField + " of item with ID=" + item.getID());
                    return;
                }
                be.setField(bibtexField, StringUtils.join(mdvals.iterator(), joinStr));
            } else // operation was not recognized. Currently, only JOIN is supported
                log.warn("Operation " + operation + " in line " + definitionLine + " was not recognized. Field " + bibtexField + " was excluded from export of item with ID=" + item.getID());  
              
        }
        else { // definition is list of dc fields and string literals enclosed by @-sign
            StringBuilder sb = new StringBuilder();
            String[] concats = definitionLine.split("\\s+");
            for (String c : concats){
                if (c.startsWith("@") && c.endsWith("@")){ //literal
                    sb.append(c.substring(1, c.length()-1));
                } else{
                    String fieldValue = getFieldsFirstValue(item, c);
                    if (fieldValue==null)
                    {
                        log.info("could not find value for DC field " + c + " to create bibtex field " + bibtexField + " of item with ID=" + item.getID());
                        return;
                    }
                    sb.append(fieldValue);
                }
            }
            be.setField(bibtexField, sb.toString());
        }
    }
    
    /**
     * Returns the first value an item has for a given metadata field. If no such value exists, null is returned.
     * @param item
     * @param mdString
     * @return 
     */
    private static String getFieldsFirstValue(Item item, String mdString){
        DCValue[] values = item.getMetadata(mdString);
        if (values!=null && values.length > 0 && values[0].value != null)
            return values[0].value;
        else
            return null;
    }
}
