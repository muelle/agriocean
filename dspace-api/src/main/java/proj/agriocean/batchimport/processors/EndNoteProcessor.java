/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * @author Christof Verdonck
 * @version aod1.0
 */
package proj.agriocean.batchimport.processors;

import java.io.File;
import org.dspace.content.Collection;
import org.dspace.core.Context;

/**
 *
 * @author 
 */
public class EndNoteProcessor extends MetadataProcessor {

    private static final String METADATAFIELDTAG_START_TEXT = "%";
    private int teller = 1;

    @Override
    protected void setNewRecord() {
        writeCurrentRecord();

        record = new EndNoteRecord(teller, context,collection,doctypeMap);
        setOutput("<table>");
        teller++;
    }

    @Override
    protected void handleFields(String lijn) {
        if (! lijn.startsWith(METADATAFIELDTAG_START_TEXT) ) // line contains a metadatavalue for the current METADATAFIELD
            record.appendField(lijn);
        else if (isUseFulField(lijn))
        {
            record.setDCEntities(element, qualifier, language);
            record.addField(lijn);
        }
    }

    private void writeCurrentRecord(){
        if (record!=null){
            setOutput(record.recordbuffer.toString());
            record.updateItem();
            setOutput("\n</table><br/><hr/><br/>\n");
        }
    }

    @Override
    public void process(Context context, Collection collection, File file, String startString) {
        super.process(context, collection, file, startString);
        writeCurrentRecord();
    }

}
