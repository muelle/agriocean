/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.authority;
//Imports
import org.dspace.core.ConfigurationManager;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

//DC
public class ISSNManager implements ChoiceAuthority
{
//ICV

    private static String values[] =
    {
        ""
    };
    private static String labels[] =
    {
        ""
    };
    private boolean isConnected = false;
    private DBPG db = null;
    private static String driver = null;
    private static String url = null;
    private static String username = null;
    private static String password = null;
    private static String sql = null;
//DC

    public ISSNManager()
    {
        if (!isConnected)
        {
            //Lees de DB parameter uit de config file
            driver = ConfigurationManager.getProperty("db.driver");
            url = ConfigurationManager.getProperty("db.url");
            username = ConfigurationManager.getProperty("db.username");
            password = ConfigurationManager.getProperty("db.password");
            sql = ConfigurationManager.getProperty("dbissn.sql");
            //Maak een connectie aan uit de DB class
            System.out.println("dbissn sql: " + sql);
            db = new DBPG(driver, url, username, password);
            isConnected = db.getConnection();
            // sanity check
            if (!isConnected)
            {
                throw new IllegalStateException("Missing DSpace configuration keys for DBName Query");
            }
        }
    }

//getMatches
    @Override
    public Choices getMatches(String field, String query, int collection, int start, int limit, String locale)
    {
        int dflt = -1;
        List<String> issns = new ArrayList<String>();
        Choice[] v;

        if (query.length() < 2)
        {
            v = new Choice[0];
        }
        else
        {
            issns = db.getData(sql, query);
//        if (issns.isEmpty())
//            issns = db.getData(sql,"");
             v = new Choice[issns.size()];

            for (int i = 0; i < issns.size(); ++i)
            {
                String label = issns.get(i);
                String authority = label.substring(label.indexOf("(") + 1, label.indexOf(")"));
                String value = label.substring(0, label.indexOf("("));
                v[i] = new Choice(authority, value, label);
            }
        }
        return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, dflt);
    }
//getBestMatches

    @Override
    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
        for (int i = 0; i < values.length; ++i)
        {
            if (text.equalsIgnoreCase(values[i]))
            {
                Choice v[] = new Choice[1];
                v[0] = new Choice(String.valueOf(i), values[i], labels[i]);
                return new Choices(v, 0, v.length, Choices.CF_UNCERTAIN, false, 0);
            }
        }
        return new Choices(Choices.CF_NOTFOUND);
    }
//getLabel

    @Override
    public String getLabel(String field, String key, String locale)
    {
        return labels[Integer.parseInt(key)];
    }
}
