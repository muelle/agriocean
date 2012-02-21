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
import java.util.Properties;
import org.dspace.content.Collection;
import org.dspace.core.Context;

class EBSCOhostRecord extends Record{
//DC
    EBSCOhostRecord(int teller,Context context,Collection collection, Properties doctypeMap) {
        super(teller,context,collection, doctypeMap);
    }
//addNewField
    public void addField(String lijn) {
	super.addField(lijn);
    }
//appendField
    public void appendField(String lijn){
        if(isNewField){
            if (field.startsWith("Abstract")){//Dit is velden die meerdere velden dienen te blijven
                buffer = field.concat("\n\t\t "+lijn.trim());
            }
            else{
                buffer = field.concat(" "+lijn.trim());
            }
            isNewField = false;
        }
        else{
            if (field.startsWith("Abstract")){//Dit is velden die meerdere velden dienen te blijven
                buffer = buffer.concat("\n\t\t "+lijn.trim());
            }
            else{
                buffer = buffer.concat(" "+lijn.trim());
            }
        }
    }
//Dit zet de gegevens klaar voor WorkSpaceItem
    public void setMetaData(String item) {
        item = cleantitem(item);
        if (qualifier.equals("")){
            if (element.equals("type")){
                //item = schema+"."+element+" = "+getSubType(item);
		item = schema+"."+element+" = "+item;
		System.out.println(item);
            }
            else{
                item = schema+"."+element+" = "+item;
            }
            recordbuffer.append(wrapItemInHTML(item));
        }
        else{
            if (isSpecialItem(item)){
                handleSpecialItem(item);
            }
            else{
                item = schema+"."+element+"."+qualifier+" = "+item;
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
    }
//handleSpecialItem
    public void handleSpecialItem(String item) {
	String buffer = "";
        if (qualifier.equals("stpage")){
            if (item.contains("-")){ 
                buffer = item.substring(0,item.indexOf("-")).trim();
                if (buffer.startsWith("p")){
                    buffer = buffer.replace("p","");
                }
                buffer = schema+"."+element+"."+qualifier+" = "+buffer;
                recordbuffer.append(wrapItemInHTML(buffer));
                buffer = "";
                qualifier = "endpage";
                buffer = item.substring(item.indexOf("-")+2).trim();
                if (!buffer.equals("")){
                    buffer = schema+"."+element+"."+qualifier+" = "+buffer;
                recordbuffer.append(wrapItemInHTML(buffer));
                }
            }
        }
        //split de auteurs in aparte velden
        if (qualifier.equals("author")){
            String splitter = "and";
            splitLine(item,splitter);
        }
	if (qualifier.equals("issued")){
	    if (item.length() >= 4){	//Haal het jaartal uit de gegevens
	    	buffer = schema+"."+element+"."+qualifier+" = "+item.substring(0,4);
            	recordbuffer.append(wrapItemInHTML(buffer));
	    }
        }
    }
//isSpecialItem : Voorbeeld pagina die gesplitst moet worden in startpage, endpage
    public boolean isSpecialItem(String item) {
        boolean retlw = false;
        if (qualifier.equals("stpage"))retlw = true;
        if (qualifier.equals("author"))retlw = true;
	if (qualifier.equals("issued"))retlw = true;
        return retlw;
    }
//SetBufferedItem: Het laatste Item op de stack wescherijven naar de buffer
    public void setBufferedItem() {
        String item = null;
        if (!field.equals("") && isNewField){
            item = field;
        }
        if(!buffer.equals("") && !isNewField){
            item = buffer;
        }
        if (item!=null)setMetaData(item);
    }
//cleanitem
    public String cleantitem(String item) {
        item = item.trim();
        item = item.substring(item.indexOf("= ")+1);
        if (item.trim().startsWith("{")) item = item.trim().substring(1);
        if (item.endsWith("},")) item = item.substring(0,item.length()-2);
        return item;
    }
//SplitLine : Gebruikt om bijvoorbeeld auteurs te splitsen
    public void splitLine(String lijn, String splitter) {
        lijn = lijn.concat(splitter);
        lijn = lijn.replace(splitter,"|");
        int beginIndex = 0;
        int endIndex;
        //record = new Vector<String>();

        for (int i = 0 ; i < lijn.length() ; i++){
            if (lijn.charAt(i) == '|'){
                endIndex = i;
                String veld = lijn.substring(beginIndex, endIndex);
                //record.add(veld);
                veld = schema+"."+element+"."+qualifier+" = "+veld;
                recordbuffer.append(wrapItemInHTML(veld));
                beginIndex = endIndex+1;
            }
        }
    }
}