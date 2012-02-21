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
import java.util.Properties;
import org.dspace.content.Collection;
import org.dspace.core.Context;

public interface MetadataProcessorInface {
    public void process(Context context, Collection collection, File file, String startString);
    public void setProperties(String string);
    public Properties getProperties();
    public void setOutput(String output);
    public StringBuffer getOuput();
    public void setContext(Context context);
}
