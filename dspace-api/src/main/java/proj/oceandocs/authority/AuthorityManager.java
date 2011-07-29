/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.authority;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.dspace.core.ConfigurationManager;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.dspace.content.authority.Choice;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 *
 * @author Denys Slipetskyy
 * @version 1
 */
public class AuthorityManager {

    private static String driver = null;
    private static String url = null;
    private static String username = null;
    private static String password = null;
    private static String sql = null;
    private static Logger log = Logger.getLogger(AuthorityManager.class);
    Context ctx;

    public AuthorityManager(String authority, Context context) {

        sql = ConfigurationManager.getProperty("sql." + authority);
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
}
