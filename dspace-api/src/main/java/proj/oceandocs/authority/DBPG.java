/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.authority;
//Imports
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBPG {
//ICV
private Connection connection;
private String driver = null;
private String url = null;
private String username = null;
private String password = null;
//DC
    public DBPG(String driver, String url, String username, String password){
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }
//getConnection
    public boolean getConnection(){
        boolean isConnected = false;
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, password);
            isConnected = true;
            System.out.println("isConnected? " + isConnected);
        } catch (SQLException ex) {
            Logger.getLogger(DBPG.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DBPG.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isConnected;
    }
//getNames
    public List getData(String sql, String key){
        //System.out.println("sql=" + sql + " key=" + key);
        List<String> results = new ArrayList<String>();
        Statement stmt = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try{
            try {
                String orderby = sql.substring(sql.indexOf("ORDER BY"));
                stmt = connection.createStatement();
                
                if (key.equals(""))
                    sql = sql.substring(0, sql.indexOf("WHERE")).concat(orderby);
                else
                    sql = sql.replace("?", "'%"+key.toLowerCase()+"%'");

                rs = stmt.executeQuery(sql);
                int nCols = rs.getMetaData().getColumnCount();
                String [] values = new String[nCols];
                try {
                    while ( rs.next() ){
                        for (int i = 0 ; i < nCols ; i++){
                            if (rs.getString(i + 1) != null)
                                values[i] = rs.getString(i + 1);
                            else
                                values[i] = "";
                        }
                        results.add(values[1] + " (" + values[0] + ")");
                    }
                } finally {
                    if (rs != null) rs.close();
                }
            } finally {
                if (pstmt != null ) pstmt.close();
            }
        } catch (SQLException ex) {
        Logger.getLogger(DBPG.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }
//closeConnection
    public void closeConnection(){
        if (connection != null){
            try {
                connection.close();
            }
            catch(SQLException sqle){
            }
        }
    }
}
