package org.bbaw.wsp.cms.servlets;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;

import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONObject;

/**
 * Class is used to split an incoming Request and match it with the Database
 * 
 * @author shk2
 * 
 */
public class RequestStatisticAnalyser extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 3711753091526093328L;
    private final String request;
    private final MySqlConnector con;

    public static void main(String[] args) {

	try {

	    RequestStatisticAnalyser rsa = new RequestStatisticAnalyser(
		    "Marx Mega", "localhost", "3306", "WspCmsCore");

	    rsa.inserSingelElementToTable(Tablenames.QUERIES,
		    Tablenames.QUERIES_COL, "Marx Mega");

	    // System.out.println(con.getID(RELEVANT_DOCS,
	    // "Http://Bla und so"));
	    rsa.updateQueries("Marx");

	    System.out.println(rsa.getQueries("Marx"));

	    rsa.closeConnection();

	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public RequestStatisticAnalyser(String request, String server, String port,
	    String databasename) throws Exception {
	super();
	con = new MySqlConnector(server, port, databasename);
	this.request = request;

	con.readDataBase();

    }

    public void closeConnection() {
	con.closeConnection();
    }

    public void updateQueries(String queryWord) throws SQLException {
	con.updateQueries(queryWord);
    }

    private void inserSingelElementToTable(String table, String col,
	    String query) throws SQLException {
	con.inserSingelElementToTable(table, col, query);
    }

    public void readDataBase() throws Exception {
	con.readDataBase();
    }

    public JSONObject getQueries(String queryWord) throws SQLException {
	ArrayList<String> temp = con.getQueries(queryWord);

	WspJsonEncoder coder = new WspJsonEncoder();

	int i = 0;
	for (String string : temp) {
	    coder.putStrings("" + ++i, string);
	}

	return coder.getJsonObject();

    }

}
