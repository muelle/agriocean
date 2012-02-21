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

class ISIRecord extends Record{
//ICV
	private boolean boolAf = false;
	private boolean boolAu = false;
	private StringBuffer AUBuffer;
//DC
	ISIRecord(int teller,Context context,Collection collection, Properties doctypeMap) {
        	super(teller,context,collection, doctypeMap);
		AUBuffer = new StringBuffer();
    	}
//addNewField
	public void addField(String lijn) {
        	super.addField(lijn);
		if (field.startsWith("AU")){
			boolAu = true;
		}
		else{
			if (field.startsWith("AF")) boolAf = true;//boolAu &&
			boolAu = false;
		}
    	}
//appendField
    	public void appendField(String lijn){
		lijn = lijn.trim();

        	if(isNewField){
            		if (field.startsWith("SP") || field.startsWith("CR") || field.startsWith("ED") || field.startsWith("AU") || field.startsWith("AF")){
				if (field.startsWith("AF")){
					buffer = field.concat("|AF "+lijn);
					boolAu = false;
				}else{
					if (boolAu){
						buffer = field.concat("|AU "+lijn);
						//Logger.getLogger(ISIProcessor.class.getName()).log(Level.SEVERE, "_"+buffer+"_");
					}
					else{
						buffer = field.concat("|CK "+lijn);
						boolAu = false;
					}
				}
            		}	
            		else{
                		buffer = field.concat(" "+lijn);
            		}
            		isNewField = false;
        	}
		else{
			if (field.startsWith("SP") || field.startsWith("CR") ||field.startsWith("ED")|| field.startsWith("AU") || field.startsWith("AF")){
				if (field.startsWith("AU")){
					buffer = buffer.concat("|AU "+lijn);
				}
				else{
					if (field.startsWith("AF")){
						buffer = buffer.concat("|AF "+lijn);
						boolAu = false;
					}else{
						buffer = buffer.concat("|CK "+lijn);
						boolAu = false;
					}
				}
			}
			else{
				buffer = buffer.concat(" "+lijn);
			}
		}
    	}
//Dit zet de gegevens klaar voor WorkSpaceItem
    	public void setMetaData(String item) {
  		item = item.trim();
		if (item.startsWith("AU ")){
			item = cleanitem(item);
			AUBuffer.append(item+"~");
		}
		else{
			if (qualifier.equals("")){
				if (element.equals("type")){
					item = schema+"."+element+" "+getSubType(item);
				}
				if (element.equals("title") && !boolAf){
					schrijfAUBuffer();
					element = "title";
					qualifier = "";
					item = schema+"."+element+" = "+cleanitem(item);
				}
				else{
					item = schema+"."+element+" = "+cleanitem(item);
				}
			}
			else{
				item = schema+"."+element+"."+qualifier+" = "+cleanitem(item);
			}
			recordbuffer.append(wrapItemInHTML(item));
		}
    	}
//schrijfAUBuffer
	public void schrijfAUBuffer(){
		element = "contributor";
		qualifier = "author";
		String item = AUBuffer.toString();
		int beginIndex = 0;
        	int endIndex;
        	for (int i = 0 ; i < item.length() ; i++){
			if (item.charAt(i) == '~'){
				endIndex = i;
                    		String veld = item.substring(beginIndex, endIndex);
                    		if (veld!=null){
					veld = schema+"."+element+"."+qualifier+" = "+veld;
					recordbuffer.append(wrapItemInHTML(veld));
				}
                    		beginIndex = endIndex+1;
                	}
      		}
    	}
//cleanitem
    	public String cleanitem(String item) {
        	item = item.substring(item.indexOf(" ")+1);
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
}