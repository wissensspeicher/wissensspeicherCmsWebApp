package org.bbaw.wsp.cms.servlets;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.tst.TSTLookup;
import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Suggest extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static Hashtable<String,String> stopwords=new Hashtable<String,String>();
  
  
  private boolean readStopwordlist(){
	  try {
		BufferedReader br = new BufferedReader(new FileReader(Constants.getInstance().getExternalDataDir()+"/suggest/suggest"));
		String x;
		while((x=br.readLine())!=null){
			x=x.trim().toLowerCase();
			stopwords.put(x, x);
		}
		br.close();
		
	} catch (Exception e) {
		return false;
	}
	  return true;
  }
  
  public Suggest() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
	super.init(config);
	readStopwordlist();
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String query = request.getParameter("query");
    String countStr = request.getParameter("count");
    final String minFreq = request.getParameter("frequency"); // min. frequency of words to be listed in result JSON
    int frequency = (minFreq!= null)? Integer.parseInt(minFreq):2; // min. frequency can be specified, default value is 2
    String suggestJSON = request.getParameter("clear"); // if true, returns a wellformed sorted JSON Array for the google like suggest function
    
    
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
        if(suggestJSON!=null && suggestJSON.equals("true")){
        	JSONArray jsonSuggestions = new JSONArray();
        	
        	ArrayList<Entry<Integer, String>> list = new ArrayList<Entry<Integer,String>>();
        	String mykey;
        	for (int x=0 ;x<suggestions.size();x++){
        		LookupResult lookupres = suggestions.get(x);
        		mykey=lookupres.key;
        		
        		if(lookupres.value>=frequency&&!stopwords.containsKey(mykey)){
        			
        			list.add(new AbstractMap.SimpleEntry<Integer, String>((int)lookupres.value,mykey));
        			        			
        		}
        		
        	}
        	//list is sorted descendingly by frequency        	
        	list.sort(new Comparator<Map.Entry<Integer, String>>(){

				@Override
				public int compare(Entry<Integer, String> o1, Entry<Integer, String> o2) {
					int a = o1.getKey();
					int b = o2.getKey();
					return (a==b)?0:((a<b)?1:-1);
				}
        		
        	});
        	
        	for(int x=0;x<list.size();x++){
        		JSONObject jsonob = new JSONObject();
        		jsonob.put("key", list.get(x).getValue());
        		jsonSuggestions.add(jsonob);
        	}
        	
        	
        	
        	jsonEncoder.putJsonObj("suggestions", jsonSuggestions);
        	
        }
        else{
        	
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
        }
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
