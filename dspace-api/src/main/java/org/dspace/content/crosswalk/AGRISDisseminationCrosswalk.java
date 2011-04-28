/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

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
    public static final Namespace AGRIS_NS =
        Namespace.getNamespace("agris", "http://purl.org/agmes/agrisap/schema");
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
        AGRIS_NS, AGS_NS, AGLS_NS, DC_NS, DCTERMS_NS
    };
    /**  URL of ARGIS XML Schema */
    public static final String AGRIS_XSD = "http://www.fao.org/agris/agmes/schemas/agrisap.xsd";
    private static final String schemaLocation = AGRIS_NS.getURI() + " " + AGRIS_XSD;
    final String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<ags:resources xmlns:" + AGRIS_NS.getPrefix() + "=\"" + AGRIS_NS.getURI() + "\" "
        + "xmlns:" + AGS_NS.getPrefix() + "=\"" + AGS_NS.getURI() + "\" "
        + "xmlns:" + DC_NS.getPrefix() + "=\"" + DC_NS.getURI() + "\" "
        + "xmlns:" + DCTERMS_NS.getPrefix() + "=\"" + DCTERMS_NS.getURI() + "\" "
        + "xmlns:" + AGLS_NS.getPrefix() + "=\"" + AGLS_NS.getURI() + "\">";
    final String postlog = "</ags:resources>";
    //private static XMLOutputter XMLoutputer = new XMLOutputter(Format.getPrettyFormat());
    private static SAXBuilder builder = new SAXBuilder();
    private HashMap<String, String> agrisMap = null;

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
        }
        else
        {
            String parent = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator;
            File propsFile = new File(parent, propsFilename);
            Properties modsConfig = new Properties();
            try
            {
                modsConfig.load(new FileInputStream(propsFile));
            }
            catch (IOException e)
            {
                log.error("Error opening or reading ARGIS properties file: " + propsFile.toString() + ": " + e.toString());
                throw new CrosswalkInternalException("ARGIS crosswalk cannot "
                    + "open config file: " + e.toString());
            }

            agrisMap = new HashMap<String, String>();
            Enumeration pe = modsConfig.propertyNames();
            while (pe.hasMoreElements())
            {
                String qdc = (String) pe.nextElement();
                String val = modsConfig.getProperty(qdc);
                String pair[] = val.split("\\s+\\|\\s+", 2);
                if (pair.length < 1)
                {
                    log.warn("Illegal ARGIS mapping in " + propsFile.toString() + ", line = "
                        + qdc + " = " + val);
                }
                else
                {
                    agrisMap.put(qdc, pair[0]);
                }
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
        return schemaLocation;
    }

    @Override
    public boolean canDisseminate(DSpaceObject dso)
    {
        if (dso.getType() == Constants.ITEM)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean preferList()
    {
        return false;
    }

    @Override
    public List<Element> disseminateList(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Element disseminateElement(DSpaceObject dso) throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        Item item = (Item) dso;
        initMap();
        Document subdoc = null;
        Element tempRoot = null;
        Element tempElement = null;
        Element currentElement = null;
        DCValue[] dc;

        Element root = new Element("resources", AGS_NS);
        root.setAttribute("schemaLocation", schemaLocation, XSI_NS);
        root.addNamespaceDeclaration(DC_NS);
        root.addNamespaceDeclaration(DCTERMS_NS);
        root.addNamespaceDeclaration(AGLS_NS);
        root.addNamespaceDeclaration(AGS_NS);

        Element resource = new Element("resource", AGS_NS);

        String arn = ARNcountry;
        dc = item.getMetadata("dc", "date", "issued", Item.ANY);
        if (dc.length > 0)
        {
            arn = arn + dc[0].value;
        }
        arn = arn + ARNinstitute + Integer.toString(item.getID());
        resource.getAttributes().add(new Attribute("ARN", arn, AGS_NS));

        String template = "";

        dc = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        //List result = new ArrayList(dc.length);
        for (int i = 0; i < dc.length; i++)
        {
            /** Compose qualified DC name - schema.element[.qualifier]
            e.g. "dc.title", "dc.subject.lcc", "lom.Classification.Keyword"*/
            String qdc = dc[i].schema + "."
                + ((dc[i].qualifier == null) ? dc[i].element
                : (dc[i].element + "." + dc[i].qualifier));

            if (agrisMap.containsKey(qdc))
            {
                template = agrisMap.get(qdc);
                if (template.equals(""))
                {
                    continue;
                }

                if (dc[i].value != null)
                {
                    template = template.replace("%s", dc[i].value);
                }
                else
                {
                    template = template.replace("%s", "");
                }

                if (dc[i].authority != null)
                {
                    template = template.replace("%a", dc[i].authority);
                }
                else
                {
                    template = template.replace("%a", "");
                }

                if (dc[i].language != null)
                {
                    template = template.replace("%l", dc[i].language);
                }
                else
                {
                    template = template.replace("%l", "");
                }

                try
                {
                    subdoc = builder.build(new StringReader(prolog + template + postlog));
                    if (subdoc != null)
                    {
                        if (!subdoc.getRootElement().getChildren().isEmpty())
                        {
                            tempRoot = (Element) subdoc.getRootElement().getChildren().get(0);
                            if (tempRoot.getChildren().isEmpty())
                            {
                                resource.addContent(tempRoot.detach());
                            }
                            else
                            {
                                if (tagExist(resource, tempRoot))
                                {
                                    tempElement = resource.getChild(tempRoot.getName(), tempRoot.getNamespace());
                                    Iterator itr = tempRoot.getChildren().iterator();
                                    while(itr.hasNext())
                                    {
                                        currentElement = (Element) itr.next();
                                        tempElement.addContent((Element)currentElement.clone());
                                    }
                                }
                                else
                                {
                                    resource.addContent(tempRoot.detach());
                                }
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    java.util.logging.Logger.getLogger(AGRISDisseminationCrosswalk.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else
            {
                log.warn("WARNING: " + getPluginInstanceName() + ": No ARGIS mapping for \"" + qdc + "\"");
            }
        }

        root.addContent(resource);
        return root;
    }

    private boolean tagExist(Element parent, Element tag)
    {
        Element child;
        Iterator itr = parent.getChildren().iterator();
        while (itr.hasNext())
        {
            child = (Element) itr.next();
            if (child.getNamespacePrefix().equals(tag.getNamespacePrefix()) && child.getName().equals(tag.getName()))
            {
                return true;
            }
        }
        return false;
    }
}
