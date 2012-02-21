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

class MSNProcessor extends MetadataProcessor {
//ICV
    int teller = 1;
    private String startString = "";
//DC
   public MSNProcessor(){
       super();
   }
//process
    @Override
    public void process(Context context, Collection collection, File file, String startString) {
	setContext(context);
	setCollection(collection);
	setStartString(startString);
        Scanner scanner;
        try {
            scanner = new Scanner(file);
            while(scanner.hasNextLine()){
                String lijn = scanner.nextLine();
                //if (lijn.trim().startsWith(startString)){
		if (lijn.trim().contains(startString)){
                    setNewRecord();
                }
                else{
                    if(record != null){
                        handleFields(lijn);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MSNProcessor.class.getName()).log(Level.SEVERE, "File Not Found", ex);
        }
    }
//setNewRecord
    @Override
    protected void setNewRecord() {
        record = new MSNRecord(teller,context,collection,doctypeMap);
        setOutput("<table>");
        teller++;
    }
//handleFields
    @Override
    protected void handleFields(String lijn) {
        record.setDCEntities(element,qualifier,language);
        if (lijn.contains("=")){
            if (isUseFulField(lijn.trim().substring(0,lijn.trim().indexOf("=")).trim())){
                record.addField(lijn);
            }
        }
        else{
            record.appendField(lijn);
        }
        if (lijn.startsWith("}")){
            setOutput(record.getRecord().toString());
            record.updateItem();
            setOutput("\n</table><br/><hr/><br/>\n");
        }
    }
//startString
    public void setStartString(String startString){
	this.startString = startString;
    } 
}