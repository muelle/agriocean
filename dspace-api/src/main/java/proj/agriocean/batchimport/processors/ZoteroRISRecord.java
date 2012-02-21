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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dspace.content.Collection;
import org.dspace.core.Context;

/**
 *
 * @author 
 */
public class ZoteroRISRecord extends Record {
    private static final String METADATAFIELD_TAG_ENDCHAR = "-";

    public ZoteroRISRecord(int teller, Context context, Collection collection, Properties doctypeMap) {
        super(teller, context, collection, doctypeMap);
    }

    @Override
    public void addField(String lijn) {
        // find first occurrence of METADATAFIELD_TAG_ENDCHAR. Content
        // of field starts there.
        int startPosContent = lijn.indexOf(METADATAFIELD_TAG_ENDCHAR);
        if (startPosContent >= 0)
            setMetaData(lijn.substring(startPosContent + 1));
        else
            setMetaData(lijn);
    }

    @Override
    public void setMetaData(String stritem) {
        stritem = stritem.trim();

        if (isSpecialField(stritem))
            handleSpecialField(stritem);
        else{
            if (qualifier != null && !qualifier.equals(""))
                stritem = schema + "." + element + "." + qualifier + " = " + stritem;
            else
                stritem = schema + "." + element + " = " + stritem;

            recordbuffer.append(wrapItemInHTML(stritem));
        }
    }

    private boolean isSpecialField(String stritem) {

        if ("date".equals(element) && "issued".equals(qualifier))
            return true;

        return false;
    }

    private void handleSpecialField(String stritem) {
        if ("date".equals(element) && "issued".equals(qualifier))
        {
            // first substring of digits is considered to be the year
            Pattern p = Pattern.compile("\\s*(\\d+)\\s*");
            Matcher m = p.matcher(stritem);
            if (m.find()){
                stritem = m.group(1);
                recordbuffer.append(wrapItemInHTML("dc.date.issued = " + stritem));
            }
        }

    }
}
