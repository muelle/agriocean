/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.utils;

import java.util.List;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 *
 * @author Denys Slipetskyy
 */
public class utilsXML
{

    public static void mergeXMLTrees(Element newParent, Element tag, String limitTagName)
    {
        Element test = newParent.getChild(tag.getName(),tag.getNamespace());
        if (test != null && equalTagExist(test, tag) && !tag.getName().equals(limitTagName))
        {
            for (Element e : (List<Element>) tag.getChildren())
            {
                mergeXMLTrees(test, e, limitTagName);
            }
        } else
        {
            newParent.getChildren().add(tag.clone());
        }

    }

    public static boolean equalTagExist(Element a, Element b)
    {
        if (a.getNamespacePrefix().equals(b.getNamespacePrefix()) && a.getName().equals(b.getName()))
        {
            if (a.getAttributes().size() != b.getAttributes().size())
            {
                return false;
            }

            for (Attribute attr : (List<Attribute>) a.getAttributes())
            {
                if (!attr.getValue().equals(b.getAttribute(attr.getName()) != null ? b.getAttribute(attr.getName()).getValue():""))
                {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
    
}

