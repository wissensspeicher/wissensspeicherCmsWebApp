package org.bbaw.wsp.cms.servlets;

import java.sql.SQLException;
import java.util.ArrayList;

import org.bbaw.wsp.cms.mdsystem.util.WspJsonEncoder;
import org.json.simple.JSONObject;

public class QuerySqlProvider extends Tablenames {

    private final String request;
    private final MySqlConnector con;

    public QuerySqlProvider(String request, String server, String port,
	    String databasename) throws Exception {
	super();
	con = MySqlConnector.getInstance();
	if (!con.hasConnectionInfos()) {
	    con.setValues(server, port, databasename);
	}

	this.request = request;

	con.readDataBase();

    }

    public void closeConnection() {
	con.closeConnection();
    }

    public void updateQueries(String queryWord) throws SQLException {
	con.updateQueries(queryWord);
    }

    public void inserSingelElementToTable(String table, String col, String query)
	    throws SQLException {
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
