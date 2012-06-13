package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Fieldable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xerces.internal.parsers.SAXParser;

import org.bbaw.wsp.cms.document.Document;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.transform.HighlightContentHandler;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class QueryDocument extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private XslResourceTransformer highlightTransformer = null;
  
  public QueryDocument() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    ServletContext context = getServletContext();
    highlightTransformer = (XslResourceTransformer) context.getAttribute("highlightTransformer");
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("docId");
    String query = request.getParameter("query");
    String[] normFunctions = {"none"};
    if (query.contains("tokenReg"))  // TODO ordentlich behandeln
      normFunctions[0] = "reg";
    else if (query.contains("tokenNorm"))  // TODO ordentlich behandeln
      normFunctions[0] = "norm";
    String[] outputOptions = {};
    if (query.contains("tokenMorph")) {  // TODO ordentlich behandeln
      outputOptions = new String[1];
      outputOptions[0] = "withLemmas";
    }
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
      Hits hits = indexHandler.queryDocument(docId, query, from, to);
      MetadataRecord docMetadataRecord = indexHandler.getDocMetadata(docId);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else if (outputFormat.equals("json"))
        response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      String resultStr = "";
      if (outputFormat.equals("xml"))
        resultStr = createXmlString(docMetadataRecord, query, page, pageSize, normFunctions, outputOptions, hits);
      else if (outputFormat.equals("html")) 
        resultStr = createHtmlString(docMetadataRecord, query, page, pageSize, normFunctions, outputOptions, hits, request);
      else if (outputFormat.equals("json")) 
        resultStr = createJsonString(docMetadataRecord, query, page, pageSize, normFunctions, outputOptions, hits, request);
      out.print(resultStr);
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  private String createXmlString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, String[] normFunctions, String[] outputOptions, Hits hits) throws ApplicationException {
    if (docMetadataRecord == null) {
      StringBuilder xmlStrBuilder = new StringBuilder();
      xmlStrBuilder.append("<document>");
      xmlStrBuilder.append("<id></id>");
      xmlStrBuilder.append("<query>");
      xmlStrBuilder.append("<queryText>" + query + "</queryText>");
      xmlStrBuilder.append("<resultPage>" + page + "</resultPage>");
      xmlStrBuilder.append("<resultPageSize>" + pageSize + "</resultPageSize>");
      xmlStrBuilder.append("</query>");
      xmlStrBuilder.append("<hitsSize>0</hitsSize>");
      xmlStrBuilder.append("<hits></hits>");
      xmlStrBuilder.append("</document>");
      return xmlStrBuilder.toString();   
    }
    String docId = docMetadataRecord.getDocId();
    ArrayList<Document> docs = null;
    if (hits != null)
      docs = hits.getHits();
    int docsSize = 0;
    if (hits != null)
      docsSize = hits.getSize();
    int from = (page * pageSize) - pageSize;  // e.g. 0
    int to = page * pageSize - 1;  // e.g. 9
    if (to >= docsSize)
      to = docsSize - 1;
    StringBuilder xmlStrBuilder = new StringBuilder();
    xmlStrBuilder.append("<document>");
    xmlStrBuilder.append("<id>" + docId + "</id>");
    xmlStrBuilder.append("<query>");
    xmlStrBuilder.append("<queryText>" + query + "</queryText>");
    xmlStrBuilder.append("<resultPage>" + page + "</resultPage>");
    xmlStrBuilder.append("<resultPageSize>" + pageSize + "</resultPageSize>");
    xmlStrBuilder.append("</query>");
    xmlStrBuilder.append("<hitsSize>" + docsSize + "</hitsSize>");
    xmlStrBuilder.append("<hits>");
    for (int i=from; i<=to; i++) {
      Document doc = docs.get(i);
      int num = i + 1;
      xmlStrBuilder.append("<hit>");
      xmlStrBuilder.append("<num>" + num + "</num>");
      String pageNumber = null;
      Fieldable fPageNumber = doc.getFieldable("pageNumber");
      if (fPageNumber != null) {
        pageNumber = fPageNumber.stringValue();
        xmlStrBuilder.append("<pageNumber>" + pageNumber + "</pageNumber>");
      }
      String elementPagePosition = null;
      Fieldable fElementPagePosition = doc.getFieldable("elementPagePosition");
      if (fElementPagePosition != null) {
        elementPagePosition = fElementPagePosition.stringValue();
        xmlStrBuilder.append("<pagePosition>" + elementPagePosition + "</pagePosition>");
      }
      String lineNumber = null;
      Fieldable fLineNumber = doc.getFieldable("lineNumber");
      if (fLineNumber != null) {
        lineNumber = fLineNumber.stringValue();
        xmlStrBuilder.append("<lineNumber>" + lineNumber + "</lineNumber>");
      }
      String elementPosition = null;
      Fieldable fElementPosition = doc.getFieldable("elementAbsolutePosition");
      if (fElementPosition != null) {
        elementPosition = fElementPosition.stringValue();
        xmlStrBuilder.append("<absolutePosition>" + elementPosition + "</absolutePosition>");
      }
      String xpath = null;
      Fieldable fXPath = doc.getFieldable("xpath");
      if (fXPath != null) {
        xpath = fXPath.stringValue();
        xmlStrBuilder.append("<xpath>" + xpath + "</xpath>");
      }
      String xmlId = null;
      Fieldable fXmlId = doc.getFieldable("xmlId");
      if (fXmlId != null) {
        xmlId = fXmlId.stringValue();
        xmlStrBuilder.append("<xmlId>" + xmlId + "</xmlId>");
      }
      String language = null;
      Fieldable fLanguage = doc.getFieldable("language");
      if (fLanguage != null) {
        language = fLanguage.stringValue();
        xmlStrBuilder.append("<language>" + language + "</language>");
      }
      String xmlContentTokenized = null;
      Fieldable fXmlContentTokenized = doc.getFieldable("xmlContentTokenized");
      if (fXmlContentTokenized != null) {
        String highlightQueryType = "orig";
        if (withLemmas(outputOptions)) {
          highlightQueryType = "morph";
        } else if (normFunctions != null) { 
          String normFunction = normFunctions[0];
          highlightQueryType = normFunction;
          if (normFunction.equals("none")) {
            highlightQueryType = "orig";
          }
        }
        xmlContentTokenized = fXmlContentTokenized.stringValue();
        String xmlPre = "<content xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
        String xmlPost = "</content>";
        String xmlInputStr = xmlPre + xmlContentTokenized + xmlPost;
        String docLanguage = docMetadataRecord.getLanguage();
        String highlightedXmlStr = highlight(xmlInputStr, highlightQueryType, query, docLanguage);
        if (highlightedXmlStr == null)
          highlightedXmlStr = "<content>" + xmlContentTokenized + "</content>";
        xmlStrBuilder.append(highlightedXmlStr);
      }
      xmlStrBuilder.append("</hit>");
    }
    xmlStrBuilder.append("</hits>");
    xmlStrBuilder.append("</document>");
    return xmlStrBuilder.toString();   
  }
  
  private String createHtmlString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, String[] normFunctions, String[] outputOptions, Hits hits, HttpServletRequest request) throws ApplicationException {
    if (hits == null) {
      return ""; 
    }
    String docId = docMetadataRecord.getDocId();
    ArrayList<Document> docs = null;
    if (hits != null)
      docs = hits.getHits();
    int docsSize = 0;
    if (hits != null)
      docsSize = hits.getSize();
    int from = (page * pageSize) - pageSize;  // e.g. 0
    int to = page * pageSize - 1;  // e.g. 9
    if (to >= docsSize)
      to = docsSize - 1;
    StringBuilder xmlStrBuilder = new StringBuilder();
    xmlStrBuilder.append("<html>");
    xmlStrBuilder.append("<head>");
    xmlStrBuilder.append("<title>Document: \"" + query + "\"</title>");
    xmlStrBuilder.append("</head>");
    xmlStrBuilder.append("<body>");
    xmlStrBuilder.append("<table align=\"right\" valign=\"top\">");
    xmlStrBuilder.append("<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW WSP CMS service\"/></a></td>");
    xmlStrBuilder.append("</table>");
    xmlStrBuilder.append("<p/>");
    xmlStrBuilder.append("<h1>Query: " + "\"" + query + "\"" + "</h1>");
    xmlStrBuilder.append("<h3>Document: " + docId + "</h3>");
    xmlStrBuilder.append("<table>");
    for (int i=from; i<=to; i++) {
      xmlStrBuilder.append("<tr valign=\"top\">");
      Document doc = docs.get(i);
      int num = i + 1;
      xmlStrBuilder.append("<td>" + num + ". " + "</td>");
      xmlStrBuilder.append("<td align=\"left\">");
      String posStr = "";
      String pageNumber = "";
      Fieldable fPageNumber = doc.getFieldable("pageNumber");
      if (fPageNumber != null) {
        pageNumber = fPageNumber.stringValue();
        posStr = posStr + "Page " + pageNumber + ", ";
      }
      String elementName = null;
      String presElementName = "";
      Fieldable fElementName = doc.getFieldable("elementName");
      if (fElementName != null) {
        elementName = fElementName.stringValue();
        presElementName = getPresentationName(elementName);
      }
      String elementPagePosition = "";
      Fieldable fElementPagePosition = doc.getFieldable("elementPagePosition");
      if (fElementPagePosition != null) {
        elementPagePosition = fElementPagePosition.stringValue();
        posStr = posStr + presElementName + " " + elementPagePosition + ":";
      }
      String baseUrl = getBaseUrl(request);
      String highlightQueryType = "orig";
      String highlightQueryTypeStr = "";
      String normalizationStr = "";
      if (withLemmas(outputOptions)) {
        highlightQueryTypeStr = "&highlightQueryType=norm";
        highlightQueryType = "norm";
      } else if (normFunctions != null) { 
        String normFunction = normFunctions[0];
        normalizationStr = "&normalization=" + normFunction;
        highlightQueryType = normFunction;
        if (normFunction.equals("none")) {
          normalizationStr = "&normalization=" + "orig";
          highlightQueryType = "orig";
        }
      }
      String language = docMetadataRecord.getLanguage();
      String getPageLink = baseUrl + "/query/GetPage?docId=" + docId + "&page=" + pageNumber + normalizationStr + "&highlightElem=" + elementName + "&highlightElemPos=" + elementPagePosition + highlightQueryTypeStr + "&highlightQuery=" + query;
      xmlStrBuilder.append("<a href=\"" + getPageLink + "\">" + posStr + "</a>");
      String xmlContentTokenized = null;
      Fieldable fXmlContentTokenized = doc.getFieldable("xmlContentTokenized");
      if (fXmlContentTokenized != null) {
        xmlContentTokenized = fXmlContentTokenized.stringValue();
        String highlightedXmlStr = highlight(xmlContentTokenized, highlightQueryType, query, language);  
        String highlightHtmlStr = highlightTransformer.transformStr(highlightedXmlStr);  // TODO performance: do not highlight each single node but highlight them all in one step
        xmlStrBuilder.append("</br>");
        xmlStrBuilder.append(highlightHtmlStr);
      }
      xmlStrBuilder.append("</td>");
      xmlStrBuilder.append("</tr>");
    }
    xmlStrBuilder.append("</table>");
    xmlStrBuilder.append("</body>");
    xmlStrBuilder.append("</html>");
    return xmlStrBuilder.toString();   
  }
  
  private String createJsonString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, String[] normFunctions, String[] outputOptions, Hits hits, HttpServletRequest request) throws ApplicationException {
    if (hits == null) {
      return ""; 
    }
    String docId = docMetadataRecord.getDocId();
    ArrayList<Document> docs = null;
    if (hits != null)
      docs = hits.getHits();
    int docsSize = 0;
    if (hits != null)
      docsSize = hits.getSize();
    int from = (page * pageSize) - pageSize;  // e.g. 0
    int to = page * pageSize - 1;  // e.g. 9
    if (to >= docsSize)
      to = docsSize - 1;
    StringBuilder xmlStrBuilder = new StringBuilder();
    xmlStrBuilder.append("<document>");
    xmlStrBuilder.append("<id>" + docId + "</id>");
    xmlStrBuilder.append("<query>");
    xmlStrBuilder.append("<queryText>" + query + "</queryText>");
    xmlStrBuilder.append("<resultPage>" + page + "</resultPage>");
    xmlStrBuilder.append("<resultPageSize>" + pageSize + "</resultPageSize>");
    xmlStrBuilder.append("</query>");
    xmlStrBuilder.append("<hitsSize>" + docsSize + "</hitsSize>");
    xmlStrBuilder.append("<hits>");
    for (int i=from; i<=to; i++) {
      Document doc = docs.get(i);
      int num = i + 1;
      xmlStrBuilder.append("<hit>");
      xmlStrBuilder.append("<num>" + num + "</num>");
      String pageNumber = null;
      Fieldable fPageNumber = doc.getFieldable("pageNumber");
      if (fPageNumber != null) {
        pageNumber = fPageNumber.stringValue();
        xmlStrBuilder.append("<pageNumber>" + pageNumber + "</pageNumber>");
      }
      String elementPagePosition = null;
      Fieldable fElementPagePosition = doc.getFieldable("elementPagePosition");
      if (fElementPagePosition != null) {
        elementPagePosition = fElementPagePosition.stringValue();
        xmlStrBuilder.append("<pagePosition>" + elementPagePosition + "</pagePosition>");
      }
      String lineNumber = null;
      Fieldable fLineNumber = doc.getFieldable("lineNumber");
      if (fLineNumber != null) {
        lineNumber = fLineNumber.stringValue();
        xmlStrBuilder.append("<lineNumber>" + lineNumber + "</lineNumber>");
      }
      String elementPosition = null;
      Fieldable fElementPosition = doc.getFieldable("elementAbsolutePosition");
      if (fElementPosition != null) {
        elementPosition = fElementPosition.stringValue();
        xmlStrBuilder.append("<absolutePosition>" + elementPosition + "</absolutePosition>");
      }
      String xpath = null;
      Fieldable fXPath = doc.getFieldable("xpath");
      if (fXPath != null) {
        xpath = fXPath.stringValue();
        xmlStrBuilder.append("<xpath>" + xpath + "</xpath>");
      }
      String xmlId = null;
      Fieldable fXmlId = doc.getFieldable("xmlId");
      if (fXmlId != null) {
        xmlId = fXmlId.stringValue();
        xmlStrBuilder.append("<xmlId>" + xmlId + "</xmlId>");
      }
      String language = null;
      Fieldable fLanguage = doc.getFieldable("language");
      if (fLanguage != null) {
        language = fLanguage.stringValue();
        xmlStrBuilder.append("<language>" + language + "</language>");
      }
      String xmlContentTokenized = null;
      Fieldable fXmlContentTokenized = doc.getFieldable("xmlContentTokenized");
      if (fXmlContentTokenized != null) {
        String highlightQueryType = "orig";
        if (withLemmas(outputOptions)) {
          highlightQueryType = "morph";
        } else if (normFunctions != null) { 
          String normFunction = normFunctions[0];
          highlightQueryType = normFunction;
          if (normFunction.equals("none")) {
            highlightQueryType = "orig";
          }
        }
        xmlContentTokenized = fXmlContentTokenized.stringValue();
        String xmlPre = "<content xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
        String xmlPost = "</content>";
        String xmlInputStr = xmlPre + xmlContentTokenized + xmlPost;
        String docLanguage = docMetadataRecord.getLanguage();
        String highlightedXmlStr = highlight(xmlInputStr, highlightQueryType, query, docLanguage);
        if (highlightedXmlStr == null)
          highlightedXmlStr = "<content>" + xmlContentTokenized + "</content>";
        xmlStrBuilder.append(highlightedXmlStr);
      }
      String xmlContent = null;
      Fieldable fXmlContent = doc.getFieldable("xmlContent");
      if (fXmlContent != null) {
        xmlContent = fXmlContent.stringValue();
        xmlStrBuilder.append(xmlContent);
      }
      xmlStrBuilder.append("</hit>");
    }
    xmlStrBuilder.append("</hits>");
    xmlStrBuilder.append("</document>");
    return xmlStrBuilder.toString();   
  }
  
  private String highlight(String xmlStr, String highlightQueryType, String highlightQuery, String language) throws ApplicationException {
    String result = null;
    try {
      HighlightContentHandler highlightContentHandler = new HighlightContentHandler(null, -1, highlightQueryType, highlightQuery, language);
      highlightContentHandler.setFirstPageBreakReachedMode(true);
      XMLReader xmlParser = new SAXParser();
      xmlParser.setContentHandler(highlightContentHandler);
      StringReader stringReader = new StringReader(xmlStr);
      InputSource inputSource = new InputSource(stringReader);
      xmlParser.parse(inputSource);
      result = highlightContentHandler.getResult().toString();
    } catch (SAXException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return result;
  }

  private String getPresentationName(String elemName) {
    String retStr = null;
    if (elemName != null) {
      if (elemName.equals("s")) {
        retStr = "Sentence";
      } else {
        // first char to uppercase
        char[] stringArray = elemName.toCharArray();
        stringArray[0] = Character.toUpperCase(stringArray[0]);
        retStr = new String(stringArray);
      }
    }
    return retStr;
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

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // Auto-generated method stub
  }

  private boolean withLemmas(String[] outputOptions) {
    boolean result = false;
    for (int i=0; i< outputOptions.length; i++) {
      String function = outputOptions[i];
      if (function.equals("withLemmas"))
        return true;
    }
    return result;
  }

}
