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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

public class MetadataProcessor implements MetadataProcessorInface{
//ICV
    //Aanhepast op 01/08/2010
    public static final String path = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator + "batchimport" + File.separator;

    // the suffix used to construct the file name containing the properties for translating
    // document types to types known by the current dspace system.
    // the map is read from the file [path] + [ProcessorClassName] + [doctypeSuffix] + .properties
    public static final String doctypeSuffix = "-doctypeMapping";
    
    // the mapping between import format's document type and dspace document types
    protected Properties doctypeMap;
    // translation between import format's metadatafield tags and DC metadata fields
    protected Properties properties;
    protected StringBuffer output;
    protected Map<String, String> map;
    protected String key;
    protected String element;
    protected String qualifier;
    protected String language;
    protected boolean isUseful;
    protected Context context;
    protected Collection collection;
    protected Record record;
//DC
    public MetadataProcessor() {
        setProperties(path.concat(getClass().getSimpleName()).concat(".properties"));
        loadDocTypeMap();
        output = new StringBuffer();
    }

    private void loadDocTypeMap(){
        doctypeMap = new Properties();
        try{
            doctypeMap.load(new FileInputStream(path.concat(this.getClass().getSimpleName()).concat(doctypeSuffix).concat(".properties")));
        } catch (IOException ex)
        {
            Logger.getLogger(this.getClass().getName()).info("Error loading "
                    + path.concat(this.getClass().getSimpleName()).concat(doctypeSuffix) + ".properties", ex);
        }
    }

//Implementations
    @Override
    public void process(Context context, Collection collection, File file, String startString) {
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
//SetProperties
    @Override
    public void setProperties(String mappingFile){
        properties = new Properties();
        try {
            File mapFile = new File(mappingFile);
            properties.load(new FileInputStream(mapFile));
            map = new HashMap<String, String>();
            Enumeration keys = properties.keys();
            Enumeration vals = properties.elements();
            while (keys.hasMoreElements()){
                map.put((String)keys.nextElement(), (String)vals.nextElement());
            }
	    Logger.getLogger(MetadataProcessor.class.getName()).info("Properties... "+mapFile.toString()+"...loaded");
        } catch (FileNotFoundException fnfe) {
		Logger.getLogger(MetadataProcessor.class.getName()).error("Exception File Not Found "+mappingFile,fnfe);
        } catch (IOException ioe) {
		Logger.getLogger(MetadataProcessor.class.getName()).error("Exception IO "+mappingFile,ioe);
        }
    }
//isUseFul
    boolean isUseFulField(String lijn) {
        isUseful = false;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (lijn.startsWith(entry.getKey())) {
                isUseful = true;
                String v = entry.getValue();
                if (v.contains(".")) {
                    element = v.substring(0, v.indexOf("."));
                    qualifier = v.substring(v.indexOf(".") + 1, v.length());
                } else {
                    element = v;
                    qualifier = "";
                }
                language = "";
                break;
            }
        }
        return isUseful;
    }
//setOuput
    @Override
    public void setOutput(String outputString) {
        output.append(outputString);
    }
//getOutput
    @Override
    public StringBuffer getOuput() {
        return output;
    }
//getProperties
    @Override
    public Properties getProperties() {
        return properties;
    }
//setNewRecord
    protected void setNewRecord() {}
//handleFields
    protected void handleFields(String lijn) {}

//GETTERS & SETTERS
//setContext
    @Override
    public void setContext(Context context){
	this.context = context;
    }
//getContext
    public Context getContext(){
	return this.context;
    }
//setCollection
    public void setCollection(Collection collection){
	    this.collection = collection;
    }
//getCollection
    public Collection getCollection(){
	    return this.collection;
    }
}
