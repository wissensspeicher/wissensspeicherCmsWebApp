package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.tst.TSTLookup;

import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Suggest extends HttpServlet {
  private static final long serialVersionUID = 1L;
  
  public Suggest() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String query = request.getParameter("query");
    String countStr = request.getParameter("count");
    int count = 10;
    if (countStr != null)
      count = Integer.parseInt(countStr);
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "json";
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
      TSTLookup suggester = IndexHandler.getInstance().getSuggester();
      List<LookupResult> suggestions = suggester.lookup(query, false, count);
      if (outputFormat.equals("xml")) {
        out.print("<result>");
        out.print("<query>" + query + "</query>");
        out.print("<suggestions>");
        for (int i=0; i<suggestions.size(); i++) {
          LookupResult suggestion = suggestions.get(i);
          String suggestionStr = suggestion.key;
          float suggestionValue = suggestion.value;
          out.print("<suggestion>");
          out.print("<key>" + suggestionStr + "</key>");
          out.print("<freq>" + suggestionValue + "</key>");
          out.print("</suggestion>");
        }
        out.print("</suggestions>");
        out.print("</result>");
      } else if (outputFormat.equals("json")) {
        WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
        jsonEncoder.clear();
        jsonEncoder.putStrings("query", query);
        JSONArray jsonSuggestions = new JSONArray();
        for (int i=0; i<suggestions.size(); i++) {
          JSONObject jsonSuggestion = new JSONObject();
          LookupResult suggestion = suggestions.get(i);
          String suggestionStr = suggestion.key;
          int suggestionValue = (int) suggestion.value;
          jsonSuggestion.put("key", suggestionStr);
          jsonSuggestion.put("freq", String.valueOf(suggestionValue));
          jsonSuggestions.add(jsonSuggestion);
        }
        jsonEncoder.putJsonObj("suggestions", jsonSuggestions);
        out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
