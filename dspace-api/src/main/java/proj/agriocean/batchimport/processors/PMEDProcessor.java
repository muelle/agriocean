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
//Imports
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dspace.content.Collection;
import org.dspace.core.Context;

class PMEDProcessor extends MetadataProcessor {
//ICV
    int teller = 1;
    boolean written = false;
//DC
   public PMEDProcessor(){
       super();
   }
//process
    @Override
    public void process(Context context, Collection collection, File file, String startString) {
	setContext(context);
	setCollection(collection);
	Scanner scanner;
        try {
            scanner = new Scanner(file);
            Logger.getLogger(MetadataProcessor.class.getName()).log(Level.INFO, "processing..."+file.getName());
            while(scanner.hasNextLine()){
                String lijn = scanner.nextLine();
                if (lijn.startsWith(startString)){
                    setNewRecord();
                }
                if(record != null){
                    handleFields(lijn);
                }
            }
	    schrijf();
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger(MetadataProcessor.class.getName()).log(Level.SEVERE, "File Not Found", fnfe);
        }
    }
//setNewRecord
    @Override
    protected void setNewRecord() {
        record = new PMEDRecord(teller,context,collection,doctypeMap);
        setOutput("<table>");
        teller++;
	written = true;
    }
//handleFields
    @Override
    protected void handleFields(String lijn) {
        if (!lijn.startsWith(" ")){
            record.setDCEntities(element,qualifier,language);
            if (isUseFulField(lijn)){
                record.addField(lijn);
            }
        }
        else{
            if(isUseful) record.appendField(lijn);
        }
        if (lijn.trim().equals("")){
            schrijf();
        }
    }
    private void schrijf(){
	if (written){
		setOutput(record.getRecord().toString());
		record.updateItem();
		setOutput("\n</table><br/><hr/><br/>\n");
		written = false;
	}
    }
}