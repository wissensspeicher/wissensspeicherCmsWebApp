package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.lucene.index.IndexableField;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.bbaw.wsp.cms.document.Document;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class MoreLikeThis extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
  public MoreLikeThis() {
    super();
  }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	  request.setCharacterEncoding("utf-8");
	  response.setCharacterEncoding("utf-8");
	  String additionalInfo = request.getParameter("addInf");
	  String outputFormat = request.getParameter("outputFormat");
	  if (outputFormat == null)
	    outputFormat = "html";
    String docId = request.getParameter("docId");
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
    PrintWriter out = response.getWriter();
    IndexHandler indexHandler;
    try {
      Date begin = new Date();
      indexHandler = IndexHandler.getInstance();
      Hits hits = indexHandler.moreLikeThis(docId, from, to);
      ArrayList<Document> docs = null;
      if (hits != null)
        docs = hits.getHits();
      int hitsSize = -1;
      int docsSize = -1;
      if (hits != null)
        hitsSize = hits.getSize();
      if (docs != null)
        docsSize = docs.size();
      Date end = new Date();
      long elapsedTime = end.getTime() - begin.getTime();
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html") || outputFormat.equals("json"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      if (outputFormat.equals("xml")) {
        out.print("<result>");
        out.print("<query>");
        out.print("<docId>" + docId + "</docId>");
        out.print("<resultPage>" + page + "</resultPage>");
        out.print("<resultPageSize>" + pageSize + "</resultPageSize>");
        out.print("</query>");
        out.print("<hitsSize>" + hitsSize + "</hitsSize>");
        out.print("<hits>");
        for (int i=0; i<docsSize; i++) {
          Document doc = docs.get(i);
          out.print("<doc>");
          String similarDocId = doc.getField("docId").stringValue();
          out.print("<docId>" + similarDocId + "</docId>");
          IndexableField docProjectIdField = doc.getField("projectId");
          if (docProjectIdField != null) {
            String docProjectId = docProjectIdField.stringValue();
            out.print("<projectId>" + docProjectId + "</projectId>");
          }
          ArrayList<String> hitFragments = doc.getHitFragments();
          if (hitFragments != null) {
            out.print("<hitFragments>");
            for (int j=0; j<hitFragments.size(); j++) {
              String hitFragment = hitFragments.get(j);
              out.print("<hitFragment>" + hitFragment + "</hitFragment>");
            }
            out.print("</hitFragments>");
          }
          out.print("</doc>");
        }
        out.print("</hits>");
        out.print("<executionTime>" + elapsedTime + "</executionTime>");
        out.print("</result>");
      } else if (outputFormat.equals("html")) {
        StringBuilder htmlStrBuilder = new StringBuilder();
        htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        htmlStrBuilder.append("<html>");
        htmlStrBuilder.append("<head>");
        htmlStrBuilder.append("<title>Similar documents of: " + docId + "</title>");
        htmlStrBuilder.append("</head>");
        htmlStrBuilder.append("<body>");
        htmlStrBuilder.append("<h4>Similar documents of: " + docId + "</h4>");
        int fromDisplay = from + 1;
        int toDisplay = to + 1;
        if (hitsSize < to)
          toDisplay = hitsSize;
        htmlStrBuilder.append("Result: " + fromDisplay + " - " + toDisplay + " of " + hitsSize + " documents" + "</td>");
        htmlStrBuilder.append("<ul>");
        for (int i=0; i<docsSize; i++) {
          Document doc = docs.get(i);
          String similarDocId = doc.getField("docId").stringValue();
          int num = (page - 1) * pageSize + i + 1;
          htmlStrBuilder.append("<li>" + num + ". " + similarDocId + "</li>");
        }
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("<p/>");
        htmlStrBuilder.append("Elapsed time: " + elapsedTime + " ms");
        htmlStrBuilder.append("</body>");
        htmlStrBuilder.append("</html>");
        out.print(htmlStrBuilder.toString());
      } else if (outputFormat.equals("json")) {
        WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
        jsonEncoder.clear();
        jsonEncoder.putStrings("searchTerm", "");
        jsonEncoder.putStrings("numberOfHits", String.valueOf(hitsSize));
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<docsSize; i++) {
          JSONObject jsonWrapper = new JSONObject();
          org.bbaw.wsp.cms.document.Document doc = docs.get(i);
          IndexableField docProjectIdField = doc.getField("projectId");
          String projectId = null;
          if (docProjectIdField != null) {
            projectId = docProjectIdField.stringValue();
            jsonWrapper.put("projectId", projectId);
          }
          IndexableField docIdField = doc.getField("docId");
          if(docIdField != null){
            jsonWrapper.put("docId", docIdField.stringValue());
          }
          IndexableField docUriField = doc.getField("uri");
          if (docUriField != null) {
            String docUri = docUriField.stringValue();
            jsonWrapper.put("uri", docUri);
          }
          IndexableField webUriField = doc.getField("webUri");
          if (webUriField != null) {
            String webUri = webUriField.stringValue();
            jsonWrapper.put("webUri", webUri);
          }
          if (projectId != null) {
            Project project = ProjectReader.getInstance().getProject(projectId);
            String homepageUrl = project.getHomepageUrl();
            if (homepageUrl != null)
              jsonWrapper.put("webBaseUri", homepageUrl);
          }
          IndexableField docAuthorField = doc.getField("author");
          if (docAuthorField != null) {
            jsonWrapper.put("author", docAuthorField.stringValue());
          }
          IndexableField docTitleField = doc.getField("title");
          if (docTitleField != null) {
            jsonWrapper.put("title", docTitleField.stringValue());
          }
          IndexableField docDateField = doc.getField("date");
          if (docDateField != null) {
            jsonWrapper.put("date", docDateField.stringValue());
          }
          IndexableField docPageCountField = doc.getField("pageCount");
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
                IndexableField docPersNameField = nameDoc.getField("xmlContent");
                if (docPersNameField != null) {
                  String docPersName = docPersNameField.stringValue();
                  docPersName = docPersName.replaceAll("\\n", "");
                  String persNameAttribute = docPersName; 
                  if(persNameAttribute.contains("persName nymRef"))
                    persNameAttribute = docPersName.replaceAll("<persName nymRef=\"(.+?)\".+?</persName>", "$1");
                  if(persNameAttribute.contains("persName name="))
                    persNameAttribute = docPersName.replaceAll("<persName name=\"(.+?)\".+?</persName>", "$1");
                  if(persNameAttribute.contains("persName key="))
                    persNameAttribute = docPersName.replaceAll("<persName.*?>(.*?)</persName>", "$1");
                  persNameAttribute = persNameAttribute.replaceAll("<persName.*?>(.*?)</persName>", "$1");
                  persNameAttribute = persNameAttribute.replaceAll("&lt;|&gt;|<|>", "");
                  persNameAttribute = persNameAttribute.trim();
                  JSONObject nameAndLink = new JSONObject();
                  //TODO was tun mit dupilaten?
                  nameAndLink.put("name", persNameAttribute);
                  nameAndLink.put("link", "http://pdrdev.bbaw.de/concord/1-4/?n="+URIUtil.encodeQuery(persNameAttribute));
                  jsonNames.add(nameAndLink);
                }
              }
              jsonWrapper.put("persNames", jsonNames);
              JSONArray jsonPlaces = new JSONArray();
              Hits placeHits = indexHandler.queryDocument(docIdField.stringValue(), "elementName:placeName", 0, 100);
              ArrayList<Document> placeList = placeHits.getHits();
              for (Document placeDoc : placeList) {
                IndexableField docPlaceField = placeDoc.getField("xmlContent");
                if (docPlaceField != null) {
                  String docPlace = docPlaceField.stringValue();
                  docPlace = docPlace.replaceAll("\\n", "");
                  String placeAttribute = docPlace.replaceAll("<placeName name=\"(.+?)\".+?</placeName>", "$1");
                  placeAttribute = placeAttribute.replaceAll("<placeName.*?>(.*?)</placeName>", "$1");
                  placeAttribute = placeAttribute.trim();
                  //TODO was tun mit dupilaten?
                  JSONObject placeObj = new JSONObject(); 
                  placeObj.put("place", placeAttribute);
                  jsonPlaces.add(placeObj);
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
    } catch (ApplicationException e) {
      e.printStackTrace();
    }
	}
}
