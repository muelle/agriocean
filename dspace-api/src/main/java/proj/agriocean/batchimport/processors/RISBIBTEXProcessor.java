/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
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
import org.dspace.content.Collection;
import org.dspace.core.Context;

public class RISBIBTEXProcessor {
    //process

    public StringBuffer process(Context context, Collection collection, File file) {
        //public StringBuffer process(Context context,Collection collection,File file) {
        Scanner scanner;
        StringBuffer retlw = null;
        String startString = "";
        try {
            scanner = new Scanner(file);
            MetadataProcessor processor = null;
            String type = scanner.nextLine();

            //skip blank lines
            while (type == null || type.trim().equals("")) {
                type = scanner.nextLine();
            }

            if (type.startsWith("FN")) {
                processor = new ISIProcessor();
                startString = "PT";
            }
            if (type.startsWith("PMID")) {
                processor = new PMEDProcessor();
                startString = "PMID";
            }
            if (type.startsWith("TY")) {
                processor = new ZoteroRISProcessor();
                startString = "TY";
            }
            if (type.startsWith("%")) {
                processor = new EndNoteProcessor();
                startString = "%0";
            }
            processor.process(context, collection, file, startString);
            retlw = processor.getOuput();
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger(RISBIBTEXProcessor.class.getName()).log(Level.SEVERE, "Exception File Not Found " + file.getName(), fnfe);
            retlw.append(" ... Exception ocured");
        } catch (Exception e) {
            Logger.getLogger(RISBIBTEXProcessor.class.getName()).log(Level.SEVERE, "Exception  " + file.getName(), e);
            retlw.append(" ... Exception ocured");
        } finally {

            return retlw;
        }
    }
}