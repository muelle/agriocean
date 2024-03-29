/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Site;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.license.CreativeCommons;

import edu.harvard.hul.ois.mets.Agent;
import edu.harvard.hul.ois.mets.Loctype;
import edu.harvard.hul.ois.mets.Mets;
import edu.harvard.hul.ois.mets.MetsHdr;
import edu.harvard.hul.ois.mets.Name;
import edu.harvard.hul.ois.mets.Role;
import edu.harvard.hul.ois.mets.Div;
import edu.harvard.hul.ois.mets.Mptr;
import edu.harvard.hul.ois.mets.StructMap;
import edu.harvard.hul.ois.mets.Type;
import edu.harvard.hul.ois.mets.helper.MetsException;
import edu.harvard.hul.ois.mets.helper.PCData;
import java.net.URLEncoder;
import java.util.Date;
import org.dspace.core.Utils;

/**
 * Subclass of the METS packager framework to disseminate a DSpace
 * Archival Information Package (AIP).  The AIP is intended to be, foremost,
 * a _complete_ and _accurate_ representation of one object in the DSpace
 * object model.  An AIP contains all of the information needed to restore
 * the object precisely in another DSpace archive instance.
 * <p>
 * Configuration keys:
 * <p>
 * The following take as values a space-and-or-comma-separated list
 * of plugin names that name *either* a DisseminationCrosswalk or
 * StreamDisseminationCrosswalk plugin.  Shown are the default values.
 * The value may be a simple crosswalk name, or a METS MDsec-name followed by
 * a colon and the crosswalk name e.g. "DSpaceDepositLicense:DSPACE_DEPLICENSE"
 *
 *    # MD types to put in the sourceMD section of the object.
 *    aip.disseminate.sourceMD = AIP-TECHMD
 *
 *    # MD types to put in the techMD section of the object (and member Bitstreams if an Item)
 *    aip.disseminate.techMD = PREMIS
 *
 *    # MD types to put in digiprovMD section of the object.
 *    #aip.disseminate.digiprovMD = 
 *
 *    # MD types to put in the rightsMD section of the object.
 *    aip.disseminate.rightsMD = DSpaceDepositLicense:DSPACE_DEPLICENSE, \
 *       CreativeCommonsRDF:DSPACE_CCRDF, CreativeCommonsText:DSPACE_CCTXT, METSRights
 *
 *    # MD types to put in dmdSec's corresponding  the object.
 *    aip.disseminate.dmd = MODS, DIM
 *
 * @author Larry Stone
 * @author Tim Donohue
 * @version $Revision: 1.1 $
 * @see AbstractMETSDisseminator
 * @see AbstractPackageDisseminator
 */
public class DSpaceAIPDisseminator extends AbstractMETSDisseminator
{
    private static final Logger log = Logger.getLogger(DSpaceAIPDisseminator.class);
    
    /**
     * Unique identifier for the profile of the METS document.
     * To ensure uniqueness, it is the URL that the XML schema document would
     * have _if_ there were to be one.  There is no schema at this time.
     */
    public static final String PROFILE_1_0 =
        "http://www.dspace.org/schema/aip/mets_aip_1_0.xsd";

    /** TYPE of the div containing AIP's parent handle in its mptr. */
    public static final String PARENT_DIV_TYPE = "AIP Parent Link";

    // Default MDTYPE value for deposit license -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private static final String DSPACE_DEPOSIT_LICENSE_MDTYPE =
        "DSpaceDepositLicense:DSPACE_DEPLICENSE";

    // Default MDTYPE value for CC license in RDF -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private static final String CREATIVE_COMMONS_RDF_MDTYPE =
        "CreativeCommonsRDF:DSPACE_CCRDF";

    // Default MDTYPE value for CC license in Text -- "magic string"
    // NOTE: format is  <label-for-METS>:<DSpace-crosswalk-name>
    private static final String CREATIVE_COMMONS_TEXT_MDTYPE =
        "CreativeCommonsText:DSPACE_CCTXT";

    // dissemination parameters passed to the AIP Disseminator
    private PackageParameters disseminateParams = null;

    @Override
    public void disseminate(Context context, DSpaceObject dso,
                            PackageParameters params, File pkgFile)
        throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        //Before disseminating anything, save the passed in PackageParameters, so they can be used by all methods
        disseminateParams = params;

        boolean disseminate = true; //by default, always disseminate

        //if user specified to only disseminate objects updated *after* a specific date
        // (Note: this only works for Items right now, as DSpace doesn't store a
        //  last modified date for Collections or Communities)
        if(disseminateParams.containsKey("updatedAfter") && dso.getType()==Constants.ITEM)
        {
            Date afterDate = Utils.parseISO8601Date(disseminateParams.getProperty("updatedAfter"));

            //if null is returned, we couldn't parse the date!
            if(afterDate==null)
            {
                 throw new IOException("Invalid date passed in via 'updatedAfter' option. Date must be in ISO-8601 format, and include both a day and time (e.g. 2010-01-01T00:00:00).");
            }

            //check when this item was last modified.
            Item i = (Item) dso;
            if(i.getLastModified().after(afterDate))
            {
                disseminate = true;
            }
            else
            {
                disseminate = false;
            }
        }

        if(disseminate)
        {
            //just do a normal dissemination as specified by AbstractMETSDisseminator
            super.disseminate(context, dso, params, pkgFile);
        }
    }

    /**
     * Return identifier string for the METS profile this produces.
     *
     * @return string name of profile.
     */
    @Override
    public String getProfile()
    {
        return PROFILE_1_0;
    }

    /**
     * Returns name of METS fileGrp corresponding to a DSpace bundle name.
     * For AIP the mapping is direct.
     *
     * @param bname name of DSpace bundle.
     * @return string name of fileGrp
     */
    @Override
    public String bundleToFileGrp(String bname)
    {
        return bname;
    }

    /**
     * Create the metsHdr element for the AIP METS Manifest.
     * <p>
     * CREATEDATE is time at which the package (i.e. this manifest) was created.
     * LASTMODDATE is last-modified time of the target object, if available.
     * Agent describes the archive this belongs to.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public MetsHdr makeMetsHdr(Context context, DSpaceObject dso,
                               PackageParameters params)
    {
        MetsHdr metsHdr = new MetsHdr();

        // Note: we specifically do not add a CREATEDATE to <metsHdr>
        // as for AIPs we want md5 checksums to be identical if no content
        // has changed.  Adding a CREATEDATE changes checksum each time.

        // Add a LASTMODDATE for items
        if (dso.getType() == Constants.ITEM)
        {
            metsHdr.setLASTMODDATE(((Item) dso).getLastModified());
        }

        // Agent Custodian - name custodian, the DSpace Archive, by handle.
        Agent agent = new Agent();
        agent.setROLE(Role.CUSTODIAN);
        agent.setTYPE(Type.OTHER);
        agent.setOTHERTYPE("DSpace Archive");
        Name name = new Name();
        name.getContent()
                .add(new PCData(Site.getSiteHandle()));
        agent.getContent().add(name);
        metsHdr.getContent().add(agent);

        // Agent Creator - name creator, which is a specific version of DSpace.
        Agent agentCreator = new Agent();
        agentCreator.setROLE(Role.CREATOR);
        agentCreator.setTYPE(Type.OTHER);
        agentCreator.setOTHERTYPE("DSpace Software");
        Name creatorName = new Name();
        creatorName.getContent()
                .add(new PCData("DSpace " + Util.getSourceVersion()));
        agentCreator.getContent().add(creatorName);
        metsHdr.getContent().add(agentCreator);
        
        return metsHdr;
    }

    /**
     * Return the name of all crosswalks to run for the dmdSec section of
     * the METS Manifest.
     * <p>
     * Default is DIM (DSpace Internal Metadata) and MODS.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String [] getDmdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        String dmdTypes = ConfigurationManager.getProperty("aip.disseminate.dmd");
        if (dmdTypes == null)
        {
            String result[] = new String[2];
            result[0] = "MODS";
            result[1] = "DIM";
            return result;
        }
        else
        {
            return dmdTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the techMD section of
     * the METS Manifest.
     * <p>
     * Default is PREMIS.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getTechMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        String techTypes = ConfigurationManager.getProperty("aip.disseminate.techMD");
        if (techTypes == null)
        {
            if (dso.getType() == Constants.BITSTREAM)
            {
                String result[] = new String[1];
                result[0] = "PREMIS";
                return result;
            }
            else
            {
                return new String[0];
            }
        }
        else
        {
            return techTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the sourceMD section of
     * the METS Manifest.
     * <p>
     * Default is AIP-TECHMD.
     * <p>
     * In an AIP, the sourceMD element MUST include the original persistent
     * identifier (Handle) of the object, and the original persistent ID
     * (Handle) of its parent in the archive, so that it can be restored.
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getSourceMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        String sourceTypes = ConfigurationManager.getProperty("aip.disseminate.sourceMD");
        if (sourceTypes == null)
        {
            String result[] = new String[1];
            result[0] = "AIP-TECHMD";
            return result;
        }
        else
        {
            return sourceTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the digiprovMD section of
     * the METS Manifest.  
     * <p>
     * By default, none are returned
     *
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getDigiprovMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {
        String dpTypes = ConfigurationManager.getProperty("aip.disseminate.digiprovMD");
        if (dpTypes == null)
        {
            return new String[0];
        }
        else
        {
            return dpTypes.split("\\s*,\\s*");
        }
    }

    /**
     * Return the name of all crosswalks to run for the rightsMD section of
     * the METS Manifest.
     * <p>
     * By default, Deposit Licenses and CC Licenses will be added for Items.
     * Also, by default METSRights info will be added for all objects.
     * 
     * @param context DSpace Context
     * @param dso current DSpace Object
     * @param params Packager Parameters
     * @return List of crosswalk names to run
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public String[] getRightsMdTypes(Context context, DSpaceObject dso, PackageParameters params)
        throws SQLException, IOException, AuthorizeException
    {

        List<String> result = new ArrayList<String>();
        String rTypes = ConfigurationManager.getProperty("aip.disseminate.rightsMD");

        //If unspecified in configuration file, add default settings
        if (rTypes == null)
        {
            // Licenses only apply to an Item
            if (dso.getType() == Constants.ITEM)
            {
                //By default, disseminate Deposit License, and any CC Licenses
                // to an item's rightsMD section
                if (PackageUtils.findDepositLicense(context, (Item)dso) != null)
                {
                    result.add(DSPACE_DEPOSIT_LICENSE_MDTYPE);
                }

                if (CreativeCommons.getLicenseRdfBitstream((Item)dso) != null)
                {
                    result.add(CREATIVE_COMMONS_RDF_MDTYPE);
                }
                else if (CreativeCommons.getLicenseTextBitstream((Item)dso) != null)
                {
                    result.add(CREATIVE_COMMONS_TEXT_MDTYPE);
                }
            }
            
            //By default, also add METSRights info to the rightsMD
            result.add("METSRights");
        }
        else
        {
            return rTypes.split("\\s*,\\s*");
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Get the URL by which the METS manifest refers to a Bitstream
     * member within the same package.  In other words, this is generally
     * a relative path link to where the Bitstream file is within the Zipped
     * up AIP.
     * <p>
     * For a manifest-only AIP, this is a reference to an HTTP URL where
     * the bitstream should be able to be downloaded from.
     * An external AIP names a file in the package
     * with a relative URL, that is, relative pathname.
     * 
     * @param bitstream  the Bitstream
     * @param params Packager Parameters
     * @return String in URL format naming path to bitstream.
     */
    @Override
    public String makeBitstreamURL(Bitstream bitstream, PackageParameters params)
    {
        // if bare manifest, use external "persistent" URI for bitstreams
        if (params != null && (params.getBooleanProperty("manifestOnly", false)))
        {
            // Try to build a persistent(-ish) URI for bitstream
            // Format: {site-base-url}/bitstream/{item-handle}/{sequence-id}/{bitstream-name}
            try
            {
                // get handle of parent Item of this bitstream, if there is one:
                String handle = null;
                Bundle[] bn = bitstream.getBundles();
                if (bn.length > 0)
                {
                    Item bi[] = bn[0].getItems();
                    if (bi.length > 0)
                    {
                        handle = bi[0].getHandle();
                    }
                }
                if (handle != null)
                {
                    return ConfigurationManager
                                    .getProperty("dspace.url")
                            + "/bitstream/"
                            + handle
                            + "/"
                            + String.valueOf(bitstream.getSequenceID())
                            + "/"
                            + URLEncoder.encode(bitstream.getName(), "UTF-8");
                }
                else
                {
                    return ConfigurationManager
                                    .getProperty("dspace.url")
                            + "/retrieve/"
                            + String.valueOf(bitstream.getID());
                }
            }
            catch (SQLException e)
            {
                log.error("Database problem", e);
            }
            catch (UnsupportedEncodingException e)
            {
                log.error("Unknown character set", e);
            }

            // We should only get here if we failed to build a nice URL above
            // so, by default, we're just going to return the bitstream name.
            return bitstream.getName();
        }
        else
        {
            String base = "bitstream_"+String.valueOf(bitstream.getID());
            String ext[] = bitstream.getFormat().getExtensions();
            return (ext.length > 0) ? base+"."+ext[0] : base;
        }
    }

    /**
     * Adds another structMap element to contain the "parent link" that
     * is an essential part of every AIP.  This is a structmap of one
     * div, which contains an mptr indicating the Handle of the parent
     * of this object in the archive.  The div has a unique TYPE attribute
     * value, "AIP Parent Link", and the mptr has a LOCTYPE of "HANDLE"
     * and an xlink:href containing the raw Handle value.
     * <p>
     * Note that the parent Handle has to be stored here because the
     * parent is needed to create a DSpace Object when restoring the
     * AIP; it cannot be determined later once the ingester parses it
     * out of the metadata when the crosswalks are run.  So, since the
     * crosswalks require an object to operate on, and creating the
     * object requires a parent, we cannot depend on metadata processed
     * by crosswalks (e.g.  AIP techMd) for the parent, it has to be at
     * a higher level in the AIP manifest.  The structMap is an obvious
     * and standards-compliant location for it.
     * 
     * @param context DSpace context
     * @param dso Current DSpace object
     * @param params Packager Parameters
     * @param mets METS manifest
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     * @throws MetsException
     */
    @Override
    public void addStructMap(Context context, DSpaceObject dso,
                               PackageParameters params, Mets mets)
        throws SQLException, IOException, AuthorizeException, MetsException
    {
        // find parent Handle
        String parentHandle = null;
        switch (dso.getType())
        {
            case Constants.ITEM:
                parentHandle = ((Item)dso).getOwningCollection().getHandle();
                break;

            case Constants.COLLECTION:
                parentHandle = (((Collection)dso).getCommunities())[0].getHandle();
                break;

            case Constants.COMMUNITY:
                Community parent = ((Community)dso).getParentCommunity();
                if (parent == null)
                {
                    parentHandle = Site.getSiteHandle();
                }
                else
                {
                    parentHandle = parent.getHandle();
                }
           case Constants.SITE:
                break;
        }

        // Parent Handle should only be null if we are creating a site-wide AIP
        if(parentHandle!=null)
        {
            // add a structMap to contain div pointing to parent:
            StructMap structMap = new StructMap();
            structMap.setID(gensym("struct"));
            structMap.setTYPE("LOGICAL");
            structMap.setLABEL("Parent");
            Div div0 = new Div();
            div0.setID(gensym("div"));
            div0.setTYPE(PARENT_DIV_TYPE);
            div0.setLABEL("Parent of this DSpace Object");
            Mptr mptr = new Mptr();
            mptr.setID(gensym("mptr"));
            mptr.setLOCTYPE(Loctype.HANDLE);
            mptr.setXlinkHref(parentHandle);
            div0.getContent().add(mptr);
            structMap.getContent().add(div0);
            mets.getContent().add(structMap);
        }
    }

    /**
     * By default, include all bundles in AIP as content.
     * <P>
     * However, if the user specified a comma separated list of bundle names
     * via the "includeBundles" option, then check if this bundle is in that
     * list.  If it is, return true.  If it is not, return false.
     *
     * @param bundle Bundle to check for
     * @return true if bundle should be disseminated when disseminating Item AIPs
     */
    @Override
    public boolean includeBundle(Bundle bundle)
    {
        //Check the 'includeBundles' option to see if a list of bundles was provided (default = "all")
        String bundleList = this.disseminateParams.getProperty("includeBundles", "all");

        if(bundleList.equalsIgnoreCase("all"))
        {
            return true; //all bundles should be disseminated
        }
        else
        {
            //Check if this bundle is in our list of bundles to include
            String[] bundleNames = bundleList.split(",");
            for(String bundleName : bundleNames)
            {
                if(bundle.getName().equals(bundleName))
                {
                    return true;
                }
            }

            //if not in the 'includeBundles' list, then return false
            return false;
        }
    }
}
