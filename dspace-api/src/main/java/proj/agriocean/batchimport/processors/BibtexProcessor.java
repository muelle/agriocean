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

public class BibtexProcessor {
//ICV
	private File file;
//DC
    public BibtexProcessor(File file) {
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
            if (type.startsWith("MathSciNet") || type.startsWith("MR:")){
                processor = new MSNProcessor();//System.out.println("MathSciNet Records");
                startString = "@";
            }
            if (type.startsWith("@")){
                processor = new ZBMProcessor();//System.out.println("zentralblatt-math Records");
                startString = "@";
            }
            if (type.startsWith("EBSCOhost")){
                processor = new EBSCOhostProcessor();//System.out.println("EBSCOhost Records");
                startString = "@";
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