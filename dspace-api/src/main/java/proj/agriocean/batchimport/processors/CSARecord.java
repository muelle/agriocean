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

class CSARecord extends Record{
//DC
    CSARecord(int teller,Context context,Collection collection, Properties doctypeMap) {
        super(teller,context,collection, doctypeMap);
    }
//addNewField
    public void addField(String lijn) {
        super.addField(lijn);
    }
//appendField
    public void appendField(String lijn){
        if(isNewField){
            if (field.startsWith("AU") || field.startsWith("AF")){//Dit is velden die meerdere velden dienen te blijven
                buffer = buffer.concat("\n\t\t "+lijn.trim());
            }
            else{
                buffer = lijn.trim();
            }
            isNewField = false;
        }
        else{
            if (field.startsWith("AU") || field.startsWith("AF")){//Dit is velden die meerdere velden dienen te blijven
                buffer = buffer.concat("\n\t\t "+lijn.trim());
            }
            else{
                buffer = buffer.concat(" "+lijn.trim());
            }
        }
    }
//getRecord:Dit returned de Controle HTML Pagina
    public StringBuffer getRecord(){
        setMetaData(buffer);
        return recordbuffer;
    }
//Dit zet de gegevens klaar voor WorkSpaceItem
    public void setMetaData(String item) {
        if (qualifier.equals("")){
            //IF RECORD instanceOf ISI Record, anders zie pmed
            if (element.equals("type")){
                item = schema+"."+element+" = "+getSubType(item);
                recordbuffer.append(wrapItemInHTML(item));
            }
            else{
                item = schema+"."+element+" = "+cleantitem(item);
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
        else{
            if (isSpecialItem(item)){
                handleSpecialItem(item);
            }else{
                item = schema+"."+element+"."+qualifier+" = "+cleantitem(item);
                recordbuffer.append(wrapItemInHTML(item));
            }
        }
        //System.out.println("\t\t"+item);
    }
//cleanitem
    private String cleantitem(String item) {
	//System.out.println("BEFORE CLEAN ITEM : \t\t"+item);
        if (isNewField)item = item.substring(item.indexOf(" ")+1);
        //System.out.println("AFTER CLEAN ITEM : \t\t"+item);
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
            //if (item!=null)setMetaData(item);
        }
        if(!buffer.equals("") && !isNewField){
            item = buffer.concat("|");
            int beginIndex = 0;
            int endIndex;
            for (int i = 0 ; i < item.length() ; i++){
                if (item.charAt(i) == '|'){
                    endIndex = i;
                    String veld = item.substring(beginIndex, endIndex);
                    if (veld!=null && !veld.trim().equals(""))setMetaData(veld);
                    beginIndex = endIndex+1;
                }
            }
        }
    }
//isSpecialItem
    public boolean isSpecialItem(String item) {
        boolean retlw = false;
        if (qualifier.equals("stpage"))retlw = true;
        if (qualifier.equals("author"))retlw = true;
        return retlw;
    }
//handleSpecialItem
    public void handleSpecialItem(String item) {
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
    	if (qualifier.equals("author")){
            String splitter = ";";
            splitLine(item,splitter);
        }
    }
//splitItem
    public void splitLine(String lijn,String splitter) {
        lijn = lijn.concat(splitter);
        lijn = lijn.replace(splitter,"|");
        int beginIndex = 0;
        int endIndex;
        for (int i = 0 ; i < lijn.length() ; i++){
            if (lijn.charAt(i) == '|'){
                endIndex = i;
                String veld = lijn.substring(beginIndex, endIndex);
                //record.add(veld);
                veld = schema+"."+element+"."+qualifier+" = "+veld;
		//System.out.println("___"+veld+"____");
                recordbuffer.append(wrapItemInHTML(veld));
                beginIndex = endIndex+1;
            }
        }
    }
}