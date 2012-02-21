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
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.core.Context;

class ISIProcessor extends MetadataProcessor {
//ICV
    int teller = 1;
//DC
   public ISIProcessor(){
       super();
   }
//process
    @Override
    public void process(Context context, Collection collection, File file, String startString) {
        //super.process(context,collection,file,formType,startString);
	setContext(context);
	setCollection(collection);
        Scanner scanner;
        try {
            scanner = new Scanner(file);
            Logger.getLogger(MetadataProcessor.class.getName()).info("processing..."+file.getName());
            while(scanner.hasNextLine()){
                String lijn = scanner.nextLine();
                if (lijn.startsWith(startString)){
                    setNewRecord();
                }
                if(record != null){
                    handleFields(lijn);
                }
            }
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger(MetadataProcessor.class.getName()).error("File Not Found", fnfe);
        }
    }
//setNewRecord
    @Override
    protected void setNewRecord() {
        record = new ISIRecord(teller,context,collection,doctypeMap);
        setOutput("<table>");
        teller++;
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
        if (lijn.trim().equals("ER")){//if (lijn.startsWith("ER")){
            setOutput(record.getRecord().toString());
            record.updateItem();
            setOutput("\n</table><br/><hr/><br/>\n");
        }
    }
}
