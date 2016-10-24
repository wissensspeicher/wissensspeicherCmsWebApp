package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.bbaw.wsp.cms.collections.OutputType;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectCollection;
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.bbaw.wsp.cms.collections.Subject;
import org.bbaw.wsp.cms.document.DBpediaResource;
import org.bbaw.wsp.cms.document.Document;
import org.bbaw.wsp.cms.document.Facets;
import org.bbaw.wsp.cms.document.GroupDocuments;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class QueryDocuments extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private XQueryEvaluator xQueryEvaluator = null;
  
  public QueryDocuments() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    ServletContext context = getServletContext();
    xQueryEvaluator = (XQueryEvaluator) context.getAttribute("xQueryEvaluator");
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String queryLanguage = request.getParameter("queryLanguage");
    if (queryLanguage == null)
      queryLanguage = "gl";  // google like
    String query = request.getParameter("query");
    String sortBy = request.getParameter("sortBy");
    String[] sortFields = null;
    if (sortBy != null && ! sortBy.trim().isEmpty())
      sortFields = sortBy.split(" ");
    String groupBy = request.getParameter("groupBy");
    String fieldExpansion = request.getParameter("fieldExpansion");
    if (fieldExpansion == null)
      fieldExpansion = "all";
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
    String requestHitFragments = request.getParameter("hitFragments");  // if "true": show result with highlighted hit fragments
    String outputOptions = request.getParameter("outputOptions");
    if (outputOptions == null)
      outputOptions = "showAll";
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "html";
    if (outputFormat.equals("xml"))
      response.setContentType("text/xml");
    else if (outputFormat.equals("html") || outputFormat.equals("htmlSmart"))
      response.setContentType("text/html");
    else if (outputFormat.equals("json"))
      response.setContentType("application/json");
    else 
      response.setContentType("text/xml");
    PrintWriter out = response.getWriter();
    if (query == null || query.isEmpty()) {
      if (outputFormat.equals("xml"))
        out.print("<error>no query specified: please set parameter &amp;query&amp;</error>");
      else if (outputFormat.equals("html"))
        out.print("no query specified: please set parameter \"query\"");
      else if (outputFormat.equals("json"))
        out.print("");
      return;
    }
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      Boolean translateBool = false;
      if (translate != null && translate.equals("true"))
        translateBool = true;
      boolean withHitHighlights = false;
      if (requestHitFragments == null || requestHitFragments.equals("true"))
        withHitHighlights = true;
      query = query.replaceAll("%22", "\""); // if double quote is percent encoded
      Hits hits = indexHandler.queryDocuments(queryLanguage, query, sortFields, groupBy, fieldExpansion, language, from, to, withHitHighlights, translateBool);
      int sizeTotalDocuments = hits.getSizeTotalDocuments();
      int sizeTotalTerms = hits.getSizeTotalTerms();
      ArrayList<Document> docs = null;
      if (hits != null)
        docs = hits.getHits();
      int hitsSize = -1;
      int docsSize = -1;
      if (hits != null)
        hitsSize = hits.getSize();
      long elapsedTime = hits.getElapsedTime();
      if (docs != null)
        docsSize = docs.size();
      String baseUrl = getBaseUrl(request);
      Comparator<String> ignoreCaseComparator = new Comparator<String>() {
        public int compare(String s1, String s2) {
          return s1.compareToIgnoreCase(s2);
        }
      };
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
          String docId = doc.getField("docId").stringValue();
          out.print("<docId>" + docId + "</docId>");
          IndexableField docProjectIdField = doc.getField("projectId");
          if (docProjectIdField != null) {
            String projectId = docProjectIdField.stringValue();
            out.print("<projectId>" + projectId + "</projectId>");
          }
          out.print("</doc>");
        }
        out.print("</hits>");
        out.print("<executionTime>" + elapsedTime + "</executionTime>");
        out.print("</result>");
      } else if (outputFormat.equals("htmlSmart")) {
        StringBuilder htmlStrBuilder = new StringBuilder();
        htmlStrBuilder.append("<!DOCTYPE html>");
        htmlStrBuilder.append("<div id=\"result\">");
        htmlStrBuilder.append("<div id=\"my-tab-content\" class=\"tab-content\">");
        htmlStrBuilder.append("<div id=\"hits\" class=\"tab-pane active\" style=\"margin-top:3px;\">");
        // pager row
        htmlStrBuilder.append("<div class=\"row vertical-align\">");
        htmlStrBuilder.append("<div class=\"col-sm-9\">");
        int countPages = hitsSize / pageSize + 1;
        if (hitsSize % pageSize == 0) // modulo operator: e.g. 280 % 10 is 0
          countPages = hitsSize / pageSize;
        htmlStrBuilder.append("<span style=\"padding-right:4px\"><button id=\"pageLeft\" type=\"button\" onclick=\"pageLeft();\" style=\"background:none;border:none;\"><img src=\"../images/left.gif\" width=\"15\" height=\"15\"/></button></span>");
        htmlStrBuilder.append("<span style=\"padding-right:4px\">" + page + " / " + "<span id=\"countPages\">" + countPages + "</span>" + "</span>");
        htmlStrBuilder.append("<span style=\"padding-right:4px\"><button id=\"pageRight\" type=\"button\" onclick=\"pageRight();\" style=\"background:none;border:none;\"><img src=\"../images/right.gif\" width=\"15\" height=\"15\"/></button></span>");
        htmlStrBuilder.append("<span style=\"padding-right:4px\"> Page: </span>");
        htmlStrBuilder.append("<span><input type=\"text\" size=\"3\" value=\"" + page + "\" id=\"page\" onkeydown=\"page(event);\"/></span>");
        int fromDisplay = from + 1;
        int toDisplay = to + 1;
        if (hitsSize < toDisplay)
          toDisplay = hitsSize;
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("<div class=\"col-sm-3\">");
        htmlStrBuilder.append("<span>" + fromDisplay + " - " + toDisplay + " of " + hitsSize + " hits (out of " + sizeTotalDocuments + " resources)" + "</span>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("</div>");
        // hits table
        htmlStrBuilder.append("<table class=\"table\">");
        htmlStrBuilder.append("<thead>");
        htmlStrBuilder.append("<tr>");
        htmlStrBuilder.append("<td style=\"font-weight:bold;\">" + "No" + "</td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('author')\">" + "Author" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('title')\">" + "Title" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('publisher')\">" + "Publisher" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('date')\">" + "Year" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('docId')\">" + "Id" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('lastModified')\">" + "Last modified" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('language')\">" + "Language" + "</button></td>");
        htmlStrBuilder.append("<td>" + "<button style=\"padding:0px;font-weight:bold;background:none;border:none;\" onclick=\"sortBy('type')\">" + "Type" + "</button></td>");
        htmlStrBuilder.append("</tr>");
        htmlStrBuilder.append("</thead>");
        htmlStrBuilder.append("<tbody>");
        for (int i=0; i<docsSize; i++) {
          Document doc = docs.get(i);
          Float luceneScore = doc.getScore();
          IndexableField docProjectIdField = doc.getField("projectId");
          String projectId = null;
          if (docProjectIdField != null) {
            projectId = docProjectIdField.stringValue();
          }
          IndexableField languageField = doc.getField("language");
          String lang = "";
          if (languageField != null)
            lang = languageField.stringValue();
          htmlStrBuilder.append("<tr>");
          int num = (page - 1) * pageSize + i + 1;
          htmlStrBuilder.append("<td>" + num + ". " + "</td>");
          IndexableField docAuthorField = doc.getField("author");
          String authorHtml = "";
          if (docAuthorField != null) {
            IndexableField docAuthorDetailsField = doc.getField("authorDetails");
            if (docAuthorDetailsField != null) {
              String docAuthorDetailsXmlStr = docAuthorDetailsField.stringValue();
              authorHtml = docPersonsDetailsXmlStrToHtml(xQueryEvaluator, docAuthorDetailsXmlStr, baseUrl, lang);            
            } else {
              String authorName = docAuthorField.stringValue();
              Person author = new Person();
              author.setName(authorName);
              String aboutPersonLink = baseUrl + "/query/About?query=" + authorName + "&type=person";
              if (lang != null && ! lang.isEmpty())
                aboutPersonLink = aboutPersonLink + "&language=" + lang;
              author.setAboutLink(aboutPersonLink);
              String htmlStrPerson = author.toHtmlStr();
              authorHtml = "<span class=\"persons\">";
              authorHtml = authorHtml + htmlStrPerson;
              authorHtml = authorHtml + "</span>";
            }
          }          
          htmlStrBuilder.append("<td>" + authorHtml + "</td>");
          IndexableField titleField = doc.getField("title");
          String title = "";
          if (titleField != null)
            title = titleField.stringValue();
          htmlStrBuilder.append("<td>" + title + "</td>");
          IndexableField publisherField = doc.getField("publisher");
          String publisher = "";
          if (publisherField != null)
            publisher = publisherField.stringValue();
          htmlStrBuilder.append("<td>" + publisher + "</td>");
          IndexableField yearField = doc.getField("date");
          String year = "";
          if (yearField != null)
            year = yearField.stringValue();
          htmlStrBuilder.append("<td>" + year + "</td>");
          String docId = doc.getField("docId").stringValue();
          htmlStrBuilder.append("<td>" + docId + "</td>");
          IndexableField lastModifiedField = doc.getField("lastModified");
          String lastModified = "";
          if (lastModifiedField != null)
            lastModified = lastModifiedField.stringValue();
          htmlStrBuilder.append("<td>" + lastModified + "</td>");
          htmlStrBuilder.append("<td>" + lang + "</td>");
          IndexableField typeField = doc.getField("type");
          String type = "";
          if (typeField != null)
            type = typeField.stringValue();
          htmlStrBuilder.append("<td>" + type + "</td>");
          htmlStrBuilder.append("</tr>");
          // hit fragments row
          htmlStrBuilder.append("<tr>");
          htmlStrBuilder.append("<td colspan=\"9\">");
          htmlStrBuilder.append("<table class=\"table table-borderless\">");
          ArrayList<String> hitFragments = doc.getHitFragments();
          if (hitFragments != null) {
            StringBuilder hitFragmentsStrBuilder = new StringBuilder();
            hitFragmentsStrBuilder.append("<b>Hit summary: </b>");
            hitFragmentsStrBuilder.append("(...) ");
            for (int j=0; j<hitFragments.size(); j++) {
              String hitFragment = hitFragments.get(j);
              hitFragmentsStrBuilder.append(hitFragment + " (...) ");
            }
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">" + hitFragmentsStrBuilder.toString() + "</td>");
            htmlStrBuilder.append("</tr>");
          }
          // project links row
          String projectUrl = null;
          boolean docIsXml = false; 
          String firstHitPageNumber = null;
          String mimeType = getMimeType(docId);
          if (mimeType != null && mimeType.contains("xml"))
            docIsXml = true;
          if (docIsXml)
            firstHitPageNumber = doc.getFirstHitPageNumber();
          if (projectId != null) {
            Project project = ProjectReader.getInstance().getProject(projectId);
            if (project != null) {
              projectUrl = project.getHomepageUrl();
              String projectRdfId = project.getRdfId();
              String projectTitle = project.getTitle();
              htmlStrBuilder.append("<tr>");
              htmlStrBuilder.append("<td></td>");
              htmlStrBuilder.append("<td colspan=\"8\">");
              htmlStrBuilder.append("<b>Project links</b>: ");
              IndexableField webUriField = doc.getField("webUri");
              String webUri = null;
              if (webUriField != null)
                webUri = webUriField.stringValue();
              if (webUri != null) {
                if (! webUri.contains("%"))
                  webUri = URIUtil.encodeQuery(webUri);  
                webUri = webUri.replaceAll("%23", "#"); // for e.g.: http://telota.bbaw.de/mega/%23?doc=MEGA_A2_B005-00_ETX.xml
                htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + webUri + "\">Project view</a> ");
              }
              if (projectUrl != null) {
                htmlStrBuilder.append("(");
                htmlStrBuilder.append("Project: <img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + projectUrl + "\">" + projectTitle + "</a>");
              }
              if (projectRdfId != null) {
                String projectDetailsUrl = "/wspCmsWebApp/query/QueryMdSystem?query=" + projectRdfId + "&detailedSearch=true";
                htmlStrBuilder.append(" (<img src=\"/wspCmsWebApp/images/search.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + projectDetailsUrl + "\">Project details</a>)");
              }
              IndexableField collectionRdfIdField = doc.getField("collectionRdfId");
              if (collectionRdfIdField != null) {
                String collectionRdfId = collectionRdfIdField.stringValue();
                if (collectionRdfId != null && ! collectionRdfId.isEmpty()) {
                  ProjectCollection coll = project.getCollection(collectionRdfId);
                  String collectionTitle = coll.getTitle();
                  if (collectionTitle == null)
                    collectionTitle = "Collection homepage";
                  String collectionHomepageUrl = coll.getHomepageUrl();
                  String collectionTypeStr = "";
                  OutputType collType = coll.getType();
                  if (collType != null)
                    collectionTypeStr = "(Type: " + collType.getLabel() + ")";
                  if (collectionHomepageUrl != null)
                    htmlStrBuilder.append(", Collection" + collectionTypeStr + ": <img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + collectionHomepageUrl + "\">" + collectionTitle + "</a>");
                }
              }
              IndexableField databaseRdfIdField = doc.getField("databaseRdfId");
              if (databaseRdfIdField != null) {
                String databaseRdfId = databaseRdfIdField.stringValue();
                if (databaseRdfId != null && ! databaseRdfId.isEmpty()) {
                    htmlStrBuilder.append(", Database: <img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + databaseRdfId + "\">" + databaseRdfId + "</a>");
                }
              }
              if (projectUrl != null)
                htmlStrBuilder.append(")");
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
          }
          // description row
          IndexableField descriptionField = doc.getField("description");
          if (descriptionField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");
            htmlStrBuilder.append("<b>Description</b>: ");
            String description = descriptionField.stringValue();
            if (description != null && description.length() > 400)
              description = description.substring(0, 400) + " (...)";
            htmlStrBuilder.append(description);
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          IndexableField personsField = doc.getField("persons");
          IndexableField personsDetailsField = doc.getField("personsDetails");
          if (personsField != null || personsDetailsField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");  
            htmlStrBuilder.append("<b>Persons</b>: ");
            if (personsDetailsField != null) {
              String personsDetailsXmlStr = personsDetailsField.stringValue();
              String personsDetailsHtmlStr = docPersonsDetailsXmlStrToHtml(xQueryEvaluator, personsDetailsXmlStr, baseUrl, language);
              htmlStrBuilder.append(personsDetailsHtmlStr);
            } else if (personsField != null) {
              String personsStr = personsField.stringValue();
              String[] persons = personsStr.split("###");  // separator of persons
              for (int j=0; j<persons.length; j++) {
                String personName = persons[j];
                Person person = new Person();
                person.setRole(Person.MENTIONED);
                person.setName(personName);
                String aboutPersonLink = baseUrl + "/query/About?query=" + personName + "&type=person";
                if (lang != null && ! lang.isEmpty())
                  aboutPersonLink = aboutPersonLink + "&language=" + lang;
                person.setAboutLink(aboutPersonLink);
                String htmlStrPerson = person.toHtmlStr();
                htmlStrBuilder.append(htmlStrPerson);
                if (j != persons.length - 1)
                  htmlStrBuilder.append(", ");
              }
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          IndexableField placesField = doc.getField("places");
          if (placesField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");
            htmlStrBuilder.append("<b>Places</b>: ");
            String placesStr = placesField.stringValue();
            String[] places = placesStr.split("###");  // separator of places
            places = cleanNames(places);
            Arrays.sort(places, ignoreCaseComparator);
            for (int j=0; j<places.length; j++) {
              String placeName = places[j];
              if (! placeName.isEmpty()) {
                String placeLink = "/wspCmsWebApp/query/About?query=" + placeName + "&type=place";
                if (lang != null && ! lang.isEmpty())
                  placeLink = placeLink + "&language=" + lang;
                htmlStrBuilder.append("<a href=\"" + placeLink + "\">" + placeName +"</a>");
                if (j != places.length - 1)
                  htmlStrBuilder.append(", ");
              }
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          IndexableField subjectControlledDetailsField = doc.getField("subjectControlledDetails");
          if (subjectControlledDetailsField != null) {
            String subjectControlledDetailsStr = subjectControlledDetailsField.stringValue();
            org.jsoup.nodes.Document subjectControlledDetailsDoc = Jsoup.parse(subjectControlledDetailsStr);
            Elements dctermsSubjects = subjectControlledDetailsDoc.select("subjects > dcterms|subject");
            if (dctermsSubjects != null && dctermsSubjects.size() > 0) {
              htmlStrBuilder.append("<tr>");
              htmlStrBuilder.append("<td></td>");
              htmlStrBuilder.append("<td colspan=\"8\">");
              htmlStrBuilder.append("<b>Subjects (controlled)</b>: ");
              for (int j=0; j<dctermsSubjects.size(); j++) {
                Element dctermsSubject = dctermsSubjects.get(j);
                // e.g.: <dcterms:subject rdf:resource="http://d-nb.info/gnd/4037764-7"/>
                String rdfIdSubject = dctermsSubject.attr("rdf:resource");
                Subject subject = ProjectReader.getInstance().getSubject(rdfIdSubject);
                if (subject != null) {
                  String subjectRdfType = subject.getType();
                  String subjectRdfLink = subject.getRdfId();
                  String subjectName = subject.getName();
                  String subjectSearchUrl = "/wspCmsWebApp/query/query.html?queryLanguage=lucene&query=subjectControlled:&quot;" + subjectName + "&quot;&fieldExpansion=none";
                  String ontologyName = getOntologyName(subjectRdfType);
                  String ontologyNameStr = ontologyName + ": ";
                  if (ontologyName == null)
                    ontologyNameStr = "";
                  String subjectRdfImgLink = "<a href=\"" + subjectRdfLink + "\">" + "<img src=\"/wspCmsWebApp/images/" + "rdfSmall.gif" + "\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                  htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + subjectName + "</a> (" + ontologyNameStr + subjectRdfImgLink + ")");
                }
                if (j < dctermsSubjects.size() - 1)
                  htmlStrBuilder.append(", ");
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
          }
          IndexableField subjectField = doc.getField("subject");
          if (subjectField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");
            htmlStrBuilder.append("<b>Subjects (free)</b>: ");
            String subjectStr = subjectField.stringValue();
            String[] subjects = subjectStr.split("[,]");  // one separator of subjects
            if (subjectStr.contains("###"))
              subjects = subjectStr.split("###");  // another separator of subjects
            subjects = cleanNames(subjects);
            Arrays.sort(subjects, ignoreCaseComparator);
            for (int j=0; j<subjects.length; j++) {
              String subjectName = subjects[j];
              if (! subjectName.isEmpty()) {
                String subjectLink = "/wspCmsWebApp/query/About?query=" + subjectName + "&type=subject";
                if (lang != null && ! lang.isEmpty())
                  subjectLink = subjectLink + "&language=" + lang;
                String subjectSearchUrl = "/wspCmsWebApp/query/query.html?queryLanguage=lucene&query=subject:&quot;" + subjectName + "&quot;&fieldExpansion=none";
                String subjectImgLink = "<a href=\"" + subjectLink + "\">" + "<img src=\"/wspCmsWebApp/images/rdfSmall.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + subjectName + "</a> (" + subjectImgLink + ")");
                if (j != subjects.length - 1)
                  htmlStrBuilder.append(", ");
              }
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          IndexableField swdField = doc.getField("swd");
          if (swdField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");
            htmlStrBuilder.append("<b>SWD</b>: ");
            String swdStr = swdField.stringValue();
            String[] swds = swdStr.split("[,]");  // separator of subjects
            swds = cleanNames(swds);
            Arrays.sort(swds, ignoreCaseComparator);
            for (int j=0; j<swds.length; j++) {
              String swdName = swds[j];
              if (! swdName.isEmpty()) {
                String swdLink = "/wspCmsWebApp/query/About?query=" + swdName + "&type=swd";
                if (lang != null && ! lang.isEmpty())
                  swdLink = swdLink + "&language=" + lang;
                String subjectSearchUrl = "/wspCmsWebApp/query/query.html?queryLanguage=lucene&query=swd:&quot;" + swdName + "&quot;&fieldExpansion=none";
                String subjectImgLink = "<a href=\"" + swdLink + "\">" + "<img src=\"/wspCmsWebApp/images/rdfSmall.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + swdName + "</a> (" + subjectImgLink + ")");
                if (j != swds.length - 1)
                  htmlStrBuilder.append(", ");
              }
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          IndexableField ddcField = doc.getField("ddc");
          if (ddcField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");
            htmlStrBuilder.append("<b>DDC</b>: ");
            String ddcStr = ddcField.stringValue();
            if (! ddcStr.isEmpty()) {
              String ddcLink = "/wspCmsWebApp/query/About?query=" + ddcStr + "&type=ddc";
              if (lang != null && ! lang.isEmpty())
                ddcLink = ddcLink + "&language=" + lang;
              String subjectSearchUrl = "/wspCmsWebApp/query/query.html?queryLanguage=lucene&query=ddc:&quot;" + ddcStr + "&quot;&fieldExpansion=none";
              String subjectImgLink = "<a href=\"" + ddcLink + "\">" + "<img src=\"/wspCmsWebApp/images/rdfSmall.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
              htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + ddcStr + "</a> (" + subjectImgLink + ")");
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          // Knowledge rows
          IndexableField entitiesField = doc.getField("entities");
          if (entitiesField != null) {
            htmlStrBuilder.append("<tr>");
            htmlStrBuilder.append("<td></td>");
            htmlStrBuilder.append("<td colspan=\"8\">");  
            htmlStrBuilder.append("<b>DBpedia spotlight entities:</b>");
            IndexableField entitiesDetailsField = doc.getField("entitiesDetails");
            if (entitiesDetailsField != null) {
              String entitiesDetailsXmlStr = entitiesDetailsField.stringValue();
              String entitiesDetailsHtmlStr = docEntitiesDetailsXmlStrToHtml(xQueryEvaluator, entitiesDetailsXmlStr, baseUrl, language, true);
              htmlStrBuilder.append(" " + entitiesDetailsHtmlStr);
            }
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          // WSP-Page-View / WSP-Download / Lucene-Metadata-View 
          htmlStrBuilder.append("<tr>");
          htmlStrBuilder.append("<td></td>");
          htmlStrBuilder.append("<td colspan=\"8\">");
          htmlStrBuilder.append("<b>WSP internal:</b>" + " Lucene score: " + luceneScore + ", ");
          String docIdPercentEscaped = docId.replaceAll("%", "%25"); // e.g. if docId contains "%20" then it is modified to "%2520"
          if (docIsXml) {
            if (firstHitPageNumber == null)
              htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/book.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetPage?docId=" + docIdPercentEscaped + "\">WSP-View</a>, ");
            else
              htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/book.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetPage?docId=" + docIdPercentEscaped + "&page=" + firstHitPageNumber + "&highlightQuery=" + query + "\">WSP-View</a>, ");
          }
          IndexableField content = doc.getField("content");
          if (content != null) {
            String contentStr = content.stringValue();
            if (contentStr != null && ! contentStr.isEmpty()) {
              htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/download.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/doc/GetDocument?id=" + docIdPercentEscaped + "\">Download</a>, ");
            }
          }
          htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/search.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetDocInfo?docId=" + docIdPercentEscaped + "\">MetadataView</a>");
          htmlStrBuilder.append("</td>");
          htmlStrBuilder.append("</tr>");
          htmlStrBuilder.append("</table>");
          htmlStrBuilder.append("</td>");
          htmlStrBuilder.append("</tr>");
        }
        htmlStrBuilder.append("</tbody>");
        htmlStrBuilder.append("</table>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("<div id=\"facets\" class=\"tab-pane\" style=\"border-top: 1px solid #ddd;border-left: 1px solid #ddd;border-right: 1px solid #ddd;border-bottom: 1px solid #ddd;border-radius: 0px 0px 5px 5px;padding: 10px;\">");
        if (outputOptions.contains("showAllFacets") || outputOptions.contains("showMainEntitiesFacet") || outputOptions.equals("showAll")) {
          Facets facets = hits.getFacets();
          if (facets != null && facets.size() > 0) {
            facets.setBaseUrl(baseUrl);
            facets.setOutputOptions(outputOptions);
            String facetsStr = facets.toHtmlString(true);
            htmlStrBuilder.append(facetsStr);
          }
        }
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("<div id=\"bottomInfo\" style=\"clear:both;margin-bottom:0;\">");
        htmlStrBuilder.append("<ul><li data-jstree='{\"icon\":\"glyphicon glyphicon-info-sign\"}'>[Technical info]");
        htmlStrBuilder.append("<ul>");
        htmlStrBuilder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-info-sign\"}'>Elapsed time: " + elapsedTime + " ms</li>");
        String luceneQueryStr = query;
        Query luceneQuery = hits.getQuery();
        if (query != null)
          luceneQueryStr = luceneQuery.toString();
        htmlStrBuilder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-info-sign\"}'>Lucene query: " + luceneQueryStr + "</li>");
        if (outputOptions.contains("showNumberOfDifferentTerms") || outputOptions.equals("showAll")) {
          htmlStrBuilder.append("<li data-jstree='{\"icon\":\"glyphicon glyphicon-info-sign\"}'>Number of different terms in all documents: " + sizeTotalTerms + "</li>");
        }
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("</li>");
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("</div>");
        out.print(htmlStrBuilder.toString());
      } else if (outputFormat.equals("html")) {
        StringBuilder htmlStrBuilder = new StringBuilder();
        htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        htmlStrBuilder.append("<html>");
        htmlStrBuilder.append("<head>");
        htmlStrBuilder.append("<title>Query: " + query + "</title>");
        htmlStrBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + request.getContextPath() + "/css/page.css" + "\"/>");
        htmlStrBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + request.getContextPath() + "/css/bootstrap.min.css\"/>");
        htmlStrBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + request.getContextPath() + "/css/style.min.css\"/>"); // jstree
        htmlStrBuilder.append("</head>");
        // body  
        htmlStrBuilder.append("<body style=\"margin-left:2px;margin-right:2px;\">");
        String luceneQueryStr = query;
        Query luceneQuery = hits.getQuery();
        if (query != null)
          luceneQueryStr = luceneQuery.toString();
        String sortByStr = sortBy;
        if (sortBy == null)
          sortByStr = "";
        // header
        htmlStrBuilder.append("<div style=\"border: none; font-family: Helvetica,Arial,sans-serif; font-size: 1.0em;\">");
        htmlStrBuilder.append("<table valign=\"top\" style=\"margin-left:2px;\">");
        htmlStrBuilder.append("<colgroup>");
        htmlStrBuilder.append("<col width=\"80%\"/>");
        htmlStrBuilder.append("<col width=\"20%\"/>");
        htmlStrBuilder.append("</colgroup>");
        htmlStrBuilder.append("<td align=\"left\" valign=\"middle\"><span style=\"font-weight:bold\">Lucene query: </span><span>" + luceneQueryStr + "</span></td>");
        htmlStrBuilder.append("<td align=\"right\" valign=\"middle\" nowrap=\"true\">[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW CMS service\"/></a></td>");
        htmlStrBuilder.append("</table>");
        htmlStrBuilder.append("</div>");
        // tabs
        htmlStrBuilder.append("<div id=\"tabs\">");
        htmlStrBuilder.append("<ul class=\"nav nav-tabs\" style=\"border: none; font-family: Helvetica,Arial,sans-serif; font-size: 1.0em;\">");
        htmlStrBuilder.append("<li role=\"presentation\" class=\"active\"><a href=\"#hits\" data-toggle=\"tab\">Hits</a></li>");
        htmlStrBuilder.append("<li role=\"presentation\"><a href=\"#facets\" data-toggle=\"tab\">Facets</a></li>");
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("<div id=\"my-tab-content\" class=\"tab-content\">");
        htmlStrBuilder.append("<div class=\"tab-pane active\" id=\"hits\">");
        if (outputOptions.contains("showHits") || outputOptions.equals("showAll")) {
          htmlStrBuilder.append("<form action=\"QueryDocuments\" method=\"get\">");
          htmlStrBuilder.append("<input type=\"hidden\" name=\"queryLanguage\" value=\"" + queryLanguage + "\"/>");
          String queryPercentEncoded = query.replaceAll("\"", "%22"); // valid html: double quote has to be percent encoded
          htmlStrBuilder.append("<input type=\"hidden\" name=\"query\" value=\"" + queryPercentEncoded + "\"/>");
          if (translate != null)
            htmlStrBuilder.append("<input type=\"hidden\" name=\"translate\" value=\"" + translate + "\"/>");
          if (language != null)
            htmlStrBuilder.append("<input type=\"hidden\" name=\"language\" value=\"" + language + "\"/>");
          htmlStrBuilder.append("<input type=\"hidden\" name=\"page\" id=\"pageId\" value=\"" + page + "\"/>");
          htmlStrBuilder.append("<input type=\"hidden\" name=\"pageSize\" id=\"pageSizeId\" value=\"" + pageSize + "\"/>");
          htmlStrBuilder.append("<input type=\"hidden\" name=\"sortBy\" id=\"sortById\" value=\"" + sortByStr + "\"/>");
          htmlStrBuilder.append("<input type=\"hidden\" name=\"fieldExpansion\" id=\"fieldExpansion\" value=\"" + fieldExpansion + "\"/>");
          htmlStrBuilder.append("<input type=\"submit\" id=\"submitId\" style=\"position: absolute; left: -9999px\"/>");
          htmlStrBuilder.append("<table>");
          htmlStrBuilder.append("<colgroup>");
          htmlStrBuilder.append("<col width=\"3\"/>");
          htmlStrBuilder.append("<col width=\"10\"/>");
          htmlStrBuilder.append("<col width=\"3\"/>");
          htmlStrBuilder.append("<col width=\"5\"/>");
          htmlStrBuilder.append("<col width=\"60\"/>");
          htmlStrBuilder.append("<col width=\"80%\"/>");
          htmlStrBuilder.append("</colgroup>");
          htmlStrBuilder.append("<tr>");
          int countPages = hitsSize / pageSize + 1;
          if (hitsSize % pageSize == 0) // modulo operator: e.g. 280 % 10 is 0
            countPages = hitsSize / pageSize;
          int pageLeft = page - 1;
          if (page == 1)
            pageLeft = 1;
          int pageRight = page + 1; 
          if (page == countPages)
            pageRight = countPages;
          htmlStrBuilder.append("<td align=\"left\" valign=\"middle\" style=\"padding-right:4px\"><button type=\"button\" onclick=\"document.getElementById('pageId').value=" + pageLeft + "; document.getElementById('submitId').click();\" style=\"background:none;border:none;\"><img src=\"../images/left.gif\"/></button></td>");
          htmlStrBuilder.append("<td align=\"middle\" valign=\"middle\" nowrap=\"true\" style=\"padding-right:4px\">" + page + " / " + countPages + "</td>");
          htmlStrBuilder.append("<td align=\"left\" valign=\"middle\" style=\"padding-right:4px\"><button type=\"button\" onclick=\"document.getElementById('pageId').value=" + pageRight + "; document.getElementById('submitId').click();\" style=\"background:none;border:none;\"><img src=\"../images/right.gif\"/></button></td>");
          htmlStrBuilder.append("<td align=\"left\" valign=\"middle\" nowrap=\"true\" style=\"padding-right:4px\"> Page: </td>");
          htmlStrBuilder.append("<td align=\"left\" valign=\"middle\" nowrap=\"true\"><input type=\"text\" size=\"3\" value=\"" + page + "\" id=\"pageTextId\" onkeydown=\"if (event.keyCode == 13) {document.getElementById('pageId').value=document.getElementById('pageTextId').value; document.getElementById('submitId').click();}\"/></td>");
          int fromDisplay = from + 1;
          int toDisplay = to + 1;
          if (hitsSize < toDisplay)
            toDisplay = hitsSize;
          htmlStrBuilder.append("<td align=\"right\" valign=\"middle\">" + fromDisplay + " - " + toDisplay + " of " + hitsSize + " hits (out of " + sizeTotalDocuments + " resources)" + "</td>");
          htmlStrBuilder.append("</tr>");
          htmlStrBuilder.append("</table>");
          htmlStrBuilder.append("<p/>");
          htmlStrBuilder.append("<table width=\"100%\" align=\"right\" border=\"1px solid #ddd\" rules=\"groups\">");
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
          htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + "<button onclick=\"document.getElementById('sortById').value='type'\" style=\"padding:0px;font-weight:bold;font-size:14px;background:none;border:none;\">" + "Type" + "</button></td>");
          htmlStrBuilder.append("</tr>");
          htmlStrBuilder.append("</thead>");
          htmlStrBuilder.append("<tbody>");
          for (int i=0; i<docsSize; i++) {
            Document doc = docs.get(i);
            Float luceneScore = doc.getScore();
            IndexableField docProjectIdField = doc.getField("projectId");
            String projectId = null;
            if (docProjectIdField != null) {
              projectId = docProjectIdField.stringValue();
            }
            IndexableField languageField = doc.getField("language");
            String lang = "";
            if (languageField != null)
              lang = languageField.stringValue();
            htmlStrBuilder.append("<tr valign=\"top\">");
            int num = (page - 1) * pageSize + i + 1;
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + num + ". " + "</td>");
            IndexableField docAuthorField = doc.getField("author");
            String authorHtml = "";
            if (docAuthorField != null) {
              IndexableField docAuthorDetailsField = doc.getField("authorDetails");
              if (docAuthorDetailsField != null) {
                String docAuthorDetailsXmlStr = docAuthorDetailsField.stringValue();
                authorHtml = docPersonsDetailsXmlStrToHtml(xQueryEvaluator, docAuthorDetailsXmlStr, baseUrl, lang);            
              } else {
                String authorName = docAuthorField.stringValue();
                Person author = new Person();
                author.setName(authorName);
                String aboutPersonLink = baseUrl + "/query/About?query=" + authorName + "&type=person";
                if (lang != null && ! lang.isEmpty())
                  aboutPersonLink = aboutPersonLink + "&language=" + lang;
                author.setAboutLink(aboutPersonLink);
                String htmlStrPerson = author.toHtmlStr();
                authorHtml = "<span class=\"persons\">";
                authorHtml = authorHtml + htmlStrPerson;
                authorHtml = authorHtml + "</span>";
              }
            }          
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + authorHtml + "</td>");
            IndexableField titleField = doc.getField("title");
            String title = "";
            if (titleField != null)
              title = titleField.stringValue();
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + title + "</td>");
            IndexableField publisherField = doc.getField("publisher");
            String publisher = "";
            if (publisherField != null)
              publisher = publisherField.stringValue();
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + publisher + "</td>");
            IndexableField yearField = doc.getField("date");
            String year = "";
            if (yearField != null)
              year = yearField.stringValue();
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + year + "</td>");
            String docId = doc.getField("docId").stringValue();
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + docId + "</td>");
            IndexableField lastModifiedField = doc.getField("lastModified");
            String lastModified = "";
            if (lastModifiedField != null)
              lastModified = lastModifiedField.stringValue();
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + lastModified + "</td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" style=\"padding-left:5px\">" + lang + "</td>");
            IndexableField typeField = doc.getField("type");
            String type = "";
            if (typeField != null)
              type = typeField.stringValue();
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\">" + type + "</td>");
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
            // project links row
            String projectUrl = null;
            boolean docIsXml = false; 
            String firstHitPageNumber = null;
            String mimeType = getMimeType(docId);
            if (mimeType != null && mimeType.contains("xml"))
              docIsXml = true;
            if (docIsXml)
              firstHitPageNumber = doc.getFirstHitPageNumber();
            if (projectId != null) {
              Project project = ProjectReader.getInstance().getProject(projectId);
              if (project != null) {
                projectUrl = project.getHomepageUrl();
                String projectRdfId = project.getRdfId();
                String projectTitle = project.getTitle();
                htmlStrBuilder.append("<tr valign=\"top\">");
                htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
                htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
                htmlStrBuilder.append("<b>Project links</b>: ");
                IndexableField webUriField = doc.getField("webUri");
                String webUri = null;
                if (webUriField != null)
                  webUri = webUriField.stringValue();
                if (webUri != null) {
                  if (! webUri.contains("%"))
                    webUri = URIUtil.encodeQuery(webUri);  
                  webUri = webUri.replaceAll("%23", "#"); // for e.g.: http://telota.bbaw.de/mega/%23?doc=MEGA_A2_B005-00_ETX.xml
                  htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + webUri + "\">Project view</a> ");
                }
                htmlStrBuilder.append("(" + projectTitle + " (" + projectId + "): ");
                if (projectUrl != null) {
                  htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/linkext.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + projectUrl + "\">Project homepage</a>, ");
                }
                if (projectRdfId != null) {
                  String projectDetailsUrl = "/wspCmsWebApp/query/QueryMdSystem?query=" + projectRdfId + "&detailedSearch=true";
                  htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/search.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"" + projectDetailsUrl + "\">Project details</a>)");
                }
                htmlStrBuilder.append("</td>");
                htmlStrBuilder.append("</tr>");
              }
            }
            // description row
            IndexableField descriptionField = doc.getField("description");
            if (descriptionField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
              htmlStrBuilder.append("<b>Description</b>: ");
              String description = descriptionField.stringValue();
              if (description != null && description.length() > 400)
                description = description.substring(0, 400) + " (...)";
              htmlStrBuilder.append(description);
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            IndexableField personsField = doc.getField("persons");
            if (personsField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");  
              htmlStrBuilder.append("<b>Persons</b>: ");
              IndexableField personsDetailsField = doc.getField("personsDetails");
              if (personsDetailsField != null) {
                String personsDetailsXmlStr = personsDetailsField.stringValue();
                String personsDetailsHtmlStr = docPersonsDetailsXmlStrToHtml(xQueryEvaluator, personsDetailsXmlStr, baseUrl, language);
                htmlStrBuilder.append(personsDetailsHtmlStr);
              } else {
                String personsStr = personsField.stringValue();
                String[] persons = personsStr.split("###");  // separator of persons
                for (int j=0; j<persons.length; j++) {
                  String personName = persons[j];
                  Person person = new Person();
                  person.setRole(Person.MENTIONED);
                  person.setName(personName);
                  String aboutPersonLink = baseUrl + "/query/About?query=" + personName + "&type=person";
                  if (lang != null && ! lang.isEmpty())
                    aboutPersonLink = aboutPersonLink + "&language=" + lang;
                  person.setAboutLink(aboutPersonLink);
                  String htmlStrPerson = person.toHtmlStr();
                  htmlStrBuilder.append(htmlStrPerson);
                  if (j != persons.length - 1)
                    htmlStrBuilder.append(", ");
                }
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            IndexableField placesField = doc.getField("places");
            if (placesField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
              htmlStrBuilder.append("<b>Places</b>: ");
              String placesStr = placesField.stringValue();
              String[] places = placesStr.split("###");  // separator of places
              places = cleanNames(places);
              Arrays.sort(places, ignoreCaseComparator);
              for (int j=0; j<places.length; j++) {
                String placeName = places[j];
                if (! placeName.isEmpty()) {
                  String placeLink = "/wspCmsWebApp/query/About?query=" + placeName + "&type=place";
                  if (lang != null && ! lang.isEmpty())
                    placeLink = placeLink + "&language=" + lang;
                  htmlStrBuilder.append("<a href=\"" + placeLink + "\">" + placeName +"</a>");
                  if (j != places.length - 1)
                    htmlStrBuilder.append(", ");
                }
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            IndexableField subjectControlledDetailsField = doc.getField("subjectControlledDetails");
            if (subjectControlledDetailsField != null) {
              String subjectControlledDetailsStr = subjectControlledDetailsField.stringValue();
              org.jsoup.nodes.Document subjectControlledDetailsDoc = Jsoup.parse(subjectControlledDetailsStr);
              Elements dctermsSubjects = subjectControlledDetailsDoc.select("subjects > dcterms|subject");
              if (dctermsSubjects != null && dctermsSubjects.size() > 0) {
                htmlStrBuilder.append("<tr>");
                htmlStrBuilder.append("<td></td>");
                htmlStrBuilder.append("<td colspan=\"8\">");
                htmlStrBuilder.append("<b>Subjects (controlled)</b>: ");
                for (int j=0; j<dctermsSubjects.size(); j++) {
                  Element dctermsSubject = dctermsSubjects.get(j);
                  // e.g.: <dcterms:subject rdf:resource="http://d-nb.info/gnd/4037764-7"/>
                  String rdfIdSubject = dctermsSubject.attr("rdf:resource");
                  Subject subject = ProjectReader.getInstance().getSubject(rdfIdSubject);
                  if (subject != null) {
                    String subjectRdfType = subject.getType();
                    String subjectRdfLink = subject.getRdfId();
                    String subjectName = subject.getName();
                    String subjectSearchUrl = "/wspCmsWebApp/query/query.html?queryLanguage=lucene&query=subjectControlled:&quot;" + subjectName + "&quot;&fieldExpansion=none";
                    String ontologyName = getOntologyName(subjectRdfType);
                    String ontologyNameStr = ontologyName + ": ";
                    if (ontologyName == null)
                      ontologyNameStr = "";
                    String subjectRdfImgLink = "<a href=\"" + subjectRdfLink + "\">" + "<img src=\"/wspCmsWebApp/images/" + "rdfSmall.gif" + "\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                    htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + subjectName + "</a> (" + ontologyNameStr + subjectRdfImgLink + ")");
                  }
                  if (j < dctermsSubjects.size() - 1)
                    htmlStrBuilder.append(", ");
                }
                htmlStrBuilder.append("</td>");
                htmlStrBuilder.append("</tr>");
              }
            }
            IndexableField subjectField = doc.getField("subject");
            if (subjectField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
              htmlStrBuilder.append("<b>Subjects (free)</b>: ");
              String subjectStr = subjectField.stringValue();
              String[] subjects = subjectStr.split("[,]");  // one separator of subjects
              if (subjectStr.contains("###"))
                subjects = subjectStr.split("###");  // another separator of subjects
              subjects = cleanNames(subjects);
              Arrays.sort(subjects, ignoreCaseComparator);
              for (int j=0; j<subjects.length; j++) {
                String subjectName = subjects[j];
                if (! subjectName.isEmpty()) {
                  String subjectLink = "/wspCmsWebApp/query/About?query=" + subjectName + "&type=subject";
                  if (lang != null && ! lang.isEmpty())
                    subjectLink = subjectLink + "&language=" + lang;
                  String subjectSearchUrl = "/wspCmsWebApp/query/QueryDocuments?query=subject:&quot;" + subjectName + "&quot;&fieldExpansion=none";
                  String subjectImgLink = "<a href=\"" + subjectLink + "\">" + "<img src=\"/wspCmsWebApp/images/rdfSmall.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                  htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + subjectName + "</a> (" + subjectImgLink + ")");
                  if (j != subjects.length - 1)
                    htmlStrBuilder.append(", ");
                }
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            IndexableField swdField = doc.getField("swd");
            if (swdField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
              htmlStrBuilder.append("<b>SWD</b>: ");
              String swdStr = swdField.stringValue();
              String[] swds = swdStr.split("[,]");  // separator of subjects
              swds = cleanNames(swds);
              Arrays.sort(swds, ignoreCaseComparator);
              for (int j=0; j<swds.length; j++) {
                String swdName = swds[j];
                if (! swdName.isEmpty()) {
                  String swdLink = "/wspCmsWebApp/query/About?query=" + swdName + "&type=swd";
                  if (lang != null && ! lang.isEmpty())
                    swdLink = swdLink + "&language=" + lang;
                  String subjectSearchUrl = "/wspCmsWebApp/query/QueryDocuments?query=swd:&quot;" + swdName + "&quot;&fieldExpansion=none";
                  String subjectImgLink = "<a href=\"" + swdLink + "\">" + "<img src=\"/wspCmsWebApp/images/rdfSmall.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                  htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + swdName + "</a> (" + subjectImgLink + ")");
                  if (j != swds.length - 1)
                    htmlStrBuilder.append(", ");
                }
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            IndexableField ddcField = doc.getField("ddc");
            if (ddcField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
              htmlStrBuilder.append("<b>DDC</b>: ");
              String ddcStr = ddcField.stringValue();
              if (! ddcStr.isEmpty()) {
                String ddcLink = "/wspCmsWebApp/query/About?query=" + ddcStr + "&type=ddc";
                if (lang != null && ! lang.isEmpty())
                  ddcLink = ddcLink + "&language=" + lang;
                String subjectSearchUrl = "/wspCmsWebApp/query/QueryDocuments?query=ddc:&quot;" + ddcStr + "&quot;&fieldExpansion=none";
                String subjectImgLink = "<a href=\"" + ddcLink + "\">" + "<img src=\"/wspCmsWebApp/images/rdfSmall.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + "</a>";
                htmlStrBuilder.append("<a href=\"" + subjectSearchUrl + "\">" + ddcStr + "</a> (" + subjectImgLink + ")");
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            // Knowledge rows
            IndexableField entitiesField = doc.getField("entities");
            if (entitiesField != null) {
              htmlStrBuilder.append("<tr valign=\"top\">");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
              htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");  
              htmlStrBuilder.append("<b>DBpedia spotlight entities:</b>");
              IndexableField entitiesDetailsField = doc.getField("entitiesDetails");
              if (entitiesDetailsField != null) {
                String entitiesDetailsXmlStr = entitiesDetailsField.stringValue();
                String entitiesDetailsHtmlStr = docEntitiesDetailsXmlStrToHtml(xQueryEvaluator, entitiesDetailsXmlStr, baseUrl, language, false);
                htmlStrBuilder.append(" " + entitiesDetailsHtmlStr);
              }
              htmlStrBuilder.append("</td>");
              htmlStrBuilder.append("</tr>");
            }
            // WSP-Page-View / WSP-Download / Lucene-Metadata-View 
            htmlStrBuilder.append("<tr valign=\"top\">");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\"></td>");
            htmlStrBuilder.append("<td align=\"left\" valign=\"top\" colspan=\"8\">");
            htmlStrBuilder.append("<b>WSP internal:</b>" + " Lucene score: " + luceneScore + ", ");
            String docIdPercentEscaped = docId.replaceAll("%", "%25"); // e.g. if docId contains "%20" then it is modified to "%2520"
            if (docIsXml) {
              if (firstHitPageNumber == null)
                htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/book.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetPage?docId=" + docIdPercentEscaped + "\">WSP-View</a>, ");
              else
                htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/book.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetPage?docId=" + docIdPercentEscaped + "&page=" + firstHitPageNumber + "&highlightQuery=" + query + "\">WSP-View</a>, ");
            }
            IndexableField content = doc.getField("content");
            if (content != null) {
              String contentStr = content.stringValue();
              if (contentStr != null && ! contentStr.isEmpty()) {
                htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/download.png\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/doc/GetDocument?id=" + docIdPercentEscaped + "\">Download</a>, ");
              }
            }
            htmlStrBuilder.append("<img src=\"/wspCmsWebApp/images/search.gif\" width=\"15\" height=\"15\" border=\"0\"/>" + " <a href=\"/wspCmsWebApp/query/GetDocInfo?docId=" + docIdPercentEscaped + "\">MetadataView</a>");
            htmlStrBuilder.append("</td>");
            htmlStrBuilder.append("</tr>");
          }
          htmlStrBuilder.append("</tbody>");
          htmlStrBuilder.append("</table>");
        }
        htmlStrBuilder.append("</form>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("<div class=\"tab-pane\" id=\"facets\" style=\"border-top: 1px solid #ddd;border-left: 1px solid #ddd;border-right: 1px solid #ddd;border-bottom: 1px solid #ddd;border-radius: 0px 0px 5px 5px;padding: 10px;\">");
        if (outputOptions.contains("showAllFacets") || outputOptions.contains("showMainEntitiesFacet") || outputOptions.equals("showAll")) {
          Facets facets = hits.getFacets();
          if (facets != null && facets.size() > 0) {
            facets.setBaseUrl(baseUrl);
            facets.setOutputOptions(outputOptions);
            String facetsStr = facets.toHtmlString(false);
            htmlStrBuilder.append(facetsStr);
          }
        }
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("<div id=\"bottomInfo\" style=\"clear:both;margin-bottom:0;\">");
        htmlStrBuilder.append("<ul><li data-jstree=\"{'icon':'glyphicon glyphicon-info-sign'}\">[Technical info]");
        htmlStrBuilder.append("<ul>");
        htmlStrBuilder.append("<li data-jstree=\"{'icon':'glyphicon glyphicon-info-sign'}\">Elapsed time: " + elapsedTime + " ms</li>");
        if (outputOptions.contains("showNumberOfDifferentTerms") || outputOptions.equals("showAll")) {
          htmlStrBuilder.append("<li data-jstree=\"{'icon':'glyphicon glyphicon-info-sign'}\">Number of different terms in all documents: " + sizeTotalTerms + "</li>");
        }
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("</li>");
        htmlStrBuilder.append("</ul>");
        htmlStrBuilder.append("</div>");
        htmlStrBuilder.append("<script src=\"" + request.getContextPath() + "/js/jquery-1.11.2.min.js" + "\"></script>");
        htmlStrBuilder.append("<script src=\"" + request.getContextPath() + "/js/bootstrap.min.js" + "\"></script>");
        htmlStrBuilder.append("<script src=\"" + request.getContextPath() + "/js/jstree.min.js" + "\"></script>");
        htmlStrBuilder.append("<script>");
        htmlStrBuilder.append("$(function () {");
        htmlStrBuilder.append("  $('#facets').jstree().bind(\"select_node.jstree\", function(e, data) {");
        htmlStrBuilder.append("    window.location.href = data.node.a_attr.href;");  // so that hrefs are not disabled
        htmlStrBuilder.append("  });");
        htmlStrBuilder.append("  $('#bottomInfo').jstree().bind(\"select_node.jstree\", function(e, data) {");
        htmlStrBuilder.append("    window.location.href = data.node.a_attr.href;");  // so that hrefs are not disabled
        htmlStrBuilder.append("  });");
        htmlStrBuilder.append("});");
        htmlStrBuilder.append("</script>");
        htmlStrBuilder.append("</body>");
        htmlStrBuilder.append("</html>");
        out.print(htmlStrBuilder.toString());
      } else if (outputFormat.equals("json")) {
        JSONObject jsonOutput = new JSONObject();
        jsonOutput.put("searchTerm", query);
        jsonOutput.put("numberOfHits", String.valueOf(hitsSize));
        if (outputOptions.contains("showAllFacets") || outputOptions.contains("showMainEntitiesFacet") || outputOptions.equals("showAll")) {
          Facets facets = hits.getFacets();
          if (facets != null && facets.size() > 0) {
            facets.setBaseUrl(baseUrl);
            facets.setOutputOptions(outputOptions);
            JSONObject jsonFacets = facets.toJsonObject();
            jsonOutput.put("facets", jsonFacets);
          }
        }
        jsonOutput.put("sizeTotalDocuments", String.valueOf(sizeTotalDocuments));
        jsonOutput.put("sizeTotalTerms", String.valueOf(sizeTotalTerms));
        jsonOutput.put("elapsedTime", String.valueOf(elapsedTime));
        if (outputOptions.contains("showHits") || outputOptions.equals("showAll")) {
          JSONArray jsonHits = new JSONArray();
          for (int i=0; i<docsSize; i++) {
            org.bbaw.wsp.cms.document.Document doc = docs.get(i);
            doc.setBaseUrl(baseUrl);
            JSONObject jsonHit = doc.toJsonObject();
            jsonHits.add(jsonHit);
          }
          jsonOutput.put("hits", jsonHits);
        }
        ArrayList<GroupDocuments> groupByHits = hits.getGroupByHits();
        if (groupByHits != null) {
          JSONArray jsonGroupByHits = new JSONArray();
          for (int i=0; i<groupByHits.size(); i++) {
            GroupDocuments groupDocuments = groupByHits.get(i);
            groupDocuments.setBaseUrl(baseUrl);
            JSONObject jsonGroupDocument = groupDocuments.toJsonObject();
            jsonGroupByHits.add(jsonGroupDocument);
          }
          jsonOutput.put("groupByHits", jsonGroupByHits);
        }
        out.println(jsonOutput.toJSONString());
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private String docEntitiesDetailsXmlStrToHtml(XQueryEvaluator xQueryEvaluator, String docEntitiesDetailsXmlStr, String baseUrl, String language, boolean htmlSmart) throws ApplicationException {
    ArrayList<DBpediaResource> entities = DBpediaResource.fromXmlStr(xQueryEvaluator, docEntitiesDetailsXmlStr);
    ArrayList<DBpediaResource> entitiesPerson = new ArrayList<DBpediaResource>();
    ArrayList<DBpediaResource> entitiesOrganisation = new ArrayList<DBpediaResource>();
    ArrayList<DBpediaResource> entitiesConcept = new ArrayList<DBpediaResource>();
    ArrayList<DBpediaResource> entitiesPlace = new ArrayList<DBpediaResource>();
    for (int i=0; i<entities.size(); i++) {
      DBpediaResource entity = entities.get(i);
      entity.setBaseUrl(baseUrl);
      String type = entity.getType();
      if (type != null && type.equals("person"))
        entitiesPerson.add(entity);
      else if (type != null && type.equals("organisation"))
        entitiesOrganisation.add(entity);
      else if (type != null && type.equals("concept"))
        entitiesConcept.add(entity);
      else if (type != null && type.equals("place"))
        entitiesPlace.add(entity);
    }
    StringBuilder retHtmlStrBuilder = new StringBuilder(); 
    if (entitiesPerson.size() > 0) {
      retHtmlStrBuilder.append("<li style=\"margin-left: 30px;\"><b>Persons</b>: ");
      for (int i=0; i<entitiesPerson.size(); i++) {
        DBpediaResource entity = entitiesPerson.get(i);
        String htmlStrEntity = entity.toHtmlStr(false);
        if (htmlSmart)
          htmlStrEntity = entity.toHtmlSmartStr(false);
        if (i == entitiesPerson.size() - 1)
          retHtmlStrBuilder.append(htmlStrEntity);
        else 
          retHtmlStrBuilder.append(htmlStrEntity + ", ");
      }
      retHtmlStrBuilder.append("</li>");
    }
    if (entitiesOrganisation.size() > 0) {
      retHtmlStrBuilder.append("<li style=\"margin-left: 30px;\"><b>Organisations</b>: ");
      for (int i=0; i<entitiesOrganisation.size(); i++) {
        DBpediaResource entity = entitiesOrganisation.get(i);
        String htmlStrEntity = entity.toHtmlStr(false);
        if (htmlSmart)
          htmlStrEntity = entity.toHtmlSmartStr(false);
        if (i == entitiesOrganisation.size() - 1)
          retHtmlStrBuilder.append(htmlStrEntity);
        else 
          retHtmlStrBuilder.append(htmlStrEntity + ", ");
      }
      retHtmlStrBuilder.append("</li>");
    }
    if (entitiesConcept.size() > 0) {
      retHtmlStrBuilder.append("<li style=\"margin-left: 30px;\"><b>Concepts</b>: ");
      for (int i=0; i<entitiesConcept.size(); i++) {
        DBpediaResource entity = entitiesConcept.get(i);
        String htmlStrEntity = entity.toHtmlStr(false);
        if (htmlSmart)
          htmlStrEntity = entity.toHtmlSmartStr(false);
        if (i == entitiesConcept.size() - 1)
          retHtmlStrBuilder.append(htmlStrEntity);
        else 
          retHtmlStrBuilder.append(htmlStrEntity + ", ");
      }
      retHtmlStrBuilder.append("</li>");
    }
    if (entitiesPlace.size() > 0) {
      retHtmlStrBuilder.append("<li style=\"margin-left: 30px;\"><b>Places</b>: ");
      for (int i=0; i<entitiesPlace.size(); i++) {
        DBpediaResource entity = entitiesPlace.get(i);
        String htmlStrEntity = entity.toHtmlStr(false);
        if (htmlSmart)
          htmlStrEntity = entity.toHtmlSmartStr(false);
        if (i == entitiesPlace.size() - 1)
          retHtmlStrBuilder.append(htmlStrEntity);
        else 
          retHtmlStrBuilder.append(htmlStrEntity + ", ");
      }
      retHtmlStrBuilder.append("</li>");
    }
    return retHtmlStrBuilder.toString();
  }
  
  private String docPersonsDetailsXmlStrToHtml(XQueryEvaluator xQueryEvaluator, String docPersonsDetailsXmlStr, String baseUrl, String language) throws ApplicationException {
    ArrayList<Person> persons = Person.fromXmlStr(xQueryEvaluator, docPersonsDetailsXmlStr);
    String retHtmlStr = "<span class=\"persons\">";
    for (int i=0; i<persons.size(); i++) {
      Person person = persons.get(i);
      person.setLanguage(language);
      String aboutPersonLink = "/query/About?query=" + person.getName() + "&type=person";
      if (language != null && ! language.isEmpty())
        aboutPersonLink = aboutPersonLink + "&language=" + language;
      person.setAboutLink(baseUrl + aboutPersonLink);
      String htmlStrPerson = person.toHtmlStr();
      retHtmlStr = retHtmlStr + htmlStrPerson + ", ";
    }
    retHtmlStr = retHtmlStr.substring(0, retHtmlStr.length() - 2);  // remove last comma
    retHtmlStr = retHtmlStr + "</span>";
    return retHtmlStr;
  }
  
  private String translateLuceneToQueryLanguage(String inputQuery, String queryLanguage) {
    String outputQuery = null;
    if (queryLanguage == null) {
      return inputQuery;
    } else if (queryLanguage.equals("ddc")) {
      // TODO translate lucene query properly to ddc
      outputQuery = inputQuery.trim(); 
      outputQuery = outputQuery.replaceAll("tokenOrig:|tokenMorph:|tokenNorm:", "");
      if (outputQuery.matches(".+:.+ *.*")) {  // contains fields, e.g.: +author:("Marx, Karl") +"Das Kapital" +projectId:("dta")
        outputQuery = outputQuery.replaceAll("[\\+\\-].+:[\\(].+?[\\)] | [\\+\\-].+:[\\(].+?[\\)]", ""); // remove fields
      }
      outputQuery = outputQuery.replaceAll("\\(|\\)|\\+", "");
      if (! outputQuery.contains("\""))  // if it is not a phrase search (like: "schlimme winterzeit") then replace blank (" ") with or ("||")
        outputQuery = outputQuery.replaceAll(" ", " || ");
      // outputQuery = outputQuery.replaceAll("", "");  // TODO "+searchTerm1 +searchTerm2" ersetzen durch "searchTerm1 && searchTerm2"
    }
    return outputQuery;
  }

  private String[] cleanNames(String[] names) {
    for (int j=0; j<names.length; j++) {
      String placeName = names[j];
      placeName = placeName.trim();
      placeName = placeName.replaceAll("-\\s([^u]?)|\\.|^-", "$1");
      names[j] = placeName;
    }
    // Dubletten entfernen
    HashSet<String> namesSet = new HashSet<String>(Arrays.asList(names));
    String[] namesArray = new String[namesSet.size()];
    namesSet.toArray(namesArray);
    return namesArray;
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private String getOntologyName(String rdfType) {
    String ontologyName = null; 
    if (rdfType != null) {
      if (rdfType.contains("skos"))
        ontologyName = "DBpedia";
      else if(rdfType.contains("gnd"))
        ontologyName = "GND";
      else if(rdfType.contains("DDC"))
        ontologyName = "DDC";
    }
    return ontologyName;
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
