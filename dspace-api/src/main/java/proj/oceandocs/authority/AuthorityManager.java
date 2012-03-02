/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 *
 * @author Denys Slipetskyy
 * @version 1.1
 */
public class AuthorityManager {

    private static String sql = null;
    private static String strictSQL = null;
    private static Logger log = Logger.getLogger(AuthorityManager.class);
    Context ctx;

    public AuthorityManager(String authority, Context context) {

        sql = ConfigurationManager.getProperty("sql." + authority);
        strictSQL = ConfigurationManager.getProperty("sql.onematch." + authority);
        ctx = context;
    }

    public ArrayList<Choice> getAutocompleteSet(String query) {
        ArrayList<Choice> v = new ArrayList<Choice>();

        try {
            TableRowIterator tri = DatabaseManager.query(ctx, sql, query);

            TableRow tr;
            while (tri.hasNext()) {
                tr = tri.next();
                v.add(new Choice(tr.getStringColumn("authority"), tr.getStringColumn("value"), ""));
            }

        } catch (SQLException ex) {
            log.error("AuthorityManager SQL error: " + ex.getLocalizedMessage());
        } finally {
            return v;
        }
    }
    
    public String getExactMatch(String query) {
        String result = "";
        try {
            TableRowIterator tri = DatabaseManager.query(ctx, strictSQL, query);

            List<TableRow> trs = tri.toList();
            
            if(trs.size() == 1) {
                result = trs.get(0).getStringColumn("authority");
            }
        } catch (SQLException ex) {
            log.error("AuthorityManager SQL error: " + ex.getLocalizedMessage());
        } finally {
            return result;
        }
    }
}
