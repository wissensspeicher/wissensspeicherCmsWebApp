package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemQueryHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class QueryMdSystem extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public QueryMdSystem() {
    super();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String query = request.getParameter("query");
    String language = request.getParameter("language");
    if (language != null && language.equals("none"))
      language = null;
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat.equals("html") || outputFormat.equals("json"))
      response.setContentType("text/html");
    else 
      response.setContentType("text/xml");
    String conceptSearch = request.getParameter("conceptSearch");
    PrintWriter out = response.getWriter();
    if (query == null) {
      out.print("no query specified: please set parameter \"query\"");
      return;
    }
    try {
      Date begin = new Date();
      //ToDo
      MdSystemQueryHandler mdQueryHandler = MdSystemQueryHandler.getInstance();
      mdQueryHandler.init();
      
      if(conceptSearch !=null && conceptSearch.equals("true")){
      final ArrayList<ConceptQueryResult> conceptHits = mdQueryHandler.getConcept(query);
      //hier hits lesen
//      String jsonResult = mdQueryHandler.queryConcepts("marx");
      
      Date end = new Date();
      long elapsedTime = end.getTime() - begin.getTime();
      String baseUrl = getBaseUrl(request);
      
      if (outputFormat.equals("json")) {
        WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
        jsonEncoder.clear();
        jsonEncoder.putStrings("searchTerm", query);
        jsonEncoder.putStrings("numberOfHits", String.valueOf(conceptHits.size()));
        JSONArray jsonOuterArray = new JSONArray();
        JSONObject jsonWrapper = null;
          for (int i=0; i<conceptHits.size(); i++) {
            out.println("**************");
            out.println("results.get(i) : "+conceptHits.get(i));
            out.println("getSet() : "+conceptHits.get(i).getAllMDFields());
            Set<String> keys = conceptHits.get(i).getAllMDFields();
            JSONArray jsonInnerArray = new JSONArray();
            for (String s : keys) {
                out.println("concepts.get(i).getValue(s) : "+conceptHits.get(i).getValue(s));
                jsonWrapper = new JSONObject();
                jsonWrapper.put(s, conceptHits.get(i).getValue(s));
                jsonInnerArray.add(jsonWrapper);
            }
            out.println("*******************");
            jsonOuterArray.add(jsonInnerArray);
          }
        
        jsonEncoder.putJsonObj("mdHits", jsonOuterArray);

        out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));
      }
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private String getBaseUrl(HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(HttpServletRequest request) {
    if ((request.getServerPort() == 80) || (request.getServerPort() == 443))
      return request.getScheme() + "://" + request.getServerName();
    else
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }
}