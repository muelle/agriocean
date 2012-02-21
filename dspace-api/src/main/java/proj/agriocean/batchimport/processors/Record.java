/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * @author Christof Verdonck (updated by Denys Slipetskyy)
 * @version aod1.0
 */
package proj.agriocean.batchimport.processors;
//Imports
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

public class Record implements RecordInface {
//ICV
    // The mapping between document types of the metadata import format
    // and the interal dspace Dublin Core document types
    // The map is passed in by the Processor.
    protected Properties doctypeMap = null;

    protected StringBuffer recordbuffer;
    protected String field = "";
    protected String buffer = "";
    protected boolean isNewField = true;
    protected String schema = "dc";
    protected String element = "";
    protected String qualifier = "";
    protected String language = "";
    protected Context context;
    protected WorkspaceItem workspaceitem;
    protected Item dsitem;

//DC
    public Record(){}
    public Record(int teller,Context context,Collection collection, Properties doctypeMap){
        this.doctypeMap = doctypeMap;
	recordbuffer = new StringBuffer();//System.out.println("\tNew Record "+teller);
        try {
            workspaceitem = WorkspaceItem.create(context,collection,true);
            workspaceitem.setMultipleTitles(true);
            workspaceitem.setPublishedBefore(true);
            dsitem = workspaceitem.getItem();
            workspaceitem.update();
        } catch (SQLException sqle) {
		Logger.getLogger(Record.class.getName()).error("Record : Exception SQL ",sqle);
        } catch (IOException ioe) {
		Logger.getLogger(Record.class.getName()).error("Record : Exception IO ",ioe);
        } catch (AuthorizeException ae) {
		Logger.getLogger(Record.class.getName()).error("Record : Exception Authorize ",ae);
        }
    }
    @Override
    public void addField(String lijn) {
	setBufferedItem();
        isNewField = true;
        buffer = "";
        field = lijn;
    }
    @Override
    public void appendField(String lijn) {
    }
//wrapItemItemInHTML
    @Override
    public String wrapItemInHTML(String stritem){
	stritem = stritem.trim();
	//Logger.getLogger(Record.class.getName()).log(Level.INFO,stritem);
	String item_buffer = stritem.substring(stritem.indexOf("=")+2).trim();
	if(qualifier.equals("")){
                // try translating value for metadatafield dc.type into a value known in the current dspace configuration
                if (element.equals("type")){
                    item_buffer = getMappedDocumentType(item_buffer);
                    // hack: for the summary to show the correct imported metadatavalues, return value must also change
                    stritem = stritem.substring(0,stritem.indexOf("=")+2) + item_buffer;
                }
		dsitem.addMetadata(schema,element,null,language,item_buffer);
	}
	else{
		dsitem.addMetadata(schema,element,qualifier,language,item_buffer);
	}
        return "\n<tr><td>"+stritem+"</td></tr>";
    }
    @Override
    public StringBuffer getRecord() {
        setMetaData(field);
        return recordbuffer;
    }
    @Override
    public void setMetaData(String stritem) {
    }
    public void setTypeMetaData(String formType) {
	element = "type";
	qualifier = "";
	setMetaData(formType);
    }


    /**
     * Maps a document type value to a type known
     * by the current dspace configuration. The mapping is specified per metadata import format,
     * i.e., per record type. It is passed in by the processor processing this type of records.
     * @param doctypeValue the document type as specified by the import format
     * @return the document type that should be stored in the DC field dc.type, instead of the
     *  given doctypeValue. When no mapping is found
     *          for the given doctypeValue, the doctypeValue itself is returned.
     */
    protected String getMappedDocumentType(String doctypeValue){
        if (doctypeMap == null)
            return doctypeValue;

        if (!doctypeMap.containsKey(doctypeValue)
                || doctypeMap.getProperty(doctypeValue) == null
                || doctypeMap.getProperty(doctypeValue).equals(""))
            return doctypeValue;

        return doctypeMap.getProperty(doctypeValue);

    }

    public void updateItem(){
        try {
            workspaceitem.update();
        } catch (SQLException sqle) {
            Logger.getLogger(Record.class.getName()).error("Update Item > EXCEPTION SQL : WorkSpaceItem "+sqle.getMessage());
        } catch (IOException ioe) {
            Logger.getLogger(Record.class.getName()).error("Update Item > EXCEPTION IO : WorkSpaceItem " + ioe.getLocalizedMessage());
        } catch (AuthorizeException ae) {
            Logger.getLogger(Record.class.getName()).error("Update Item > EXCEPTION Authorize : WorkSpaceItem " + ae.getLocalizedMessage());
        }
    }
//set Dublin Core MetadatElements
    @Override
    public void setDCEntities(String element,String qualifier,String language) {
        this.schema = "dc";
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
    }
//setSubType
    public String getSubType(String field) {
        return "";
    }
//setBufferdItem
    public void setBufferedItem() {
    }
}