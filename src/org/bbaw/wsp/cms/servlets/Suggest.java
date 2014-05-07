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
    else if (outputFormat.equals("html"))
      response.setContentType("text/html");
    else if (outputFormat.equals("json"))
      response.setContentType("application/json");
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
          out.print("<freq>" + suggestionValue + "</freq>");
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
      } else if (outputFormat.equals("html")) {
        StringBuilder htmlStrBuilder = new StringBuilder();
        String title = "Suggest";
        htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        htmlStrBuilder.append("<html>");
        String head = 
            "<head>" + 
              "<title>" + title + "</title>" +
              "<link type=\"text/css\" rel=\"stylesheet\" href=\"https://www.gstatic.com/freebase/suggest/4_2/suggest.min.css\"/>" +
              "<script type=\"text/javascript\" src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.js\"></script>" +
              "<script type=\"text/javascript\" src=\"https://www.gstatic.com/freebase/suggest/4_2/suggest.min.js\"></script>" +
            "</head>";
        htmlStrBuilder.append(head);
        htmlStrBuilder.append("<body onload=\"prettyPrint()\">");
        htmlStrBuilder.append("<h2>Your query: " + query + "</h2>");
        htmlStrBuilder.append("<h3>WSP suggestions:</h3>");
        htmlStrBuilder.append("<ul>");
        for (int i=0; i<suggestions.size(); i++) {
          LookupResult suggestion = suggestions.get(i);
          String suggestionStr = suggestion.key;
          float suggestionValue = suggestion.value;
          htmlStrBuilder.append("<li>");
          htmlStrBuilder.append("key: " + suggestionStr + "; ");
          htmlStrBuilder.append("freq: " + suggestionValue);
          htmlStrBuilder.append("</li>");
        }
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("<h3>Freebase suggestions:</h3>");
        htmlStrBuilder.append("<input type=\"text\" id=\"freebaseSuggestInput\" value=\"" + query + "\">");
        String jsFreebaseSuggestStr =
          "<script type=\"text/javascript\">" +
            "$(function() {" +
              "$(\"#freebaseSuggestInput\").suggest({\"key\":\"AIzaSyBBJoStIWMWfWkgHIoRtLCCAg8B4ay2Vk8\",\"query\":" + "\"" + query + "\",\"lang\":\"de,en,fr,el\"" + "});" +  // {filter:'(all type:/film/director)'} 
            "});" +
          "</script>";
        htmlStrBuilder.append(jsFreebaseSuggestStr);
        String jsFocusStr = 
          "<script type=\"text/javascript\">" +
          "document.getElementById('freebaseSuggestInput').focus();" +
          "</script>";
        htmlStrBuilder.append(jsFocusStr);
        /*
        String jsFreebaseTopicStr =
          "<h3>Freebase get test topic</h3>" + 
          "<script>" + 
          "  var topic_id = '/en/san_francisco';" +
          "  var service_url = 'https://www.googleapis.com/freebase/v1/topic';" +
          "  var params = {};" +
          "  $.getJSON(service_url + topic_id + '?callback=?', params, function(topic) {" +
          "    $('<div>',{text:topic.property['/type/object/name'].values[0].text}).appendTo(document.body);" +
          "  });" +
          "</script>";
        htmlStrBuilder.append(jsFreebaseTopicStr);
        */
        htmlStrBuilder.append("</body>");
        htmlStrBuilder.append("</html>");
        out.println(htmlStrBuilder.toString());
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
