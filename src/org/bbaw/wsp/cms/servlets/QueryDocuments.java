package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Query;

import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.bbaw.wsp.cms.document.Document;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;

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
    String sortBy = request.getParameter("sortBy");
    String[] sortFields = null;
    if (sortBy != null && ! sortBy.trim().isEmpty())
      sortFields = sortBy.split(" ");
    String language = request.getParameter("language");
    if (language != null && language.equals("none"))
      language = null;
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
      Date begin = new Date();
      IndexHandler indexHandler = IndexHandler.getInstance();
      Boolean translateBool = false;
      if (translate != null && translate.equals("true"))
        translateBool = true;
      boolean withHitHighlights = false;
      if (query.contains("tokenOrig:") || query.contains("tokenMorph:") || query.contains("tokenReg:") || query.contains("tokenNorm:"))
        withHitHighlights = true;
      Hits hits = indexHandler.queryDocuments(query, sortFields, language, from, to, withHitHighlights, translateBool);
      int sizeTotalDocuments = hits.getSizeTotalDocuments();
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
      String baseUrl = getBaseUrl(request);
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
        out.print("<executionTime>" + elapsedTime + "</executionTime>");
        out.print("</result>");
      } else if (outputFormat.equals("html")) {
        StringBuilder htmlStrBuilder = new StringBuilder();
        String cssUrl = baseUrl + "/css/page.css";
        htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        htmlStrBuilder.append("<html>");
        htmlStrBuilder.append("<head>");
        htmlStrBuilder.append("<title>Query: " + query + "</title>");
        htmlStrBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/>");
        htmlStrBuilder.append("</head>");
        htmlStrBuilder.append("<body>");
        htmlStrBuilder.append("<table align=\"right\" valign=\"top\">");
        htmlStrBuilder.append("<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW CMS service\"/></a></td>");
        htmlStrBuilder.append("</table>");
        htmlStrBuilder.append("<p/>");
        String luceneQueryStr = query;
        Query luceneQuery = hits.getQuery();
        if (query != null)
          luceneQueryStr = luceneQuery.toString();
        String sortByStr = sortBy;
        if (sortBy == null)
          sortByStr = "";
        htmlStrBuilder.append("<h4>Query: " + luceneQueryStr + ", sorted by: " + sortByStr + "</h4>");
        htmlStrBuilder.append("<form action=\"QueryDocuments\" method=\"get\">");
        htmlStrBuilder.append("<input type=\"hidden\" name=\"query\" value=\"" + query + "\"/>");
        if (translate != null)
          htmlStrBuilder.append("<input type=\"hidden\" name=\"translate\" value=\"" + translate + "\"/>");
        if (language != null)
          htmlStrBuilder.append("<input type=\"hidden\" name=\"language\" value=\"" + language + "\"/>");
        htmlStrBuilder.append("<input type=\"hidden\" name=\"page\" id=\"pageId\" value=\"" + page + "\"/>");
        htmlStrBuilder.append("<input type=\"hidden\" name=\"sortBy\" id=\"sortById\" value=\"" + sortByStr + "\"/>");
        htmlStrBuilder.append("<input type=\"submit\" id=\"submitId\" style=\"position: absolute; left: -9999px\"/>");
        htmlStrBuilder.append("<table>");
        htmlStrBuilder.append("<colgroup>");
        htmlStrBuilder.append("<col width=\"3%\"/>");
        htmlStrBuilder.append("<col width=\"7%\"/>");
        htmlStrBuilder.append("<col width=\"3%\"/>");
        htmlStrBuilder.append("<col width=\"12%\"/>");
        htmlStrBuilder.append("<col width=\"70%\"/>");
        htmlStrBuilder.append("</colgroup>");
        htmlStrBuilder.append("<tr>");
        int countPages = hitsSize / 10 + 1;
        if (hitsSize % 10 == 0) // modulo operator: e.g. 280 % 10 is 0
          countPages = hitsSize / 10;
        int pageLeft = page - 1;
        if (page == 1)
          pageLeft = 1;
        int pageRight = page + 1; 
        if (page == countPages)
          pageRight = countPages;
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\"><button onclick=\"document.getElementById('pageId').value=" + pageLeft + "\" style=\"background:none;border:none;\"><img src=\"../images/left.gif\"/></button></td>");
        htmlStrBuilder.append("<td align=\"middle\" valign=\"top\" nowrap=\"true\">" + page + " / " + countPages + "</td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\"><button onclick=\"document.getElementById('pageId').value=" + pageRight + "\" style=\"background:none;border:none;\"><img src=\"../images/right.gif\"/></button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\" nowrap=\"true\">Page: <input type=\"text\" size=\"3\" value=\"" + page + "\" id=\"pageTextId\" onkeydown=\"if (event.keyCode == 13) {document.getElementById('pageId').value=document.getElementById('pageTextId').value; document.getElementById('submitId').click();}\"/></td>");
        int fromDisplay = from + 1;
        int toDisplay = to + 1;
        if (hitsSize < to)
          toDisplay = hitsSize;
        htmlStrBuilder.append("<td align=\"right\" valign=\"top\">" + fromDisplay + " - " + toDisplay + " of " + hitsSize + " hits (out of " + sizeTotalDocuments + " resources)" + "</td>");
        htmlStrBuilder.append("</tr>");
        htmlStrBuilder.append("</table>");
        htmlStrBuilder.append("<p/>");
        htmlStrBuilder.append("<table width=\"100%\" align=\"right\" border=\"2\" rules=\"groups\">");
        htmlStrBuilder.append("<colgroup>");
        htmlStrBuilder.append("<col width=\"3%\"/>");
        htmlStrBuilder.append("<col width=\"15%\"/>");
        htmlStrBuilder.append("<col width=\"45%\"/>");
        htmlStrBuilder.append("<col width=\"10%\"/>");
        htmlStrBuilder.append("<col width=\"5%\"/>");
        htmlStrBuilder.append("<col width=\"10%\"/>");
        htmlStrBuilder.append("<col width=\"6%\"/>");
        htmlStrBuilder.append("<col width=\"5%\"/>");
        htmlStrBuilder.append("<col width=\"4%\"/>");
        htmlStrBuilder.append("</colgroup>");
        htmlStrBuilder.append("<thead>");
        htmlStrBuilder.append("<tr>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\" style=\"font-weight:bold;\">" + "No" + "</td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='author'\" style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Author" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='title'\"  style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Title" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='publisher'\"  style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Publisher" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='date'\"  style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Year" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='docId'\" style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Id" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='lastModified'\" style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Last modified" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='language'\" style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Language" + "</button></td>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='schemaName'\" style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Schema" + "</button></td>");
        htmlStrBuilder.append("</tr>");
        htmlStrBuilder.append("</thead>");
        htmlStrBuilder.append("<tbody>");
        for (int i=0; i<docsSize; i++) {
          Document doc = docs.get(i);
          Fieldable docCollectionNamesField = doc.getFieldable("collectionNames");
          String docCollectionName = null;
          if (docCollectionNamesField != null) {
            docCollectionName = docCollectionNamesField.stringValue();
          }
          htmlStrBuilder.append("<tr valign=\"top\">");
          int num = (page - 1) * pageSize + i + 1;
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + num + ". " + "</td>");
          Fieldable authorField = doc.getFieldable("author");
          String author = "";
          if (authorField != null)
            author = authorField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + author + "</td>");
          Fieldable titleField = doc.getFieldable("title");
          String title = "";
          if (titleField != null)
            title = titleField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + title + "</td>");
          Fieldable publisherField = doc.getFieldable("publisher");
          String publisher = "";
          if (publisherField != null)
            publisher = publisherField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + publisher + "</td>");
          Fieldable yearField = doc.getFieldable("date");
          String year = "";
          if (yearField != null)
            year = yearField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + year + "</td>");
          String docId = doc.getFieldable("docId").stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + docId + "</td>");
          Fieldable lastModifiedField = doc.getFieldable("lastModified");
          String lastModified = "";
          if (lastModifiedField != null)
            lastModified = lastModifiedField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + lastModified + "</td>");
          Fieldable languageField = doc.getFieldable("language");
          String lang = "";
          if (languageField != null)
            lang = languageField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\" style=\"padding-left:5px\">" + lang + "</td>");
          Fieldable schemaNameField = doc.getFieldable("schemaName");
          String schemaName = "";
          if (schemaNameField != null)
            schemaName = schemaNameField.stringValue();
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + schemaName + "</td>");
          htmlStrBuilder.append("</tr>");
          // project link row
          String projectUrl = null;
          if (docCollectionName != null) {
            Collection projectColl = CollectionReader.getInstance().getCollection(docCollectionName);
            projectUrl = projectColl.getWebBaseUrl();
            String projectName = projectColl.getName();
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("Project: ");
            String projectStr = docCollectionName;
            if (projectName != null)
              projectStr = projectName + " (Id: " + docCollectionName + ")";
            if (projectUrl != null) {
              projectStr = "<a href=\"" + projectUrl + "\">" + projectStr + "</a>";
            }
            htmlStrBuilder.append(projectStr);
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          // description row
          Fieldable descriptionField = doc.getFieldable("description");
          if (descriptionField != null) {
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("Description: ");
            String description = descriptionField.stringValue();
            htmlStrBuilder.append(description);
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          // Knowledge rows
          Fieldable personsField = doc.getFieldable("persons");
          if (personsField != null) {
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");  
            htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/persons.png\" width=\"15\" height=\"15\" border=\"0\"/> ");
            String personsStr = personsField.stringValue();
            String[] persons = personsStr.split("###");  // separator of persons
            for (int j=0; j<persons.length; j++) {
              String personName = persons[j];
              String personLink = "/wspCmsWebApp/query/About?query=" + personName + "&type=person";
              if (lang != null && ! lang.isEmpty())
                personLink = personLink + "&language=" + lang;
              htmlStrBuilder.append("<a href=\"" + personLink + "\">" + personName +"</a>");
              if (j != persons.length - 1)
                htmlStrBuilder.append(", ");
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          Fieldable placesField = doc.getFieldable("places");
          if (placesField != null) {
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/place.png\" width=\"15\" height=\"15\" border=\"0\"/> ");
            String placesStr = placesField.stringValue();
            String[] places = placesStr.split("###");  // separator of places
            for (int j=0; j<places.length; j++) {
              String placeName = places[j];
              String placeLink = "/wspCmsWebApp/query/About?query=" + placeName + "&type=place";
              if (lang != null && ! lang.isEmpty())
                placeLink = placeLink + "&language=" + lang;
              htmlStrBuilder.append("<a href=\"" + placeLink + "\">" + placeName +"</a>");
              if (j != places.length - 1)
                htmlStrBuilder.append(", ");
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          Fieldable subjectField = doc.getFieldable("subject");
          if (subjectField != null) {
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("Subjects: ");
            String subjectStr = subjectField.stringValue();
            String[] subjects = subjectStr.split("[,]");  // separator of subjects
            for (int j=0; j<subjects.length; j++) {
              String subjectName = subjects[j].trim();
              if (! subjectName.isEmpty()) {
                String subjectLink = "/wspCmsWebApp/query/About?query=" + subjectName + "&type=subject";
                if (lang != null && ! lang.isEmpty())
                  subjectLink = subjectLink + "&language=" + lang;
                htmlStrBuilder.append("<a href=\"" + subjectLink + "\">" + subjectName +"</a>");
                if (j != subjects.length - 1)
                  htmlStrBuilder.append(", ");
              }
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          Fieldable swdField = doc.getFieldable("swd");
          if (swdField != null) {
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("SWD: ");
            String swdStr = swdField.stringValue();
            String[] swds = swdStr.split("[,]");  // separator of subjects
            for (int j=0; j<swds.length; j++) {
              String swdName = swds[j].trim();
              if (! swdName.isEmpty()) {
                String swdLink = "/wspCmsWebApp/query/About?query=" + swdName + "&type=swd";
                if (lang != null && ! lang.isEmpty())
                  swdLink = swdLink + "&language=" + lang;
                htmlStrBuilder.append("<a href=\"" + swdLink + "\">" + swdName +"</a>");
                if (j != swds.length - 1)
                  htmlStrBuilder.append(", ");
              }
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          Fieldable ddcField = doc.getFieldable("ddc");
          if (ddcField != null) {
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("DDC: ");
            String ddcStr = ddcField.stringValue();
            if (! ddcStr.isEmpty()) {
              String ddcLink = "/wspCmsWebApp/query/About?query=" + ddcStr + "&type=ddc";
              if (lang != null && ! lang.isEmpty())
                ddcLink = ddcLink + "&language=" + lang;
              htmlStrBuilder.append("<a href=\"" + ddcLink + "\">" + ddcStr +"</a>");
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          // Link row
          boolean docIsXml = false; 
          String mimeType = getMimeType(docId);
          if (mimeType != null && mimeType.contains("xml"))
            docIsXml = true;
          htmlStrBuilder.append("<tr valign=\"top\">");
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
          Fieldable webUriField = doc.getFieldable("webUri");
          String webUri = null;
          if (webUriField != null)
            webUri = webUriField.stringValue();
          if (webUri != null)
            htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + webUri + "\">Project-View</a>, ");
          String docIdPercentEscaped = docId.replaceAll("%", "%25"); // e.g. if docId contains "%20" then it is modified to "%2520"
          if (docIsXml)
            htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/book.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetPage?docId=" + docIdPercentEscaped + "\">WSP-View</a>, ");
          htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/download.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/doc/GetDocument?id=" + docIdPercentEscaped + "\">Download</a>, ");
          htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/search.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetDocInfo?docId=" + docIdPercentEscaped + "\">MetadataView</a>");
          htmlStrBuilder.append("</td>");
          htmlStrBuilder.append("</tr>");
          // hit fragments row
          ArrayList<String> hitFragments = doc.getHitFragments();
          if (hitFragments != null) {
            StringBuilder hitFragmentsStrBuilder = new StringBuilder();
            hitFragmentsStrBuilder.append("<b>Hit summary: </b>");
            hitFragmentsStrBuilder.append("(...) ");
            for (int j=0; j<hitFragments.size(); j++) {
              String hitFragment = hitFragments.get(j);
              hitFragmentsStrBuilder.append(hitFragment + " (...) ");
            }
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">" + hitFragmentsStrBuilder.toString() + "</td>");
            htmlStrBuilder.append("</tr>");
          }
        }
        htmlStrBuilder.append("</tbody>");
        htmlStrBuilder.append("</table>");
        htmlStrBuilder.append("</form>");
        htmlStrBuilder.append("<p/>");
        htmlStrBuilder.append("Elapsed time: " + elapsedTime + " ms");
        htmlStrBuilder.append("</body>");
        htmlStrBuilder.append("</html>");
        out.print(htmlStrBuilder.toString());
      } else if (outputFormat.equals("json")) {
        WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
        jsonEncoder.clear();
        jsonEncoder.putStrings("searchTerm", query);
        jsonEncoder.putStrings("numberOfHits", String.valueOf(hitsSize));
        jsonEncoder.putStrings("sizeTotalDocuments", String.valueOf(sizeTotalDocuments));
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<docsSize; i++) {
          JSONObject jsonWrapper = new JSONObject();
          org.bbaw.wsp.cms.document.Document doc = docs.get(i);
          Fieldable docCollectionNamesField = doc.getFieldable("collectionNames");
          String docCollectionName = null;
          if (docCollectionNamesField != null) {
            docCollectionName = docCollectionNamesField.stringValue();
            jsonWrapper.put("collectionName", docCollectionName);
          }
          if (docCollectionName != null) {
            JSONArray jsonProject = new JSONArray();
            Collection coll = CollectionReader.getInstance().getCollection(docCollectionName);
            JSONObject jsonProjectId = new JSONObject();
            jsonProjectId.put("id", docCollectionName);
            jsonProject.add(jsonProjectId);
            String projectName = coll.getName();
            if (projectName != null) {
              JSONObject jsonProjectName = new JSONObject();
              jsonProjectName.put("name", projectName);
              jsonProject.add(jsonProjectName);
            }
            String projectUrl = coll.getWebBaseUrl();
            if (projectUrl != null) {
              JSONObject jsonProjectUrl = new JSONObject();
              jsonProjectUrl.put("url", projectUrl);
              jsonProject.add(jsonProjectUrl);
            }
            jsonWrapper.put("project", jsonProject);
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
          Fieldable webUriField = doc.getFieldable("webUri");
          if (webUriField != null) {
            String webUri = webUriField.stringValue();
            jsonWrapper.put("webUri", webUri);
          }
          if (docCollectionName != null) {
            Collection coll = CollectionReader.getInstance().getCollection(docCollectionName);
            String webBaseUrl = coll.getWebBaseUrl();
            if (webBaseUrl != null)
              jsonWrapper.put("webBaseUri", webBaseUrl);
          }
          Fieldable docAuthorField = doc.getFieldable("author");
          if (docAuthorField != null) {
            String docAuthor = docAuthorField.stringValue();
            docAuthor = StringUtils.resolveXmlEntities(docAuthor);
            jsonWrapper.put("author", docAuthor);
          }
          Fieldable docTitleField = doc.getFieldable("title");
          if (docTitleField != null) {
            String docTitle = docTitleField.stringValue();
            docTitle = StringUtils.resolveXmlEntities(docTitle);
            jsonWrapper.put("title", docTitle);
          }
          Fieldable languageField = doc.getFieldable("language");
          String lang = "";
          if (languageField != null) {
            lang = languageField.stringValue();
            jsonWrapper.put("language", lang);
          }
          Fieldable descriptionField = doc.getFieldable("description");
          if (descriptionField != null) {
            String description = descriptionField.stringValue();
            description = StringUtils.resolveXmlEntities(description);
            jsonWrapper.put("description", description);
          }
          Fieldable docDateField = doc.getFieldable("date");
          if (docDateField != null) {
            jsonWrapper.put("date", docDateField.stringValue());
          }
          Fieldable typeField = doc.getFieldable("type");
          if (typeField != null) {
            String type = typeField.stringValue();
            jsonWrapper.put("type", type);
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
          
          JSONArray jsonPersons = new JSONArray();
          Fieldable personsField = doc.getFieldable("persons");
          if (personsField != null) {
            String personsStr = personsField.stringValue();
            String[] persons = personsStr.split("###");  // separator of persons
            for (int j=0; j<persons.length; j++) {
              String personName = persons[j];
              JSONObject persNameAndLink = new JSONObject();
              persNameAndLink.put("name", personName);
              String personLink = baseUrl + "/query/About?query=" + personName + "&type=person";
              if (lang != null && ! lang.isEmpty())
                personLink = personLink + "&language=" + lang;
              persNameAndLink.put("link", personLink);
              jsonPersons.add(persNameAndLink);
            }
          }
          jsonWrapper.put("persNames", jsonPersons);
          JSONArray jsonPlaces = new JSONArray();
          Fieldable placesField = doc.getFieldable("places");
          if (placesField != null) {
            String placesStr = placesField.stringValue();
            String[] places = placesStr.split("###");  // separator of places
            for (int j=0; j<places.length; j++) {
              String placeName = places[j];
              JSONObject placeNameAndLink = new JSONObject();
              String placeLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(placeName) + "&type=place";
              if (lang != null && ! lang.isEmpty())
                placeLink = placeLink + "&language=" + lang;
              placeNameAndLink.put("name", placeName);
              placeNameAndLink.put("link", placeLink);  
              jsonPlaces.add(placeNameAndLink);
            }
          }
          jsonWrapper.put("placeNames", jsonPlaces);
          JSONArray jsonSubjects = new JSONArray();
          Fieldable subjectField = doc.getFieldable("subject");
          if (subjectField != null) {
            String subjectStr = subjectField.stringValue();
            String[] subjects = subjectStr.split("[,]");  // separator of subjects
            for (int j=0; j<subjects.length; j++) {
              String subjectName = subjects[j].trim();
              if (! subjectName.isEmpty()) {
                JSONObject subjectNameAndLink = new JSONObject();
                subjectNameAndLink.put("name", subjectName);
                String subjectLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(subjectName) + "&type=subject";
                if (lang != null && ! lang.isEmpty())
                  subjectLink = subjectLink + "&language=" + lang;
                subjectNameAndLink.put("link", subjectLink);
                jsonSubjects.add(subjectNameAndLink);
              }
            }
          }
          jsonWrapper.put("subjects", jsonSubjects);
          JSONArray jsonSwd = new JSONArray();
          Fieldable swdField = doc.getFieldable("swd");
          if (swdField != null) {
            String swdStr = swdField.stringValue();
            String[] swdEntries = swdStr.split("[,]");  // separator of swd entries
            for (int j=0; j<swdEntries.length; j++) {
              String swdName = swdEntries[j].trim();
              if (! swdName.isEmpty()) {
                JSONObject swdNameAndLink = new JSONObject();
                swdNameAndLink.put("name", swdName);
                String swdLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(swdName) + "&type=swd";
                if (lang != null && ! lang.isEmpty())
                  swdLink = swdLink + "&language=" + lang;
                swdNameAndLink.put("link", swdLink);  
                jsonSwd.add(swdNameAndLink);
              }
            }
          }
          jsonWrapper.put("swd", jsonSwd);
          JSONArray jsonDdc = new JSONArray();
          Fieldable ddcField = doc.getFieldable("ddc");
          if (ddcField != null) {
            String ddcStr = ddcField.stringValue();
            if (! ddcStr.isEmpty()) {
              JSONObject ddcNameAndLink = new JSONObject();
              ddcNameAndLink.put("name", ddcStr);
              String ddcLink = baseUrl + "/query/About?query=" + URIUtil.encodeQuery(ddcStr) + "&type=ddc";
              if (lang != null && ! lang.isEmpty())
                ddcLink = ddcLink + "&language=" + lang;
              ddcNameAndLink.put("link", ddcLink);
              jsonDdc.add(ddcNameAndLink);
            }
          }
          jsonWrapper.put("ddc", jsonDdc);

          jsonArray.add(jsonWrapper);
        }
        jsonEncoder.putJsonObj("hits", jsonArray);
        out.println(JSONValue.toJSONString(jsonEncoder.getJsonObject()));
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private String getMimeType(String docId) {
    String mimeType = null;
    FileNameMap fileNameMap = URLConnection.getFileNameMap();  // map with 53 entries such as "application/xml"
    mimeType = fileNameMap.getContentTypeFor(docId);
    return mimeType;
  }

  private String getBaseUrl(HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName();
  }
}
