/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import proj.oceandocs.utils.utilsXML;

/**
 * Configurable AGRIS Crosswalk
 * <p>
 * This class supports multiple dissemination crosswalks from DSpace
 * internal data to the AGRIS XML format
 *  (see <a href="http://www.fao.org/agris/tools/AGRIS_AP/WhatItIs.htm">http://www.fao.org/agris/tools/AGRIS_AP/WhatItIs.htm</a>.)
 * <p>
 * It registers multiple Plugin names, which it reads from
 * the DSpace configuration as follows:
 *
 * <h3>Configuration</h3>
 * Every key starting with <code>"crosswalk.argis.properties."</code> describes a
 * AGRIS crosswalk.  Everything after the last period is the <em>plugin name</em>,
 * and the value is the pathname (relative to <code><em>dspace.dir</em>/config</code>)
 * of the crosswalk configuration file.
 * <p>
 * You can have two names point to the same crosswalk,
 * just add two configuration entries with the same value, e.g.
 * <pre>
 *    crosswalk.argis.properties.ARGIS = crosswalks/argis.properties
 *    crosswalk.argis.properties.default = crosswalks/argis.properties
 * </pre>
 * The first line creates a plugin with the name <code>"ARGIS"</code>
 * which is configured from the file <em>dspace-dir</em><code>/config/crosswalks/argis.properties</code>.
 * <p>
 * Since there is significant overhead in reading the properties file to
 * configure the crosswalk, and a crosswalk instance may be used any number
 * of times, we recommend caching one instance of the crosswalk for each
 * name and simply reusing those instances.  The PluginManager does this
 * by default.
 */
public class AGRISDisseminationCrosswalk extends SelfNamedPlugin implements DisseminationCrosswalk
{

    /** log4j category */
    private static Logger log = Logger.getLogger(AGRISDisseminationCrosswalk.class);
    private final static String CONFIG_PREFIX = "crosswalk.agris.properties.";
    /**
     * Fill in the plugin alias table from DSpace configuration entries
     * for configuration files for flavors of ARGIS crosswalk:
     */
    private static String aliases[] = null;
    /**ARN number staff*/
    private static String ARNcountry = "";
    private static String ARNinstitute = "";
//    private static String AsfaURI = "";
//    private static String AgrovocURI = "";
    /**
     * ARGIS namespace.
     */
//    public static final Namespace AGRIS_NS =
//            Namespace.getNamespace("agris", "http://purl.org/agmes/agrisap/schema");
    public static final Namespace AGS_NS =
        Namespace.getNamespace("ags", "http://purl.org/agmes/1.1/");
    public static final Namespace AGLS_NS =
        Namespace.getNamespace("agls", "http://www.naa.gov.au/recordkeeping/gov_online/agls/1.2");
    public static final Namespace DC_NS =
        Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
    public static final Namespace DCTERMS_NS =
        Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static final Namespace namespaces[] =
    {
        /*AGRIS_NS,*/ AGS_NS, AGLS_NS, DC_NS, DCTERMS_NS
    };
    /**  URL of ARGIS XML Schema */
//    public static final String AGRIS_XSD = "http://www.fao.org/agris/agmes/schemas/agrisap.xsd";
//    private static final String schemaLocation = AGRIS_NS.getURI() + " " + AGRIS_XSD;
    //private static XMLOutputter XMLoutputer = new XMLOutputter(Format.getPrettyFormat());
    private static SAXBuilder builder = new SAXBuilder();
    private LinkedHashMap<String, String> agrisMap = null;
    private Map<String, String> groupingLimits = null;
    //private LinkedHashMap<String, String> elementsSequence = null;

    static
    {
        List aliasList = new ArrayList();
        Enumeration pe = ConfigurationManager.propertyNames();
        while (pe.hasMoreElements())
        {
            String key = (String) pe.nextElement();
            if (key.startsWith(CONFIG_PREFIX))
            {
                aliasList.add(key.substring(CONFIG_PREFIX.length()));
            }
        }
        aliases = (String[]) aliasList.toArray(new String[aliasList.size()]);

        /** get some parameters to generate ARN number later */
        ARNcountry = ConfigurationManager.getProperty("ARN.coutrycode");
        ARNinstitute = ConfigurationManager.getProperty("ARN.institutecode");

    }

    public static String[] getPluginNames()
    {
        return aliases;
    }

    /**
     * Initialize Crosswalk table from a properties file
     * which itself is the value of the DSpace configuration property
     * "crosswalk.agris.properties.X", where "X" is the alias name of this instance.
     * Each instance may be configured with a separate mapping table.
     *
     * The ARGIS crosswalk configuration properties follow the format:
     *
     *  {field-name} = {XML-fragment} | {XPath}
     *
     *  1. qualified DC field name is of the form
     *       {MDschema}.{element}.{qualifier}
     *
     *      e.g.  dc.contributor.author
     *
     *  2. XML fragment is prototype of metadata element, with empty or "%s"
     *     placeholders for value(s).  NOTE: Leave the %s's in because
     *     it's much easier then to see if something is broken.
     *
     *  3. XPath expression listing point(s) in the above XML where
     *     the value is to be inserted.  Context is the element itself.
     *
     * Example properties line:
     *
     *  dc.description.abstract = <dc:description><dcterms:abstract xml:lang="eng">%s</dcterms:abstract></dc:description> | dcterms:abstract/text()
     *
     */
    private void initMap()
        throws CrosswalkInternalException
    {
        if ((agrisMap != null) && (agrisMap.size() > 0))
        {
            return;
        }
        String myAlias = getPluginInstanceName();
        if (myAlias == null)
        {
            log.error("Must use PluginManager to instantiate AGRISDisseminationCrosswalk so the class knows its name.");
            return;
        }

        String cmPropName = CONFIG_PREFIX + myAlias;
        String propsFilename = ConfigurationManager.getProperty(cmPropName);
        if (propsFilename == null)
        {
            String msg = "ARGIS crosswalk missing " + "configuration file for crosswalk named \"" + myAlias + "\"";
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

                agrisMap = new LinkedHashMap<String, String>();
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
                                log.warn("Illegal ARGIS mapping in " + propsFile.toString() + ", line = "
                                    + qdc + " = " + val);
                            } else
                            {
                                agrisMap.put(qdc, pair[0]);
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
                log.error("Error opening or reading ARGIS properties file: " + propsFile.toString() + ": " + e.toString());
                throw new CrosswalkInternalException("ARGIS crosswalk cannot "
                    + "open config file: " + e.toString());
            }
        }
    }

    @Override
    public Namespace[] getNamespaces()
    {
        return namespaces;
    }

    @Override
    public String getSchemaLocation()
    {
        return "http://purl.org/agmes/agrisap/schema";
    }

    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        if (dso.getType() == Constants.ITEM)
        {
            return true;
        } else
        {
            return false;
        }
    }

    @Override
    public boolean preferList()
    {
        return false;
    }

    private Map<String, ArrayList<Element>> prepareTags(Map<String, ArrayList<DCValue>> metadata)
    {
        final String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ags:resources "
            + "xmlns:" + AGS_NS.getPrefix() + "=\"" + AGS_NS.getURI() + "\" "
            + "xmlns:" + DC_NS.getPrefix() + "=\"" + DC_NS.getURI() + "\" "
            + "xmlns:" + AGLS_NS.getPrefix() + "=\"" + AGLS_NS.getURI() + "\" "
            + "xmlns:" + DCTERMS_NS.getPrefix() + "=\"" + DCTERMS_NS.getURI() + "\">";
        final String postlog = "</ags:resources>";

        //$dc.element.qualifier|s$ like constructions will be replased by value of apropriate field
        Pattern p = Pattern.compile("\\$(\\w+.\\w+.\\w+)\\|([s,a,l])\\$", Pattern.CASE_INSENSITIVE);
        Matcher m;
        DCValue tempDCV;


        String subst = "";
        Map<String, ArrayList<Element>> result = new LinkedHashMap<String, ArrayList<Element>>();

        for (String field : agrisMap.keySet())
        {
            if (metadata.containsKey(field))
            {
                ArrayList<Element> elements = new ArrayList<Element>();

                for (DCValue dcv : metadata.get(field))
                {
                    StringBuffer sb = new StringBuffer();
                    sb.append(prolog);
                    String template = agrisMap.get(field);
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
                    sb.append(postlog);
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

    @Override
    public List<Element> disseminateList(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        throw new UnsupportedOperationException("AGRIS dissemination as list of resources tags not applicable.");
    }

    @Override
    public Element disseminateElement(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        Item item = (Item) dso;
        initMap();
        DCValue[] dc;

        Element root = new Element("resources", AGS_NS);
        //root.setAttribute("schemaLocation", schemaLocation, XSI_NS);
        root.addNamespaceDeclaration(DC_NS);
        root.addNamespaceDeclaration(DCTERMS_NS);
        root.addNamespaceDeclaration(AGLS_NS);
        root.addNamespaceDeclaration(AGS_NS);

        Element resource = new Element("resource", AGS_NS);

        String arn = "", year = "";
        dc = item.getMetadata("dc", "identifier", "arn", Item.ANY); // if ARN already put by user we will use it, if not then generate
        if ((dc != null) && (dc.length > 0) && (dc[0].value != null) && ("".equals(dc[0].value)))
        {
            arn = dc[0].value;
        } else
        {
            dc = item.getMetadata("dc", "date", "issued", Item.ANY);
            if (dc.length > 0)
            {
                year = dc[0].value.split("-")[0];
            }

            arn = ARNcountry + year + ARNinstitute + String.format("%5s", ((item.getHandle().split("/").length == 2) ? item.getHandle().split("/")[1] : "")).replace(' ', '0');
        }

        if (!"".equals(arn))
        {
            resource.getAttributes().add(new Attribute("ARN", arn, AGS_NS));
        }

        HashMap<String, ArrayList<DCValue>> itemDCVs = new HashMap<String, ArrayList<DCValue>>();

        DCValue[] dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

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
        List children;
        String curKey = "";
        try
        {
            String field = "";
            for (Entry kvp : tags.entrySet())
            {
                curKey = (String) kvp.getKey();
                field = groupingLimits.get(curKey);
                temp = (ArrayList<Element>) kvp.getValue();
                for (Element e : temp)
                {
                    children = e.getChildren();
                    if (children != null && children.size() > 0)
                    {
                        utilsXML.mergeXMLTrees(resource, (Element) children.get(0), field);
                    }
                }
            }
            root.addContent(resource);

        } catch (Exception e)
        {
            log.error(getPluginInstanceName() + ": " + e.getLocalizedMessage());
        } finally
        {
            return root;
        }
    }
}
