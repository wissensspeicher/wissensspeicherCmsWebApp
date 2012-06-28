package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

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

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

/**
 * Servlet implementation class MoreLikeThis
 */
public class MoreLikeThis extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MoreLikeThis() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
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

        if (outputFormat.equals("html") || outputFormat.equals("json"))
          response.setContentType("text/html");
        
        if (outputFormat.equals("json")) {
          WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
          jsonEncoder.clear();
          jsonEncoder.putStrings("searchTerm", "");
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
                    String persNameAttribute = ""; 
                    if(persNameAttribute.contains("persName nymRef"))
                      persNameAttribute = docPersName.replaceAll("<persName nymRef=\"(.+)\"", "$1");
                    if(persNameAttribute.contains("persName name="))
                      persNameAttribute = docPersName.replaceAll("<persName name=\"(.+)\"", "$1");
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
        
      } catch (ApplicationException e) {
        e.printStackTrace();
      }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
