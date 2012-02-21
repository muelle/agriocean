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

import java.util.Properties;
import org.dspace.content.Collection;
import org.dspace.core.Context;

/**
 *
 * @author 
 */
public class EndNoteRecord extends Record {
    // number of characters used at the beginning of a line to identify the metadata field described
    private static final int METADAFIELD_TAG_SKIPLENGTH = 3;

    public EndNoteRecord(int teller, Context context, Collection collection,  Properties doctypeMap){
        super(teller, context, collection, doctypeMap);
    }

    @Override
    public void addField(String lijn) {
        if(lijn.length() >= METADAFIELD_TAG_SKIPLENGTH) {
            setMetaData(lijn.substring(METADAFIELD_TAG_SKIPLENGTH));
        }
    }

    @Override
    public void appendField(String lijn) {
        if (lijn != null && !lijn.equals("") && element != null && !element.equals("")) // ignore empty lines and make sure current field is set
            setMetaData(lijn);
    }


   
    @Override
    public void setMetaData(String stritem) {
        stritem = stritem.trim();


        if (qualifier != null && !qualifier.equals(""))
            stritem = schema + "." + element + "." + qualifier + " = " + stritem;
        else
            stritem = schema + "." + element + " = " + stritem;

        recordbuffer.append(wrapItemInHTML(stritem));
    }

    


}
