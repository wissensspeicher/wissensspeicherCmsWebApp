package org.bbaw.wsp.cms.servlets;

import java.sql.SQLException;
import java.util.ArrayList;

import org.bbaw.wsp.cms.mdsystem.util.WspJsonEncoder;
import org.json.simple.JSONObject;

public class QuerySqlProvider extends Tablenames {

    private final MySqlConnector con;

    @SuppressWarnings("static-access")
    public QuerySqlProvider(String server, String port, String databasename)
	    throws Exception {
	super();
	con = MySqlConnector.getInstance();
	if (!con.hasConnectionInfos()) {
	    con.setValues(server, port, databasename);
	    con.readDataBase();
	}

	con.readDataBase();

    }

    public void closeConnection() {
	con.closeConnection();
    }

    public void updateQueries(String query) throws SQLException {

	String queryWord = query.split("[ ]+")[0];
	System.out.println(queryWord);

	con.inserSingelElementToTable(QUERY_WORDS, QUERY_WORDS_COL, queryWord);

	con.inserSingelElementToTable(QUERIES, QUERIES_COL, query);

	con.updateQueries(queryWord);
    }

    public void updateDocs(String query, String docUrl) throws SQLException {

	con.inserSingelElementToTable(RELEVANT_DOCS, RELEVANT_DOCS_COL, docUrl);

	con.updateDocs(query, docUrl);
    }

    public void inserSingelElementToTable(String table, String col, String query)
	    throws SQLException {
	con.inserSingelElementToTable(table, col, query);
    }

    @SuppressWarnings("static-access")
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

    public JSONObject getDocuments(String query) throws SQLException {
	ArrayList<String> temp = con.getDocmuents(query);

	WspJsonEncoder coder = new WspJsonEncoder();

	int i = 0;
	for (String string : temp) {
	    coder.putStrings("" + ++i, string);
	}

	return coder.getJsonObject();

    }

}
