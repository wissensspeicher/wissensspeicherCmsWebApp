package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONValue;

/**
 * Class is used to split an incoming Request and match it with the Database
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
	    // WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
	    // jsonEncoder.clear();
	    // jsonEncoder.putStrings("searchTerm", query);

	    QuerySqlProvider qsp = new QuerySqlProvider("Marx Mega",
		    "localhost", "3306", "WspCmsCore");

	    qsp.inserSingelElementToTable(Tablenames.QUERIES,
		    Tablenames.QUERIES_COL, "Marx Mega");

	    // System.out.println(con.getID(RELEVANT_DOCS, //
	    // "Http://Bla und so"));
	    qsp.updateQueries("Marx");

	    WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
	    jsonEncoder.clear();
	    // jsonEncoder.putStrings("searchTerm", query);
	    // out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));

	    // out.println("{}");
	    out.println(JSONValue.toJSONString(qsp.getQueries(query)));

	    qsp.closeConnection();

	    out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));

	} catch (Exception e) {
	    throw new ServletException(e);
	}
    }
}
