package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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

    /*
     * public static void main(String[] args) {
     * 
     * try {
     * 
     * RequestStatisticAnalyser rsa = new RequestStatisticAnalyser( "Marx Mega",
     * "localhost", "3306", "WspCmsCore");
     * 
     * rsa.inserSingelElementToTable(Tablenames.QUERIES, Tablenames.QUERIES_COL,
     * "Marx Mega");
     * 
     * // System.out.println(con.getID(RELEVANT_DOCS, // "Http://Bla und so"));
     * rsa.updateQueries("Marx");
     * 
     * System.out.println(rsa.getQueries("Marx"));
     * 
     * rsa.closeConnection();
     * 
     * } catch (Exception e) { // TODO Auto-generated catch block
     * e.printStackTrace(); } }
     */
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

    @Override
    protected void doGet(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {

	request.setCharacterEncoding("utf-8");
	response.setCharacterEncoding("utf-8");
	String query = request.getParameter("query");

	String outputFormat = request.getParameter("outputFormat");
	if (outputFormat == null)
	    outputFormat = "html";
	if (outputFormat.equals("xml"))
	    response.setContentType("text/xml");
	else if (outputFormat.equals("html") || outputFormat.equals("json"))
	    response.setContentType("text/html");
	else
	    response.setContentType("text/xml");
	PrintWriter out = response.getWriter();
	if (query == null) {
	    out.print("no query specified: please set parameter \"query\"");
	    return;
	}
	try {

	    RequestStatisticAnalyser rsa = new RequestStatisticAnalyser(
		    "Marx Mega", "localhost", "3306", "WspCmsCore");

	    rsa.inserSingelElementToTable(Tablenames.QUERIES,
		    Tablenames.QUERIES_COL, "Marx Mega");

	    // System.out.println(con.getID(RELEVANT_DOCS, //
	    // "Http://Bla und so"));
	    rsa.updateQueries("Marx");

	    out.println(JSONValue.toJSONString(rsa.getQueries(query)));

	    rsa.closeConnection();

	} catch (Exception e) {
	    throw new ServletException(e);
	}
    }

    @Override
    protected void doPost(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
	doGet(request, response);
    }

    private String getMimeType(String docId) {
	String mimeType = null;
	FileNameMap fileNameMap = URLConnection.getFileNameMap(); // map with 53
								  // entries
								  // such as
								  // "application/xml"
	mimeType = fileNameMap.getContentTypeFor(docId);
	return mimeType;
    }

    private String getBaseUrl(HttpServletRequest request) {
	return getServerUrl(request) + request.getContextPath();
    }

    private String getServerUrl(HttpServletRequest request) {
	return request.getScheme() + "://" + request.getServerName();
    }
}
