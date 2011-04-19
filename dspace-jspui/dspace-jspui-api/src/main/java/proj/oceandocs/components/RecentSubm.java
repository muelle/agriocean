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
 * Class to generate the recent submissions list
 */

package proj.oceandocs.components;


import org.dspace.content.Item;
import java.sql.*;
import java.util.List;
import org.dspace.core.Context;

public class RecentSubm {
	public static String GenerateHTML(Context context, int amount,String contextPath) throws SQLException{
		String layout = "";
                List<String> result;
                String citation="";
		for(int i = 0;i<amount;i++){
			int latestId = Item.latestAdditionsId(context)[i];
			if(latestId != 0){ 
				layout+="<tr><td class=\"latestLayout\">";

				//String handle = Item.getHandleMod(context,latestId);
				layout += "<a href=\"" + contextPath + "/handle/"+ Item.getHandleMod(context,latestId) +"\">";
				
                                result = Item.latestAdditionsText(context,latestId,"title","");
                                if(!result.isEmpty())
                                    layout += result.get(0);
                                layout +="</a><br />";

                                result = Item.latestAdditionsText(context,latestId,"identifier","citation");
                                if(!result.isEmpty())
                                    citation = result.get(0);
                                else
                                    citation = "";
				if(citation != null && !citation.equals("")){
					layout +=citation +"<br />";
				}

                                result = Item.latestAdditionsText(context,latestId,"contributor","author");
				for(int t=0;t < result.size(); t++)
                                {
                                    layout += "<a href=\"" + contextPath + "/browse?type=author&amp;value=" + result.get(t) +"\"> "+
                                    result.get(t) + "</a>";
				}
				layout +="</td><td align=\"right\" valign=\"top\" width=\"10px\" class=\"latestLayout\">";
                                result = Item.latestAdditionsText(context,latestId,"type","");
                                if(!result.isEmpty())
                                    layout += result.get(0);
                                layout += "</td></tr>";
			}
		}
		return layout;
	}
}