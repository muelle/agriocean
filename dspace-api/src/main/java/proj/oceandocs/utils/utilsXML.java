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
        if (tag.getQualifiedName().equals(limitTagName) || tag.getChildren().isEmpty())
        {
            newParent.getChildren().add(tag.clone());
        }
        else
        {
            List<Element> testlist = newParent.getChildren();

            if (testlist.isEmpty() || !haveTag(testlist,tag))
            {
                newParent.getChildren().add(tag.clone());
            }
            else
            {
                for (int i = 0; i < testlist.size(); i++)
                {
                    Element e = testlist.get(i);

                    if (equalTag((Element) e, tag))
                    {
                        for (Element c : (List<Element>) tag.getChildren())
                        {
                            mergeXMLTrees(e, c, limitTagName);
                        }
                    }
                }
            }
        }
    }

    public static boolean equalTag(Element a, Element b)
    {
        if (a.getQualifiedName().equals(b.getQualifiedName()))
        {
            if (a.getAttributes().size() != b.getAttributes().size())
            {
                return false;
            }

            for (Attribute attr : (List<Attribute>) a.getAttributes())
            {
                if (!attr.getValue().equals((b.getAttribute(attr.getName(),attr.getNamespace()) != null ? b.getAttribute(attr.getName(),attr.getNamespace()).getValue() : "")))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public static boolean haveTag(List<Element> list, Element test)
    {
        for(Element e: list)
        {
            if(equalTag(e, test))
            {
                return true;
            }
        }
        return false;
    }
}
