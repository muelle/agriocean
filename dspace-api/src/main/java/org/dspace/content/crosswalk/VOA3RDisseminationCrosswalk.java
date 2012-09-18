/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.export.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.license.CreativeCommons;
import org.jdom.Document;




public class VOA3RDisseminationCrosswalk extends XSLTDisseminationCrosswalk implements DisseminationCrosswalk
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(VOA3RDisseminationCrosswalk.class);
    
    private static final String DIRECTION = "dissemination";
    // the type is used to locate config keys in the configuration file
    // keys starting with "crosswalk.[type].[direction]." are searched
    private static final String TYPE = "voa3r";
    
    @Override
    protected String getConfigPrefix()
    {
        return CONFIG_PREFIX + TYPE + ".";
    }

    @Override
    protected void init()
        throws CrosswalkInternalException
    {
        super.init();
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
    }

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(makeAliases(TYPE, DIRECTION));
    }

    /**
     * Extra elements can be added to the DIM representation to make non-DC information available to the XSLT transformation
     * @param dimDoc
     * @param dso 
     */
    @Override
    protected void appendExtraDIMFieldElements(Document dimDoc, DSpaceObject dso)
    {

        // link to Bitstream as voa3r.isShownBy
        dimDoc.getRootElement().addContent(createField("voa3r", "isShownBy", null, null, getLinkToBitstream(dso)));
        
        // BibTeX as voa3r.bibliographicCitation
        dimDoc.getRootElement().addContent(createField("voa3r", "bibliographicCitation", null, null, getVOA3RBibliographicCitation(dso)));
        
        // embargo/accessRights as voa3r.accessRights
        dimDoc.getRootElement().addContent(createField("voa3r", "accessRights", null, null, getVOA3RAccessRights(dso)));
        
        // license info as voa3r.license
        dimDoc.getRootElement().addContent(createField("voa3r", "license", null, null, getVOA3RLicense(dso)));
    }
    
    private static String getVOA3RLicense(DSpaceObject dso)
    {
        if (dso.getType() != Constants.ITEM)
            return null;
        
        Item item = (Item) dso;
        try {
            return CreativeCommons.getLicenseURL(item);
        } catch (Exception ex) {
            log.debug("Error when extracting license from item with ID " + item.getID() + ".", ex);
        }        
        return null;
    }
    
    private static String getVOA3RAccessRights(DSpaceObject dso)
    {
        // ! change when embargo on bitstreams is enabled
        return "Public";
    }
    
    private static String getVOA3RBibliographicCitation(DSpaceObject dso)
    {
        if (dso.getType() != Constants.ITEM)
            return null;
        Item item = (Item) dso;
        try{
        String bibtex = (new DC2BibTeXExport().export(item));
        if (bibtex!=null)
            return "BibTEX:" +  bibtex;
        } catch (NoTargetBibtexTypeException ex) {
        } catch (TargetBibtexTypeInitException ex) {
        } catch (NoTargetBibtexFieldsException ex) {
        } catch (NoDCTypeException nte){
        }
        
        return null;
    }
        
    private static String getLinkToBitstream(DSpaceObject dso)
    {
        if (dso.getType() != Constants.ITEM)
            return null;
        Item item = (Item) dso;

        Bitstream bitstream = getFirstBitstreamInDefaultBundle(item);
        if (bitstream != null) {
            try {
                StringBuilder path = new StringBuilder();
                path.append(ConfigurationManager.getProperty("dspace.url"));

                if (item.getHandle() != null) {
                    path.append("/bitstream/");
                    path.append(item.getHandle());
                    path.append("/");
                    path.append(bitstream.getSequenceID());
                } else {
                    path.append("/retrieve/");
                    path.append(bitstream.getID());
                }

                path.append("/");
                path.append(Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING));
                return path.toString();
            } catch (UnsupportedEncodingException ex) {
                log.debug(null, ex);
            }
        }
       
        return null;
    }
    
    /**
     * Returns the first bitstream in the default content bundle, and null otherwise.
     * @param item
     * @return
     */
    private static Bitstream getFirstBitstreamInDefaultBundle(Item item){
        try{
            Bundle[] contentBundles = item.getBundles("ORIGINAL");
            if (contentBundles.length > 0) {
                Bitstream[] bitstreams = contentBundles[0].getBitstreams();
                if (bitstreams.length == 1) {
                    return bitstreams[0];
                }
            }
        } catch (SQLException sqle){
            log.debug(sqle.getMessage());
        }
        return null;
    }
    

    /**
     * Determine is this crosswalk can dessiminate the given object.
     *
     * @see DisseminationCrosswalk
     */
    public boolean canDisseminate(DSpaceObject dso)
    {
        return dso.getType() == Constants.ITEM;
    }


}
