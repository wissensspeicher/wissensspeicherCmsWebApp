package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.lucene.document.Fieldable;

import org.bbaw.wsp.cms.document.Document;
import org.bbaw.wsp.cms.document.Hits;
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
    String additionalInfo = request.getParameter("addInf");
    String translate = request.getParameter("translate");
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
      Boolean translateBool = false;
      if (translate != null)
        translateBool = true;
      Hits hits = indexHandler.queryDocuments(query, language, from, to, true, translateBool);
      ArrayList<org.bbaw.wsp.cms.document.Document> docs = null;
      if (hits != null)
        docs = hits.getHits();
      int hitsSize = -1;
      int docsSize = -1;
      if (hits != null)
        hitsSize = hits.getSize();
      if (docs != null)
        docsSize = docs.size();
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
        out.print("<hitsSize>" + hitsSize + "</hitsSize>");
        out.print("<hits>");
        for (int i=0; i<docsSize; i++) {
          org.bbaw.wsp.cms.document.Document doc = docs.get(i);
          out.print("<doc>");
          String docId = doc.getFieldable("docId").stringValue();
          out.print("<docId>" + docId + "</docId>");
          Fieldable docUriField = doc.getFieldable("uri");
          if (docUriField != null) {
            String docUri = docUriField.stringValue();
            out.print("<uri>" + docUri + "</uri>");
          }
          Fieldable docCollectionNamesField = doc.getFieldable("collectionNames");
          if (docCollectionNamesField != null) {
            String docCollectionNames = docCollectionNamesField.stringValue();
            out.print("<collectionNames>" + docCollectionNames + "</collectionNames>");
          }
          out.print("</doc>");
        }
        out.print("</hits>");
        out.print("</result>");
      } else if (outputFormat.equals("json")) {
        WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
        jsonEncoder.clear();
        jsonEncoder.putStrings("searchTerm", query);
        jsonEncoder.putStrings("numberOfHits", String.valueOf(hitsSize));
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<docsSize; i++) {
          JSONObject jsonWrapper = new JSONObject();
          org.bbaw.wsp.cms.document.Document doc = docs.get(i);
          Fieldable docCollectionNamesField = doc.getFieldable("collectionNames");
          if (docCollectionNamesField != null) {
            jsonWrapper.put("collectionName", docCollectionNamesField.stringValue());
          }
          Fieldable docIdField = doc.getFieldable("docId");
          if(docIdField != null){
            jsonWrapper.put("docId", docIdField.stringValue());
          }
          Fieldable docUriField = doc.getFieldable("uri");
          if (docUriField != null) {
            String docUri = docUriField.stringValue();
            jsonWrapper.put("uri", docUri);
          }
          Fieldable docAuthorField = doc.getFieldable("author");
          if (docAuthorField != null) {
            jsonWrapper.put("author", docAuthorField.stringValue());
          }
          Fieldable docTitleField = doc.getFieldable("title");
          if (docTitleField != null) {
            jsonWrapper.put("title", docTitleField.stringValue());
          }
          Fieldable docDateField = doc.getFieldable("date");
          if (docDateField != null) {
            jsonWrapper.put("date", docDateField.stringValue());
          }
          Fieldable docPageCountField = doc.getFieldable("pageCount");
          if (docPageCountField != null) {
            jsonWrapper.put("pageCount", docPageCountField.stringValue());
          }
          ArrayList<String> hitFragments = doc.getHitFragments();
          JSONArray jasonFragments = new JSONArray();
          if (hitFragments != null) {
            for (int j = 0; j < hitFragments.size(); j++) {
              String hitFragment = hitFragments.get(j);
              jasonFragments.add(hitFragment);
            }
          }
          jsonWrapper.put("fragments", jasonFragments);
          
          if(additionalInfo != null) {
            if(additionalInfo.equals("true")) {
              JSONArray jsonNames = new JSONArray();
              Hits persHits = indexHandler.queryDocument(docIdField.stringValue(), "elementName:persName", 0, 100);
              ArrayList<Document> namesList = persHits.getHits();
              for (Document nameDoc : namesList) {
                Fieldable docPersNameField = nameDoc.getFieldable("xmlContent");
                if (docPersNameField != null) {
                  String docPersName = docPersNameField.stringValue();
                  String persNameAttribute = docPersName.replaceAll("<persName name=\"(.+)\".*>", "$1");
                  if(persNameAttribute.contains("</persName>"))
                    persNameAttribute = persNameAttribute.replace("</persName>", "");
                  persNameAttribute = persNameAttribute.trim();
                  //TODO evtl auch den inhalt der tags mit auswerten, nicht nur das attribut
                  String persNameContent = docPersName.replaceAll("<persName.*?>(.*)</persName>", "$1");    
                  JSONObject nameAndLink = new JSONObject();
                  //TODO was tun mit dupilaten?
                  nameAndLink.put(persNameAttribute, "http://pdrdev.bbaw.de/concord/1-4/?n="+URIUtil.encodeQuery(persNameAttribute));
                  jsonNames.add(nameAndLink);
                }
              }
              jsonWrapper.put("persNames", jsonNames);
              JSONArray jsonPlaces = new JSONArray();
              Hits placeHits = indexHandler.queryDocument(docIdField.stringValue(), "elementName:placeName", 0, 100);
              ArrayList<Document> placeList = placeHits.getHits();
              for (Document placeDoc : placeList) {
                Fieldable docPlaceField = placeDoc.getFieldable("xmlContent");
                if (docPlaceField != null) {
                  String docPlace = docPlaceField.stringValue();
                  String placeAttribute = docPlace.replaceAll("<placeName name=\"(.+)\".*>", "$1");
                  if(placeAttribute.contains("</placeName>"))
                    placeAttribute = placeAttribute.replace("</placeName>", "");
                  placeAttribute = placeAttribute.trim();
                  //TODO evtl auch den inhalt der tags mit auswerten, nicht nur das attribut
                  String placeContent = docPlace.replaceAll("<placeName.*?>(.*)</placeName>", "$1"); 
                  if(placeAttribute.contains("</placeName>"))
                    placeAttribute = placeAttribute.replace("</placeName>", "");
                  //TODO was tun mit dupilaten?
                  jsonPlaces.add(placeAttribute);
                }
              }
              jsonWrapper.put("placeNames", jsonPlaces);
            }
          }
          jsonArray.add(jsonWrapper);
        }
        jsonEncoder.putJsonObj("hits", jsonArray);
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
