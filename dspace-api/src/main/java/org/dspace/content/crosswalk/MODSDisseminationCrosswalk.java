/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Verifier;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import proj.oceandocs.utils.utilsXML;

/**
 * Configurable MODS Crosswalk
 * <p>
 * This class supports multiple dissemination crosswalks from DSpace
 * internal data to the MODS XML format
 *  (see <a href="http://www.loc.gov/standards/mods/">http://www.loc.gov/standards/mods/</a>.)
 * <p>
 * It registers multiple Plugin names, which it reads from
 * the DSpace configuration as follows:
 *
 * <h3>Configuration</h3>
 * Every key starting with <code>"crosswalk.mods.properties."</code> describes a
 * MODS crosswalk.  Everything after the last period is the <em>plugin name</em>,
 * and the value is the pathname (relative to <code><em>dspace.dir</em>/config</code>)
 * of the crosswalk configuration file.
 * <p>
 * You can have two names point to the same crosswalk,
 * just add two configuration entries with the same value, e.g.
 * <pre>
 *    crosswalk.mods.properties.MODS = crosswalks/mods.properties
 *    crosswalk.mods.properties.default = crosswalks/mods.properties
 * </pre>
 * The first line creates a plugin with the name <code>"MODS"</code>
 * which is configured from the file <em>dspace-dir</em><code>/config/crosswalks/mods.properties</code>.
 * <p>
 * Since there is significant overhead in reading the properties file to
 * configure the crosswalk, and a crosswalk instance may be used any number
 * of times, we recommend caching one instance of the crosswalk for each
 * name and simply reusing those instances.  The PluginManager does this
 * by default.
 *
 * @author Larry Stone
 * @author Scott Phillips
 * @version $Revision: 5844 $
 */
public class MODSDisseminationCrosswalk extends SelfNamedPlugin
    implements DisseminationCrosswalk
{

    /** log4j category */
    private static Logger log = Logger.getLogger(MODSDisseminationCrosswalk.class);
    private static final String CONFIG_PREFIX = "crosswalk.mods.properties.";
    /**
     * Fill in the plugin alias table from DSpace configuration entries
     * for configuration files for flavors of MODS crosswalk:
     */
    private static String aliases[] = null;

    static
    {
        List<String> aliasList = new ArrayList<String>();
        Enumeration<String> pe = (Enumeration<String>) ConfigurationManager.propertyNames();
        while (pe.hasMoreElements())
        {
            String key = pe.nextElement();
            if (key.startsWith(CONFIG_PREFIX))
            {
                aliasList.add(key.substring(CONFIG_PREFIX.length()));
            }
        }
        aliases = (String[]) aliasList.toArray(new String[aliasList.size()]);
    }

    public static String[] getPluginNames()
    {
        return (String[]) ArrayUtils.clone(aliases);
    }
    /**
     * MODS namespace.
     */
    public static final Namespace MODS_NS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    private static final Namespace XLINK_NS = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
    private static final Namespace namespaces[] =
    {
        MODS_NS, XLINK_NS
    };
    /**  URL of MODS XML Schema */
    public static final String MODS_XSD = "http://www.loc.gov/standards/mods/v3/mods-3-3.xsd";
    private static final String schemaLocation = MODS_NS.getURI() + " " + MODS_XSD;
    private static SAXBuilder builder = new SAXBuilder();
    private LinkedHashMap<String, String> modsMap = null;
    private Map<String, String> groupingLimits = null;

    /**
     * Initialize Crosswalk table from a properties file
     * which itself is the value of the DSpace configuration property
     * "crosswalk.mods.properties.X", where "X" is the alias name of this instance.
     * Each instance may be configured with a separate mapping table.
     *
     * The MODS crosswalk configuration properties follow the format:
     *
     *  {field-name} = {XML-fragment}
     *
     *  1. qualified DC field name is of the form
     *       {MDschema}.{element}.{qualifier}
     *
     *      e.g.  dc.contributor.author
     *
     *  2. XML fragment is prototype of metadata element, with empty or "%s", "%a", "%l"
     *     placeholders for value(s), authority value(s), language attribute value(s).
     *
     * Example properties line:
     *
     *  dc.description.abstract = <mods:abstract>%s</mods:abstract>
     *
     */
    private void initMap() throws CrosswalkInternalException
    {
        if (modsMap != null)
        {
            return;
        }
        String myAlias = getPluginInstanceName();
        if (myAlias == null)
        {
            log.error("Must use PluginManager to instantiate MODSDisseminationCrosswalk so the class knows its name.");
            return;
        }
        String cmPropName = CONFIG_PREFIX + myAlias;
        String propsFilename = ConfigurationManager.getProperty(cmPropName);
        if (propsFilename == null)
        {
            String msg = "MODS crosswalk missing "
                + "configuration file for crosswalk named \"" + myAlias + "\"";
            log.error(msg);
            throw new CrosswalkInternalException(msg);
        } else
        {
            String parent = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator;
            File propsFile = new File(parent, propsFilename);

            try
            {
                BufferedReader br = new BufferedReader(new FileReader(propsFile));

                modsMap = new LinkedHashMap<String, String>();
                groupingLimits = new HashMap<String, String>();
                String[] props;

                String line;
                while ((line = br.readLine()) != null)
                {
                    line = line.trim();
                    if (!line.startsWith("#") && !line.equals(""))
                    {
                        props = line.split("\\s+=\\s+");
                        if (props.length == 2)
                        {
                            String qdc = props[0].trim();
                            String val = props[1].trim();

                            String pair[] = val.split("\\s+\\|\\s+", 2);
                            if (pair.length < 1)
                            {
                                log.warn("Illegal MODS mapping in " + propsFile.toString() + ", line = "
                                    + qdc + " = " + val);
                            } else
                            {
                                modsMap.put(qdc, pair[0]);
                                if (pair.length >= 2 && (!"".equals(pair[1])))
                                {
                                    groupingLimits.put(qdc, pair[1].trim());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e)
            {
                log.error("Error opening or reading MODS properties file: " + propsFile.toString() + ": " + e.toString());
                throw new CrosswalkInternalException("MODS crosswalk cannot "
                    + "open config file: " + e.toString());
            }
        }
    }

    /**
     *  Return the MODS namespace
     */
    @Override
    public Namespace[] getNamespaces()
    {
        return (Namespace[]) ArrayUtils.clone(namespaces);
    }

    /**
     * Return the MODS schema
     */
    @Override
    public String getSchemaLocation()
    {
        return schemaLocation;
    }

    private Map<String, ArrayList<Element>> prepareTags(Map<String, ArrayList<DCValue>> metadata)
    {

        //StringBuilder result = new StringBuilder();
        //$dc.element.qualifier|s$ like constructions will be replased by value of apropriate field
        Pattern p = Pattern.compile("\\$(\\w+.\\w+.\\w+)\\|([s,a,l])\\$", Pattern.CASE_INSENSITIVE);
        Matcher m;
        DCValue tempDCV;


        String subst = "";
        Map<String, ArrayList<Element>> result = new LinkedHashMap<String, ArrayList<Element>>();

        for (String field : modsMap.keySet())
        {
            if (metadata.containsKey(field))
            {
                ArrayList<Element> elements = new ArrayList<Element>();

                for (DCValue dcv : metadata.get(field))
                {
                    StringBuffer sb = new StringBuffer();

                    String template = modsMap.get(field);
                    template = template.replace("%s", dcv.value != null ? dcv.value : "");
                    template = template.replace("%a", dcv.authority != null ? dcv.authority : "");
                    template = template.replace("%l", dcv.language != null ? dcv.language : "");

                    template = template.replace("xml:lang=\"\"", "");

                    m = p.matcher(template);
                    while (m.find())
                    {
                        if (m.groupCount() == 2)
                        {
                            tempDCV = metadata.get(m.group(1)) != null ? metadata.get(m.group(1)).get(0) : null;
                            if (tempDCV != null)
                            {
                                if ("s".equalsIgnoreCase(m.group(2)))
                                {
                                    subst = tempDCV.value != null ? tempDCV.value : "";
                                } else if ("a".equalsIgnoreCase(m.group(2)))
                                {
                                    subst = tempDCV.authority != null ? tempDCV.authority : "";
                                } else if ("l".equalsIgnoreCase(m.group(2)))
                                {
                                    subst = tempDCV.language != null ? tempDCV.language : "";
                                }
                                m.appendReplacement(sb, subst);

                            } else
                            {
                                m.appendReplacement(sb, "");
                            }
                        }
                    }
                    m.appendTail(sb);
                    try
                    {
                        Element tempRoot = builder.build(new StringReader((sb.toString()))).getRootElement();
                        elements.add(tempRoot);
                    } catch (Exception e)
                    {
                        log.error("AGRISDisseminationCrosswalk error: " + e.getLocalizedMessage());
                    }
                }

                result.put(field, elements);
            }
        }
        return result;
    }

    /**
     * Returns object's metadata in MODS format, as List of XML structure nodes.
     */
    @Override
    public List<Element> disseminateList(DSpaceObject dso) throws CrosswalkException,
        IOException, SQLException, AuthorizeException
    {
        throw new UnsupportedOperationException("MODS dissemination as list of mods tags not applicable.");
    }

    /**
     * Disseminate an Item, Collection, or Community to MODS.
     */
    @Override
    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
        IOException, SQLException, AuthorizeException
    {
        Element root = new Element("modsCollection");
        root.addNamespaceDeclaration(XLINK_NS);
        root.addNamespaceDeclaration(XSI_NS);
        root.setAttribute("schemaLocation", schemaLocation, XSI_NS);


        DCValue[] dcvs = null;
        if (dso.getType() == Constants.ITEM)
        {
            dcvs = item2Metadata((Item) dso);
        } else if (dso.getType() == Constants.COLLECTION)
        {
            dcvs = collection2Metadata((Collection) dso);
        } else if (dso.getType() == Constants.COMMUNITY)
        {
            dcvs = community2Metadata((Community) dso);
        } else if (dso.getType() == Constants.SITE)
        {
            dcvs = site2Metadata((Site) dso);
        } else
        {
            throw new CrosswalkObjectNotSupported(
                "MODSDisseminationCrosswalk can only crosswalk Items, Collections, or Communities");
        }
        initMap();

        HashMap<String, ArrayList<DCValue>> itemDCVs = new HashMap<String, ArrayList<DCValue>>();

        for (int i = 0; i < dcvs.length; i++)
        {
            String qdc = dcvs[i].schema + "." + dcvs[i].element;
            if (dcvs[i].qualifier != null)
            {
                qdc += "." + dcvs[i].qualifier;
            }

            if (!itemDCVs.containsKey(qdc))
            {
                ArrayList al = new ArrayList();
                al.add(dcvs[i]);
                itemDCVs.put(qdc, al);
            } else
            {
                itemDCVs.get(qdc).add(dcvs[i]);
            }
        }

        Map<String, ArrayList<Element>> tags = prepareTags(itemDCVs);
        ArrayList<Element> temp = null;
        String curKey = "";
        try
        {
            Element mods = new Element("mods");
            mods.setAttribute("version", "3.3");
            root.getChildren().add(mods);
            String field = "";

            for (Entry kvp : tags.entrySet())
            {
                curKey = (String) kvp.getKey();
                field = groupingLimits.get(curKey);
                temp = (ArrayList<Element>) kvp.getValue();
                for (Element e : temp)
                {
                    utilsXML.mergeXMLTrees(mods, e, field);
                }
            }
        } catch (Exception e)
        {
            log.error(getPluginInstanceName() + ": " + e.getLocalizedMessage());
        } finally
        {
            return root;
        }
    }

    /**
     * ModsCrosswalk can disseminate: Items, Collections, Communities, and Site.
     */
    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        return (dso.getType() == Constants.ITEM
            || dso.getType() == Constants.COLLECTION
            || dso.getType() == Constants.COMMUNITY
            || dso.getType() == Constants.SITE);
    }

    /**
     * ModsCrosswalk prefer's element form over list.
     */
    @Override
    public boolean preferList()
    {
        return false;
    }

    /**
     * Generate a list of metadata elements for the given DSpace
     * site.
     *
     * @param site
     *            The site to derive metadata from
     */
    protected DCValue[] site2Metadata(Site site)
    {
        List<DCValue> metadata = new ArrayList<DCValue>();

        String identifier_uri = "http://hdl.handle.net/"
            + site.getHandle();
        String title = site.getName();
        String url = site.getURL();

        if (identifier_uri != null)
        {
            metadata.add(createDCValue("identifier.uri", null, identifier_uri));
        }

        //FIXME: adding two URIs for now (site handle and URL), in case site isn't using handles
        if (url != null)
        {
            metadata.add(createDCValue("identifier.uri", null, url));
        }

        if (title != null)
        {
            metadata.add(createDCValue("title", null, title));
        }

        return (DCValue[]) metadata.toArray(new DCValue[metadata.size()]);
    }

    /**
     * Generate a list of metadata elements for the given DSpace
     * community.
     *
     * @param community
     *            The community to derive metadata from
     */
    protected DCValue[] community2Metadata(Community community)
    {
        List<DCValue> metadata = new ArrayList<DCValue>();

        String description = community.getMetadata("introductory_text");
        String description_abstract = community.getMetadata("short_description");
        String description_table = community.getMetadata("side_bar_text");
        String identifier_uri = "http://hdl.handle.net/"
            + community.getHandle();
        String rights = community.getMetadata("copyright_text");
        String title = community.getMetadata("name");

        if (description != null)
        {
            metadata.add(createDCValue("description", null, description));
        }

        if (description_abstract != null)
        {
            metadata.add(createDCValue("description", "abstract", description_abstract));
        }

        if (description_table != null)
        {
            metadata.add(createDCValue("description", "tableofcontents", description_table));
        }

        if (identifier_uri != null)
        {
            metadata.add(createDCValue("identifier.uri", null, identifier_uri));
        }

        if (rights != null)
        {
            metadata.add(createDCValue("rights", null, rights));
        }

        if (title != null)
        {
            metadata.add(createDCValue("title", null, title));
        }

        return (DCValue[]) metadata.toArray(new DCValue[metadata.size()]);
    }

    /**
     * Generate a list of metadata elements for the given DSpace
     * collection.
     *
     * @param collection
     *            The collection to derive metadata from
     */
    protected DCValue[] collection2Metadata(Collection collection)
    {
        List<DCValue> metadata = new ArrayList<DCValue>();

        String description = collection.getMetadata("introductory_text");
        String description_abstract = collection.getMetadata("short_description");
        String description_table = collection.getMetadata("side_bar_text");
        String identifier_uri = "http://hdl.handle.net/"
            + collection.getHandle();
        String provenance = collection.getMetadata("provenance_description");
        String rights = collection.getMetadata("copyright_text");
        String rights_license = collection.getMetadata("license");
        String title = collection.getMetadata("name");

        if (description != null)
        {
            metadata.add(createDCValue("description", null, description));
        }

        if (description_abstract != null)
        {
            metadata.add(createDCValue("description", "abstract", description_abstract));
        }

        if (description_table != null)
        {
            metadata.add(createDCValue("description", "tableofcontents", description_table));
        }

        if (identifier_uri != null)
        {
            metadata.add(createDCValue("identifier", "uri", identifier_uri));
        }

        if (provenance != null)
        {
            metadata.add(createDCValue("provenance", null, provenance));
        }

        if (rights != null)
        {
            metadata.add(createDCValue("rights", null, rights));
        }

        if (rights_license != null)
        {
            metadata.add(createDCValue("rights.license", null, rights_license));
        }

        if (title != null)
        {
            metadata.add(createDCValue("title", null, title));
        }

        return (DCValue[]) metadata.toArray(new DCValue[metadata.size()]);
    }

    /**
     * Generate a list of metadata elements for the given DSpace item.
     *
     * @param item
     *            The item to derive metadata from
     */
    protected DCValue[] item2Metadata(Item item)
    {
        DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
            Item.ANY);

        return dcvs;
    }

    private DCValue createDCValue(String element, String qualifier, String value)
    {
        DCValue dcv = new DCValue();
        dcv.schema = "dc";
        dcv.element = element;
        dcv.qualifier = qualifier;
        dcv.value = value;
        return dcv;
    }

    // check for non-XML characters
    private String checkedString(String value)
    {
        if (value == null)
        {
            return null;
        }
        String reason = Verifier.checkCharacterData(value);
        if (reason == null)
        {
            return value;
        } else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Filtering out non-XML characters in string, reason=" + reason);
            }
            StringBuilder result = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); ++i)
            {
                char c = value.charAt(i);
                if (Verifier.isXMLCharacter((int) c))
                {
                    result.append(c);
                }
            }
            return result.toString();
        }
    }
}
