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
import org.dspace.content.Collection;
import org.dspace.core.Context;

class ZBMRecord extends Record{
//DC
    ZBMRecord(int teller,Context context,Collection collection, Properties doctypeMap) {
        super(teller,context,collection, doctypeMap);
    }
//addNewField
    public void addField(String lijn) {
        super.addField(lijn);
    }
//appendField
    public void appendField(String lijn){
        if(isNewField){
            if (field.startsWith("ED") || field.startsWith("AU")){//Dit is velden die meerdere velden dienen te blijven
                buffer = field.concat("\n\t\t|AU "+lijn.trim());
            }
            else{
                buffer = field.concat(" "+lijn.trim());
            }
            isNewField = false;
        }
        else{
            if (field.startsWith("ED")|| field.startsWith("AU")){//Dit is velden die meerdere velden dienen te blijven
                buffer = buffer.concat("\n\t\t|AU "+lijn.trim());
            }
            else{
                buffer = buffer.concat(" "+lijn.trim());
            }
        }
    }
//Dit zet de gegevens klaar voor WorkSpaceItem
    public void setMetaData(String item) {
        item = cleanItem(item);
        if (qualifier.equals("")){
            if (element.equals("type")){
                item = schema+"."+element+" = "+item;
                recordbuffer.append(wrapItemInHTML(item));
            }
            else{
                item = schema+"."+element+" = "+cleanItem(item);
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
        else{
            if (isSpecialItem(item)){
                handleSpecialItem(item);
            }
            else{
                item = schema+"."+element+"."+qualifier+" = "+cleanItem(item);
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
        System.out.println("\t\t"+item);
    }
//clean Item : verwijder alle onnodige characters
    public String cleanItem(String item) {
        item = item.substring(item.indexOf("=")+1);
        if(item.trim().startsWith("\""))item = item.trim().substring(1);
        if(item.trim().startsWith("{"))item = item.trim().substring(1);
        if(item.endsWith(","))item = item.substring(0,item.length()-1);
        if(item.endsWith("\""))item = item.substring(0,item.length()-1);
        if(item.endsWith("}"))item = item.substring(0,item.length()-1);
        if(item.startsWith("Summary:"))item = item.replace("Summary:",""); 
        return item;
    }
//setSubType
    public String getSubType(String field) {
        String subtype = field.substring(field.indexOf("PT")+3).trim();
        if (subtype.equals("J")){
            subtype = "Journal Contribution";
        }
        else{
            subtype = "Proceedings Paper";
        }
        return subtype;
    }
//SetBufferedItem: Het laatste Item op de stack wescherijven naar de buffer
    public void setBufferedItem() {
        String item = null;
        if (!field.equals("") && isNewField){
            item = field;
            if (item!=null)setMetaData(item);
        }
        if(!buffer.equals("") && !isNewField){
            item = buffer.concat("|");
            int beginIndex = 0;
            int endIndex;
            for (int i = 0 ; i < item.length() ; i++){
                if (item.charAt(i) == '|'){
                    endIndex = i;
                    String veld = item.substring(beginIndex, endIndex);
                    if (veld!=null)setMetaData(veld);
                    beginIndex = endIndex+1;
                }
            }
        }
    }
//isSpecialItem : Voorbeeld pagina die gesplitst moet worden in startpage, endpage
    public boolean isSpecialItem(String item) {
        boolean retlw = false;
        if (qualifier.equals("stpage"))retlw = true;
        if (qualifier.equals("author"))retlw = true;
        return retlw;
    }
//handleSpecialItem
    public void handleSpecialItem(String item) {
        //Split de pagina's in begin -en eindpagina
        if (qualifier.equals("stpage")){
            String buffer = "";
            if (item.contains("-")){ 
                buffer = item.substring(0,item.indexOf("-"));
                buffer = schema+"."+element+"."+qualifier+" = "+buffer;
                recordbuffer.append(wrapItemInHTML(buffer));
                buffer = "";
                qualifier = "endpage";                
                buffer = item.substring(item.indexOf("-")+1);
                buffer = schema+"."+element+"."+qualifier+" = "+buffer;
                recordbuffer.append(wrapItemInHTML(buffer));
            }
        }
        //split de auteurs in aparte velden
        if (qualifier.equals("author")){
            String splitter = "and";
            splitLine(item,splitter);
        }
    }
//splitItem
    public void splitLine(String lijn,String splitter) {
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