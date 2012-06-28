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
    String additionalInfo = request.getParameter("addInf");
    String translate = request.getParameter("translate");
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
      outputFormat = "html";
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      Boolean translateBool = false;
      if (translate != null && translate.equals("true"))
        translateBool = true;
      Hits hits = indexHandler.queryDocuments(query, language, from, to, true, translateBool);
      ArrayList<Document> docs = null;
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
          Document doc = docs.get(i);
          out.print("<doc>");
          String docId = doc.getFieldable("docId").stringValue();
          out.print("<docId>" + docId + "</docId>");
          Fieldable docCollectionNamesField = doc.getFieldable("collectionNames");
          if (docCollectionNamesField != null) {
            String docCollectionNames = docCollectionNamesField.stringValue();
            out.print("<collectionName>" + docCollectionNames + "</collectionName>");
          }
          out.print("</doc>");
        }
        out.print("</hits>");
        out.print("</result>");
      } else if (outputFormat.equals("html")) {
        StringBuilder htmlStrBuilder = new StringBuilder();
        String baseUrl = getBaseUrl(request);
        String cssUrl = baseUrl + "/css/page.css";
        htmlStrBuilder.append("<html>");
        htmlStrBuilder.append("<head>");
        htmlStrBuilder.append("<title>Query: \"" + query + "\"</title>");
        htmlStrBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/>");
        htmlStrBuilder.append("</head>");
        htmlStrBuilder.append("<body>");
        htmlStrBuilder.append("<table align=\"right\" valign=\"top\">");
        htmlStrBuilder.append("<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW WSP CMS service\"/></a></td>");
        htmlStrBuilder.append("</table>");
        htmlStrBuilder.append("<p/>");
        htmlStrBuilder.append("<h1>Query: " + "\"" + query + "</h1>");
        htmlStrBuilder.append((from+1) + " - " + (to+1) + " of " + hitsSize + " documents");
        htmlStrBuilder.append("<p/>");
        htmlStrBuilder.append("<table align=\"right\" width=\"100%\" border=\"2\" rules=\"groups\">");
        htmlStrBuilder.append("<colgroup>");
        htmlStrBuilder.append("<col width=\"4%\"/>");
        htmlStrBuilder.append("<col width=\"20%\"/>");
        htmlStrBuilder.append("<col width=\"75%\"/>");
        htmlStrBuilder.append("</colgroup>");
        htmlStrBuilder.append("<thead");
        htmlStrBuilder.append("<tr>");
        htmlStrBuilder.append("<th align=\"left\" valign=\"top\">" + "No" + "</th>");
        htmlStrBuilder.append("<th align=\"left\" valign=\"top\">" + "Document" + "</th>");
        htmlStrBuilder.append("<th align=\"left\" valign=\"top\">" + "Hits" + "</th>");
        htmlStrBuilder.append("</tr>");
        htmlStrBuilder.append("</thead");
        for (int i=0; i<docsSize; i++) {
          htmlStrBuilder.append("<tr valign=\"top\">");
          Document doc = docs.get(i);
          int num = (page - 1) * pageSize + i + 1;
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + num + ". " + "</td>");
          String docId = doc.getFieldable("docId").stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + docId + "</td>");
          ArrayList<String> hitFragments = doc.getHitFragments();
          if (hitFragments != null) {
            StringBuilder hitFragmentsStrBuilder = new StringBuilder();
            hitFragmentsStrBuilder.append("(...) ");
            for (int j=0; j<hitFragments.size(); j++) {
              String hitFragment = hitFragments.get(j);
              hitFragmentsStrBuilder.append(hitFragment + " (...) ");
            }
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + hitFragmentsStrBuilder.toString() + "</td>");
          }
          htmlStrBuilder.append("</tr>");
        }
        htmlStrBuilder.append("</table");
        htmlStrBuilder.append("</body>");
        htmlStrBuilder.append("</html>");
        out.print(htmlStrBuilder.toString());
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
                  String persNameAttribute = docPersName; 
                  if(persNameAttribute.contains("persName nymRef"))
                    persNameAttribute = docPersName.replaceAll("<persName nymRef=\"(.+)\".+?</persName>", "$1");
                  if(persNameAttribute.contains("persName name="))
                    persNameAttribute = docPersName.replaceAll("<persName name=\"(.+)\".+?</persName>", "$1");
                  if(persNameAttribute.contains("persName key="))
                    persNameAttribute = docPersName.replaceAll("<persName.*?>(.*)</persName>", "$1");
                  if(persNameAttribute.contains("</persName>"))
                    persNameAttribute = persNameAttribute.replace("</persName>", "");
                  if(persNameAttribute.contains("<persName>"))
                      persNameAttribute = persNameAttribute.replace("<persName>", "");
                  persNameAttribute = persNameAttribute.trim();
                  //TODO evtl auch den inhalt der tags mit auswerten, nicht nur das attribut
                  String persNameContent = docPersName.replaceAll("<persName.*?>(.*)</persName>", "$1");
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
                Fieldable docPlaceField = placeDoc.getFieldable("xmlContent");
                if (docPlaceField != null) {
                  String docPlace = docPlaceField.stringValue();
                  String placeAttribute = docPlace.replaceAll("<placeName name=\"(.+)\"", "$1");
                  if(placeAttribute.contains("</placeName>"))
                    placeAttribute = placeAttribute.replace("</placeName>", "");
                  if(placeAttribute.contains("<placeName>"))
                    placeAttribute = placeAttribute.replace("<placeName>", "");
                  placeAttribute = placeAttribute.trim();
                  //TODO evtl auch den inhalt der tags mit auswerten, nicht nur das attribut
                  String placeContent = docPlace.replaceAll("<placeName.*?>(.*)</placeName>", "$1"); 
                  if(placeAttribute.contains("</placeName>"))
                    placeAttribute = placeAttribute.replace("</placeName>", "");
                  if(placeAttribute.contains("<placeName>"))
                      placeAttribute = placeAttribute.replace("<placeName>", "");
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
      out.close();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

  private String getBaseUrl(HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(HttpServletRequest request) {
    if ( ( request.getServerPort() == 80 ) || ( request.getServerPort() == 443 ) )
      return request.getScheme() + "://" + request.getServerName();
    else
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }
}
