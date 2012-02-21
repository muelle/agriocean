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
interface RecordInface {
    public void addField(String lijn);
    public void appendField(String lijn);
    public String wrapItemInHTML(String item);
    public StringBuffer getRecord();
    public void setMetaData(String item);
    public void setDCEntities(String element,String qualifier,String language);
    public String getSubType(String field);
    public void setBufferedItem();
}
