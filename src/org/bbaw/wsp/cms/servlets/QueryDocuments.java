package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.bbaw.wsp.cms.translator.MicrosoftTranslator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class QueryDocuments extends HttpServlet {
  private static final long serialVersionUID = 1L;
  public QueryDocuments() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String query = request.getParameter("query");
    String language = request.getParameter("language");
    String pageStr = request.getParameter("page");
    if (pageStr == null)
      pageStr = "1";
    int page = Integer.parseInt(pageStr);
    String pageSizeStr = request.getParameter("pageSize");
    if (pageSizeStr == null)
      pageSizeStr = "10";
    int pageSize = Integer.parseInt(pageSizeStr);
    int from = (page * pageSize) - pageSize;  // e.g. 0
    int to = page * pageSize - 1;  // e.g. 9
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      if (language == null) {
        ArrayList<String> queryTerms = indexHandler.fetchTerms(query);
        String queryTermsStr = toString(queryTerms);
        language = MicrosoftTranslator.detectLanguageCode(queryTermsStr);
      }
      ArrayList<Document> docs = indexHandler.queryDocuments(query, language, from, to);
      ArrayList<String> fragments = indexHandler.getFragments();
      System.out.println("fragemnts size : "+fragments.size());
      int docsSize = 0;
      if (docs != null)
        docsSize = docs.size();
      if (to >= docsSize)
        to = docsSize - 1;
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html") || outputFormat.equals("json"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      if (outputFormat.equals("xml")) {
        out.print("<result>");
        out.print("<query>");
        out.print("<queryText>" + query + "</queryText>");
        out.print("<resultPage>" + page + "</resultPage>");
        out.print("<resultPageSize>" + pageSize + "</resultPageSize>");
        out.print("</query>");
        out.print("<hitsSize>" + docsSize + "</hitsSize>");
        out.print("<hits>");
        for (int i=from; i<=to; i++) {
          Document doc = docs.get(i);
          out.print("<doc>");
          String docId = doc.getFieldable("docId").stringValue();
          out.print("<docId>" + docId + "</docId>");
          Fieldable docUriField = doc.getFieldable("uri");
          if (docUriField != null) {
            String docUri = docUriField.stringValue();
            out.print("<uri>" + docUri + "</uri>");
          }
          Fieldable docProjectIdsField = doc.getFieldable("projectIds");
          if (docProjectIdsField != null) {
            String docProjectIds = docProjectIdsField.stringValue();
            out.print("<projectIds>" + docProjectIds + "</projectIds>");
          }
          out.print("</doc>");
        }
        out.print("</hits>");
        out.print("</result>");
      } else if (outputFormat.equals("json")) {
        WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
        jsonEncoder.clear();
        jsonEncoder.putStrings("searchTerm", query);
        jsonEncoder.putStrings("numberOfHits", String.valueOf(docsSize));  
        JSONArray jsonArray = new JSONArray();  
        for (int i=from; i<to; i++) {
          JSONObject jsonWrapper = new JSONObject();
          Document doc = docs.get(i);
          Fieldable docUriField = doc.getFieldable("uri");
          if (docUriField != null) {
            String docUri = docUriField.stringValue();
            jsonWrapper.put("uri", docUri);
          }
          String docId = doc.getFieldable("docId").stringValue();
          jsonWrapper.put("docId", docId);
          jsonArray.add(jsonWrapper);
        }
        //TODO
//        jsonWrapper.put("projects", doc.get("projectIds"));
        JSONArray jasonFragents = new JSONArray();
        //TODO
        for (int j = 0; j < fragments.size(); j++) {
          if (fragments.get(j) != null) {
//            out.println((fragements[j].toString()));
//            //send to Json
            jasonFragents.add(fragments.get(j).toString());
          }
        }
          jsonEncoder.putJsonObj("hits", jsonArray);
          //send Json
          out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));
      }
      out.close();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

  private String toString(ArrayList<String> queryForms) {
    String queryFormsStr = "";
    for (int i=0; i<queryForms.size(); i++) {
      String form = queryForms.get(i);
      queryFormsStr = queryFormsStr + form + " ";
    }
    if (queryForms == null || queryForms.size() == 0)
      return null;
    else
      return queryFormsStr.substring(0, queryFormsStr.length() -1); 
  }
  
}
