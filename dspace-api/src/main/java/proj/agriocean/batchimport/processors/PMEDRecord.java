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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dspace.content.Collection;
import org.dspace.core.Context;

class PMEDRecord extends Record{
//DC
    PMEDRecord(int teller,Context context,Collection collection, Properties doctypeMap) {
        super(teller,context,collection, doctypeMap);
    }
//SetField
    public void addField(String lijn) {
        super.addField(lijn);
    }
//appendField
    public void appendField(String lijn){
        if(isNewField){
            if (field.startsWith("ED") || field.startsWith("AU")){
                buffer = field.concat("\n\t\t~AU "+lijn.trim());
            }
            else{
                buffer = field.concat(" "+lijn.trim());
            }
            isNewField = false;
        }
        else{
            if (field.startsWith("ED")|| field.startsWith("AU")){
                buffer = buffer.concat("\n\t\t~AU "+lijn.trim());
            }
            else{
                buffer = buffer.concat(" "+lijn.trim());
            }
        }
    }
//SetMetaData
    public void setMetaData(String item) {
        if (qualifier.equals("")){
	    if (element.equals("type")){
		item = schema+"."+element+" = "+item;
	    }
	    else{
		item = schema+"."+element+" = "+cleanitem(item);
	    }
            recordbuffer.append(wrapItemInHTML(item));
        }
        else{
            item = cleanitem(item);
            if (isSpecialItem(item)){
                handleSpecialItem(item);
            }
            else{
		if (item.contains("[doi]")) item = item.substring(0,item.indexOf("[")).trim();
                item = schema+"."+element+"."+qualifier+" = "+item;
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
    }
//cleanItem
    public String cleanitem(String item) {
        item = item.substring(item.indexOf("- ")+1);
        item = item.trim();
        return item;
    }
//SetBufferedItem : Dit is het laatste item op de stack
    public void setBufferedItem() {
        String item = null;
        if (!field.equals("") && isNewField){
            item = field;
            if (item!=null){
                setMetaData(item);
            }
        }
        if(!buffer.equals("") && !isNewField){
            item = buffer.concat("~");
            int beginIndex = 0;
            int endIndex;
            for (int i = 0 ; i < item.length() ; i++){
                if (item.charAt(i) == '~'){
                    endIndex = i;
                    String veld = item.substring(beginIndex, endIndex);
                    if (veld!=null)setMetaData(veld);
                    beginIndex = endIndex+1;
                }
            }
        }
    }
//specialItems Voorbeeld pages waarbij endpage gescheiden dient te worden van de startpage
    public boolean isSpecialItem(String item) {
        boolean retlw = false;
        if (qualifier.equals("issued"))retlw = true;
	if (qualifier.equals("doi") && item.contains("[pii]"))retlw = true;
        return retlw;
    }
//handleSpecialItem
    public void handleSpecialItem(String item) {
        //System.out.print("handle special item : "+qualifier);
        if (qualifier.equals("issued")){
            Pattern p = Pattern.compile("\\s*(\\d+)\\s*");
            Matcher m = p.matcher(item);
            if (m.find() && m.groupCount() == 1){
                item = m.group(1);
                item = schema+"."+element+"."+qualifier+" = "+item;
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
	if (qualifier.equals("doi") && item.contains("[pii]"));
    }
}
