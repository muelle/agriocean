/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
 * Made by Dimitri Surinx
 * Hasselt University
 * Class to generate a item list for a specified user/collection
 * New types can be added on request (mail me at firstname.lastname@student.uhasselt.be)
 * This class can also be used to pick up authority fields from a specified user
 */
package proj.oceandocs.components;

import org.dspace.content.Item;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.dspace.core.Context;

public class BrowseAC
{

    // Show type
    static public enum Type
    {

        AUTHOR, COLLECTION
    }

    // Generate the html for a given Type and a given parameter (for example: collection id)
    public static String GenerateHTML(Context context, Type type, String param, String contextPath, String beginY, String endY) throws SQLException
    {

        // Pickup the metadata_field_id for a given element and a qualifier (from the metadatafieldregistry)
        int autId = Item.returnId(context, "contributor", "author");
        int citId = Item.returnId(context, "identifier", "citation");
        int titleId = Item.returnId(context, "title", "");
        int dateId = Item.returnId(context, "date", "issued");
        int typeId = Item.returnId(context, "type", "");
        boolean stop = false;
        String typ = "";

        //Get a vector with the IDs of the requested items
        List<Integer> ids = GetIds(context, dateId, type, param, beginY, endY);

        // actual HTML string
        String result = "";
        int currentYear = 0;

        // Construct the html string
        for (int i = 0; i < ids.size(); ++i)
        {
            String auth = "";

            // pickup the metadata from the DB
            List<String> authors = Item.latestAdditionsText(context, ids.get(i), autId, 0);
            List<String> citations = Item.latestAdditionsText(context, ids.get(i), citId);
            List<String> titles = Item.latestAdditionsText(context, ids.get(i), titleId);
            List<String> dates = Item.latestAdditionsText(context, ids.get(i), dateId);
            List<String> types = Item.latestAdditionsText(context, ids.get(i), typeId);
            int year = getYear(dates.get(0));

            // incase this items year differs from the last one, display a new year header
            if (year != currentYear)
            {
                currentYear = year;
                if (year != 0)
                {
                    result += "</ul>";
                }
                result += "<h2 class=\"dateItem\" style=\"cursor: pointer;\" onclick=\"Effect.toggle('" + year + "', 'blind')\">" + year + "</h2><ul id=\"" + year + "\">";
            }
            // incase this items type differs from the last one, display a new type header
            if (!typ.equals(types.get(0)))
            {
                typ = types.get(0);
                result += "<li><h3>" + typ + "</h3></li>";
            }

            // display all authors
            for (int j = 0; j < authors.size() && !stop; ++j)
            {
                if (j < (authors.size() - 1))
                {
                    auth += "<a href=\"" + contextPath + "/browse?type=author&amp;value=" + Item.latestAdditionsText(context, ids.get(i), "contributor", "author").get(0) + "\">" + Item.latestAdditionsText(context, ids.get(i), "contributor", "author").get(0) + "</a>; ";
                }
                else if (authors.get(j) == null)
                {

                    stop = false;
                }
                else
                {
                    auth += "<a href=\"" + contextPath + "/browse?type=author&amp;value=" + Item.latestAdditionsText(context, ids.get(i), "contributor", "author").get(0) + "\">" + Item.latestAdditionsText(context, ids.get(i), "contributor", "author").get(0) + "</a> ";
                }
            }
            // display the other important metadata
            result += "<ul class=\"collectionListItem\"><li class=\"metadataFieldValue\" style=\"list-style-type: none;\">" + auth + " (" + year + ") <em>" + "<a href=\"" + contextPath + "/handle/" + Item.getHandleMod(context, ids.get(i)) + "\"/>" + titles.get(0) + "</a><br/>" + "</em>" + citations.get(0) + "</li></ul>";
        }

        // close the year tag!
        if (currentYear != 0)
        {
            result += "</ul>";
        }

        return result;
    }

    // Extract a year from a string formed as followed (yyyy-mm-dd)
    private static int getYear(String str)
    {
        return Integer.parseInt(str.substring(0, 4));

    }
    // picks up the authority field from a user

    public static String pickupAuthorityFromAuthor(Context con, String authorName) throws SQLException
    {
//        String query = "SELECT authority FROM bi_2_dis WHERE value = ?";
//        PreparedStatement statement = con.getDBConnection().prepareStatement(query);
//        statement.setString(1, authorName);
//        ResultSet rs = statement.executeQuery();
//        while (rs.next())
//        {
//            return rs.getString("authority");
//        }
        return null;
    }
    // Get item ids from a given type and a parameter ( from example: collection, collectionid)

    private static List<Integer> GetIds(Context context, int dateId, Type type, String param, String beginY, String endY) throws SQLException
    {
        String subQuery = null;
        int typeId = Item.returnId(context, "type", "");
        String begin = "";
        String end = "";
        begin += beginY + "-01-01";
        end += endY + "-12-31";

        // Different subqueries for different types
        if (type == Type.COLLECTION)
        {
            subQuery = "(SELECT item_id FROM collection2item WHERE collection_id = ?) a";
        }
        else if (type == Type.AUTHOR)
        {
            subQuery = "(SELECT item_id FROM metadatavalue WHERE metadata_field_id = ? AND text_value = ?) a";
        }


        // Main query
        String query = "SELECT item.item_id FROM item,handle, "
            + "(SELECT metadata_value_id ,metadatavalue.item_id,metadatavalue.text_value FROM metadatavalue WHERE metadatavalue.text_value >= ? AND metadatavalue.text_value <= ? AND metadata_field_id = ? "
            + "ORDER BY metadatavalue.text_value DESC) e, (SELECT item_id, metadatavalue.text_value FROM metadatavalue WHERE metadata_field_id = ?) g, " + subQuery + " WHERE g.item_id = item.item_id AND a.item_id = item.item_id AND item.item_id = e.item_id AND item.item_id = handle.resource_id AND "
            + "handle.resource_type_id = 2 AND in_archive AND NOT withdrawn ORDER BY e.text_value DESC, g.text_value";
        List<Integer> ids = new ArrayList<Integer>();

        PreparedStatement statement = context.getDBConnection().prepareStatement(query);
        statement.setString(1, begin); // fill in boundries
        statement.setString(2, end);
        statement.setInt(3, dateId); // fill in dateId
        statement.setInt(4, typeId); // fill in type id

        // Fill in the given parameters depending on type, add custom types here incase needed!
        if (type == Type.COLLECTION)
        {
            int col = Integer.parseInt(param);
            statement.setInt(5, col);
        }
        else if (type == Type.AUTHOR)
        {
            int id = Item.returnId(context, "contributor", "author");
            statement.setInt(5, id);
            statement.setString(6, param);
        }
        ResultSet rs = statement.executeQuery();


        int i = 0;
        while (rs.next())
        {
            ids.add(rs.getInt("item_id"));
        }
        return ids;
    }
}
