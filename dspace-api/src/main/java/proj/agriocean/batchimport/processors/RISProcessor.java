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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RISProcessor {
//ICV
    private File file;
//DC
    public RISProcessor(File file) {
	setFile(file);
    }
//process
    public StringBuffer process(File file) {
	setFile(file);
        Scanner scanner;
        StringBuffer retlw = null;
        String startString = "";
        try {
            scanner = new Scanner(file);
            MetadataProcessor processor = null;
            String type = scanner.nextLine();
            if (type.startsWith("FN")){
                processor = new ISIProcessor();
                startString = "PT";
            }
            if (type.startsWith("PMID")){
                processor = new PMEDProcessor();
                startString = "PMID";
            }
            else{
                type = scanner.nextLine();
                if (type.equals("CSA")){ 
                    processor = new CSAProcessor();
                    startString = "Record";
                }
            }
            //processor.process(file,startString);
            //retlw = processor.getOuput();
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger(RISProcessor.class.getName()).log(Level.SEVERE,"Exception File Not Found > "+file.getName(),fnfe);
        }
        return retlw;
    }
//setFile
    private void setFile(File file){
	this.file = file;
    }
}