/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * Class to generate the items list for collection with primitive & ugly paging
 */
package proj.oceandocs.components;

import java.sql.SQLException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;

public class CollectionItems
{

    public static String GenerateHTML(Context context, Collection collection, int page, String contextPath) throws SQLException
    {
        String layout = " ";
        ItemIterator itemsItr = collection.getAllItems();
        int start = (page - 1) * 10;
        if (start < 0)
        {
            start = 0;
        }
        int end = start + 10;
        int i = 0;
        Item item = null;
        DCValue[] dcvs;
        String stitle, scitation;

        while (itemsItr.hasNext())
        {
            if (i > end)
            {
                break;
            } else if (i >= start)
            {
                item = itemsItr.next();
                dcvs = item.getMetadata("dc.title");
                if (dcvs != null && dcvs.length > 0)
                {
                    stitle = dcvs[0].value != null ? dcvs[0].value : "";
                } else
                {
                    stitle = "";
                }

                dcvs = item.getMetadata("dc.identifier.citation");
                if (dcvs != null && dcvs.length > 0)
                {
                    scitation = dcvs[0].value != null ? dcvs[0].value : "";
                } else
                {
                    scitation = "";
                }

                layout += "<tr><td class=\"latestLayout\">";
                layout += "<a href=\"" + contextPath + "/handle/" + item.getHandle() + "\">";

                layout += stitle;
                layout += "</a>";


                if (!scitation.equals(""))
                {
                    layout += "<div>" + scitation + "</div>";
                }

                boolean haveauthors = false;
                layout += "<div style=\"margin-left: 0px\">";

                dcvs = item.getMetadata("dc.contributor.author");
                if (dcvs != null && dcvs.length > 0)
                {

                    for (int t = 0; t < dcvs.length; t++)
                    {
                        if (t > 0)
                        {
                            layout += "; ";
                        }
                        layout += "<a href=\"" + contextPath + "/browse?type=author&amp;value=" + dcvs[t].value + "\"> "
                            + dcvs[t].value + "</a>";
                        haveauthors = true;
                    }
                }
                dcvs = item.getMetadata("dc.contributor.editor");
                if (dcvs != null && dcvs.length > 0)
                {
                    for (int t = 0; t < dcvs.length; t++)
                    {

                        if (t > 0 || haveauthors)
                        {
                            layout += "; ";
                        }
                        layout += "<a href=\"" + contextPath + "/browse?type=author&amp;value=" + dcvs[t].value + "\"> "
                            + dcvs[t].value + "</a>";
                        haveauthors = true;
                    }
                }

                dcvs = item.getMetadata("dc.contributor.corpauthor");
                if (dcvs != null && dcvs.length > 0)
                {
                    for (int t = 0; t < dcvs.length; t++)
                    {

                        if (t > 0 || haveauthors)
                        {
                            layout += "; ";
                        }
                        layout += "<a href=\"" + contextPath + "/browse?type=author&amp;value=" + dcvs[t].value + "\"> "
                            + dcvs[t].value + "</a>";
                        haveauthors = true;
                    }
                }

                layout += "</div>";
                layout += "</td><td align=\"right\" valign=\"top\" width=\"10px\" class=\"latestLayout\">";

                dcvs = item.getMetadata("dc.type");
                if (dcvs != null && dcvs.length > 0)
                {
                    layout += dcvs[0].value;
                }
                layout += "</td></tr>";
            }else
            {
                itemsItr.next();
            }
            
            i++;
        }
        itemsItr.close();
        return layout;
    }
}