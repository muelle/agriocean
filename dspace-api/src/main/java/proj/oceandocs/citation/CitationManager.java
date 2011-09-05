/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.citation;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.util.regex.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;

/**
 *
 * @author Denys SLIPETSKYY, Dirk Leinders
 */
public class CitationManager {


    private HashMap<String, CitationTemplatesCollection> types;
    private org.w3c.dom.Document document;

    public CitationManager()
    {
        types = new HashMap<String, CitationTemplatesCollection>();
    }

    private boolean LoadTemplates(String filename)
    {
        try
        {
            if ((new File(filename)).exists())
            {
                this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filename);
                //this.document.normalizeDocument();
                this.visitDocument();
                return true;
            } else
            {
                return false;
            }
        } catch (SAXException ex)
        {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex)
        {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (ParserConfigurationException ex)
        {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private void visitDocument()
    {
        org.w3c.dom.Element element = document.getDocumentElement();
        element.normalize();
        if ((element != null) && element.getTagName().equals("citation"))
        {
            visitElement_citation(element);
        } else
        {
            System.out.println("Wrong XML format: root elemnt <citation> is missing");
        }
    }

    private void visitElement_citation(org.w3c.dom.Element element)
    {
        org.w3c.dom.NodeList nodes = element.getChildNodes();
        String typeName;
        for (int i = 0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Node node = nodes.item(i);
            switch (node.getNodeType())
            {
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                    break;
                case org.w3c.dom.Node.ELEMENT_NODE:
                    org.w3c.dom.Element nodeElement = (org.w3c.dom.Element) node;
                    if (nodeElement.getTagName().equals("type"))
                    {
                        typeName = nodeElement.getAttribute("name");
                        types.put(typeName, visitElement_type(nodeElement, typeName));
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }
    }

    private CitationTemplatesCollection visitElement_type(org.w3c.dom.Element element, String typeName)
    {
        CitationTemplatesCollection tmpCol = new CitationTemplatesCollection();

        org.w3c.dom.NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            org.w3c.dom.Node node = nodes.item(i);
            switch (node.getNodeType())
            {
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                    break;
                case org.w3c.dom.Node.ELEMENT_NODE:
                    org.w3c.dom.Element nodeElement = (org.w3c.dom.Element) node;
                    if (nodeElement.getTagName().equals("template"))
                    {

                        CitationTemplate tmpTMPL = new CitationTemplate();

                        org.w3c.dom.NamedNodeMap attrs = nodeElement.getAttributes();
                        for (int j = 0; j < attrs.getLength(); j++)
                        {
                            org.w3c.dom.Attr attr = (org.w3c.dom.Attr) attrs.item(j);
                            if (attr.getName().equals("name"))
                            {
                                tmpTMPL.name = attr.getValue().toString();
                            }
                            if (attr.getName().equals("defining-field"))
                            {
                                tmpTMPL.definingField = attr.getValue().toString();
                            }
                        }

                        org.w3c.dom.NodeList g_nodes = nodeElement.getChildNodes();
                        for (int gg = 0; gg < g_nodes.getLength(); gg++)
                        {
                            org.w3c.dom.Node g_template = g_nodes.item(gg);
                            if (g_template.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
                            {
                                org.w3c.dom.Element g_element = (org.w3c.dom.Element) g_template;
                                if (g_element.getTagName().equals("g"))
                                {
                                    org.w3c.dom.NodeList g_nodes2 = g_template.getChildNodes();
                                    for (int gg2 = 0; gg2 < g_nodes2.getLength(); gg2++)
                                    {
                                        org.w3c.dom.Node g_template2 = g_nodes2.item(gg2);
                                        if (g_template2.getNodeType() == org.w3c.dom.Node.TEXT_NODE)
                                        {
                                            tmpTMPL.template.add(g_template2.getNodeValue());
                                        }
                                    }
                                }
                            }
                        }
                        tmpTMPL.type = typeName;
                        tmpCol.addTemplate(tmpTMPL.name, tmpTMPL);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }

        if (tmpCol.getTemplatesCount() > 0)
        {
            return tmpCol;
        } else
        {
            return null;
        }
    }

    private CitationTemplate getTemplate(String type, String name)
    {
        if (this.types.containsKey(type))
        {
            CitationTemplatesCollection tmpCol = this.types.get(type);
            if (!name.isEmpty())
            {
                return tmpCol.getTemplateByName(name);
            } else
            {
                return tmpCol.getTemplateByName("default");
            }
        } else
        {
            return null;
        }
    }

    private CitationTemplatesCollection getAllTemplatesForType(String type)
    {
        if (this.types.containsKey(type))
        {
            return this.types.get(type);
        } else
        {
            return null;
        }
    }

    private HashSet fillQuals(String type)
    {
        return fillQuals(type, "default");
    }

    private HashSet fillQuals(String type, String name)
    {
        CitationTemplate tmpTMPL = this.getTemplate(type, name);
        HashSet h = new HashSet();
        if (tmpTMPL != null)
        {
            //now find what metadata fields are in template and put them in a HashSet
            Pattern p = Pattern.compile("\\$(\\w+.\\w+)\\$", Pattern.CASE_INSENSITIVE);
            for (int i = 0; i < tmpTMPL.template.size(); i++)
            {
                Matcher m = p.matcher(tmpTMPL.template.get(i));
                while (m.find())
                {
                    h.add(m.group(m.groupCount()));
                }
            }
        }
        return h;
    }

    private String compileCitation(String type, HashMap map)
    {
        return compileCitation(type, map, "default");
    }

    private String compileCitation(String type, HashMap map, String name)
    {

        String citation = "";
        DCValue dcv = null;
        DCValue[] authors = null;
        CitationTemplate tmpTMPL = this.getTemplate(type, name);

        // do not set identifier.citation when definingField has no value
        if (map.containsKey(tmpTMPL.definingField) && !map.get(tmpTMPL.definingField).equals(""))
        {

            if (tmpTMPL != null)
            {
                Pattern p = Pattern.compile("\\$(\\w+.\\w+)\\$", Pattern.CASE_INSENSITIVE);
                for (int i = 0; i < tmpTMPL.template.size(); i++)
                {
                    Matcher m = p.matcher(tmpTMPL.template.get(i));
                    StringBuffer sb = new StringBuffer();
                    while (m.find())
                    {
                        /* if metadata field is null - must skip it with related formatting.
                         * Also in the case of authors names we need put them all (we will
                         * have DCValue[] instead of just DCValue) in the map in that case
                         */
                        if ("bibliographicCitation.authors".equalsIgnoreCase(m.group(m.groupCount())))
                        {
                            authors = (DCValue[]) map.get(m.group(m.groupCount()));
                            String allAuthorsNames = "";
                            for (int a = 0; a < authors.length; a++)
                            {
                                if (a > 0)
                                {
                                    allAuthorsNames += "; ";
                                }
                                allAuthorsNames += authors[a].value != null ? (authors[a].value) : "";

                            }
                            if (!allAuthorsNames.equals(""))
                            {
                                m.appendReplacement(sb, allAuthorsNames);
                                m.appendTail(sb);
                                citation += sb.toString();
                            }

                        } else
                        {
                            dcv = (DCValue) map.get(m.group(m.groupCount()));
                            if ((dcv != null) && (dcv.value != null)/*map.containsKey(m.group(m.groupCount()))*/)
                            {
                                m.appendReplacement(sb, dcv.value);
                                m.appendTail(sb);
                                citation += sb.toString();
                            }
                        }
                    }
                }
            }
        }
        return citation;
    }

    public String updateCitationString(Item item) throws SQLException, AuthorizeException
    {
        String citation = "";
        DCValue doctypes[] = item.getMetadata(MetadataSchema.DC_SCHEMA, "type", null, Item.ANY);

        String type = "";
        HashSet quals;
        HashMap values;

        if (doctypes.length > 0)
        {
            type = doctypes[0].value;

            if (LoadTemplates(ConfigurationManager.getProperty("dspace.dir")
                    + File.separator + "config" + File.separator + "citation-templates.xml"))
            {
                if (this.types.size() > 0)
                {
                    //find element.qualifier used in template
                    quals = fillQuals(type);
                    values = getBibliographicValues(item, quals);

                    citation = compileCitation(type, values);
                    if (!citation.equals(""))
                    {
                        item.clearMetadata(MetadataSchema.DC_SCHEMA, "identifier", "citation", Item.ANY);
                        item.addMetadata(MetadataSchema.DC_SCHEMA, "identifier", "citation", this.getDefiningFieldLanguage(item, type), citation);
                    }

                    item.update();
                }
            }
        }

        return citation;
    }

    private String getDefiningFieldLanguage(Item item, String type)
    {
        CitationTemplate tmpl = this.getTemplate(type, "");
        HashSet singletonDefiningField = new HashSet();
        singletonDefiningField.add(tmpl.definingField);
        HashMap result = this.getBibliographicValues(item, singletonDefiningField);
        return ((DCValue)result.get(tmpl.definingField)).language;
    }

//    // Added by Walter Brebels
//    private String getString(DCValue[] v)
//    {
//        if (v.length > 0)
//        {
//            return v[0].value;
//        } else
//        {
//            return null;
//        }
//    }
//
//    // Added by Walter Brebels
//    private void addLanguages(HashSet languages, DCValue v[])
//    {
//        if (v != null)
//        {
//            for (int i = 0; i < v.length; i++)
//            {
//                languages.add(v[i].language);
//            }
//        }
//    }
    /**
     * @author Walter Brebels
     * @author Denys Slipetskyy
     * @see proj.oceandocs.CitationManager
     */
    private HashMap getBibliographicValues(Item item, HashSet quals)
    {
        Iterator qit;
        DCValue v[];
        HashMap result = new HashMap();
        String qual;

        qit = quals.iterator();
        while (qit.hasNext())
        {
            qual = (String) qit.next();
//            element = qual.split("\\.")[0];
//            if (qual.split("\\.").length >= 2)
//            {
//                qualifier = qual.split("\\.")[1];
//            }
//            else
//            {
//                qualifier = "null";
//            }
            v = item.getMetadata("dc." + qual);
            if (qual.equalsIgnoreCase("bibliographicCitation.authors"))
            {
                result.put(qual, v.length > 0 ? v : null);
            } else
            {
                result.put(qual, v.length > 0 ? v[0] : null);
            }
        }
        return result;
    }

}
