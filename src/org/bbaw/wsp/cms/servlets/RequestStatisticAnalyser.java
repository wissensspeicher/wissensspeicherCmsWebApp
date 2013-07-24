package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.document.Token;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.query.rf.QuerySqlProvider;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Class provides an Servlet service to get matching querys or Documents of the
 * Database or inser them in the Database. If query is just one word a json with
 * usally requests ordered by rate to this word in the Database will be
 * returned. Otherwise there will be created an entry or if exists already the
 * parameter requests increased
 * 
 * @author shk2
 * 
 */
public class RequestStatisticAnalyser extends HttpServlet {
    private static final long serialVersionUID = 3711753091526093328L;

    public RequestStatisticAnalyser() throws Exception {
	super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
	super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request,
	    HttpServletResponse response) throws ServletException, IOException {
	request.setCharacterEncoding("utf-8");
	response.setCharacterEncoding("utf-8");
	String query = request.getParameter("query");
	String docUrl = request.getParameter("url");
	String type = request.getParameter("type");
	String outputFormat = request.getParameter("outputFormat");
	if (outputFormat == null)
	    outputFormat = "html";
	if (outputFormat.equals("xml"))
	    response.setContentType("text/xml");
	else if (outputFormat.equals("html") || outputFormat.equals("json"))
	    response.setContentType("text/html");
	else
	    response.setContentType("text/xml");
	final PrintWriter out = response.getWriter();
	if (query == null) {
	    out.print("no query specified: please set parameter \"query\"");
	    return;
	}
	if (type == null) {
	    type = "queries";
	}

	try {

	    query = query.toLowerCase().trim();

	    IndexHandler indexHandler = IndexHandler.getInstance();

	    String[] array = query.split("[ ]+");

	    Boolean inserInDatabase = true;

	    for (String str : array) {
		ArrayList<Token> token = indexHandler.getToken("tokenOrig",
			str, 1);

		if (token.get(0) == null) {
		    inserInDatabase = false;
		}

	    }

	    final QuerySqlProvider qsp = new QuerySqlProvider("localhost",
		    "3306", "WspCmsCore");

	    if (inserInDatabase) {

		qsp.updateQueries(query);

	    } else
		return;

	    WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
	    jsonEncoder.clear();

	    if (docUrl == null && type.equals("resources")) {

		JSONObject documents = qsp.getDocuments(query);

		if (documents != null)
		    out.println(JSONValue.toJSONString(documents));

	    } else if (docUrl == null && type.equals("queries")) {

		if (query.contains(" ")) {
		    out.print("query is longer as than as one word. Please just give one.");
		    return;
		}

		JSONObject queries = qsp.getQueries(query);
		if (queries != null)
		    out.println(JSONValue.toJSONString(queries));

	    } else {

		docUrl = docUrl.toLowerCase().trim();

		qsp.updateDocs(query, docUrl);
	    }

	    qsp.closeConnection();

	} catch (Exception e) {
	    throw new ServletException(e);
	}
    }
}
