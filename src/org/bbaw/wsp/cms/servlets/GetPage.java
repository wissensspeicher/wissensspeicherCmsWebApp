package org.bbaw.wsp.cms.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xerces.internal.parsers.SAXParser;

import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.transform.HighlightContentHandler;
import org.bbaw.wsp.cms.transform.PageTransformer;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.text.tokenize.WordContentHandler;

public class GetPage extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private PageTransformer pageTransformer;

  public GetPage() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    ServletContext context = getServletContext();
    pageTransformer = (PageTransformer) context.getAttribute("pageTransformer");
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String result = "";
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("docId");
    String pageStr = request.getParameter("page");
    String normalization = request.getParameter("normalization");
    if (normalization == null)
      normalization = "norm";
    String highlightQuery = request.getParameter("highlightQuery");
    String highlightQueryType = request.getParameter("highlightQueryType");
    if (highlightQueryType == null)
      highlightQueryType = "form";
    String highlightElem = request.getParameter("highlightElem");
    String highlightElemPosStr = request.getParameter("highlightElemPos");
    int highlightElemPos = -1;
    if (highlightElemPosStr != null)
      highlightElemPos = Integer.parseInt(highlightElemPosStr);
    String mode = request.getParameter("mode");
    if (mode == null)
      mode = "untokenized";
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "html";
    String cssUrl = request.getParameter("cssUrl");
    if (cssUrl == null) {
      String baseUrl = getBaseUrl(request);
      cssUrl = baseUrl + "/css/page.css";
    }
    int page = 1;
    if (pageStr != null)
      page = Integer.parseInt(pageStr);
    if (outputFormat.equals("xml"))
      response.setContentType("text/xml");
    else if (outputFormat.equals("html") || outputFormat.equals("xmlDisplay"))
      response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      DocumentHandler docHandler = new DocumentHandler();
      MetadataRecord mdRecord = indexHandler.getDocMetadata(docId);
      String mimeType = mdRecord.getType();
      if (mimeType == null)
        mimeType = docHandler.getMimeType(docId);
      if (mimeType != null && mimeType.equals("application/pdf")) {
        String title = docId;
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        String head = "<head><title>" + title + "</title><link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/></head>";
        String htmlStr = "<h2>Document page view</h2>";
        htmlStr = htmlStr +	"There is no single page preview for document type: " + mimeType + ". Please download the whole file: ";
        String docUrl = getBaseUrl(request) + "/doc/GetDocument?id=" + docId;
        String webUri = mdRecord.getWebUri();
        if (webUri != null)
          docUrl = webUri;
        htmlStr = htmlStr + "<ul><li><a href=\"" + docUrl + "\">" + docUrl + "</a></li></ul>";
        result = xmlHeader + "<html>" + head + "<body>" + htmlStr + "</body>" + "</html>";
        out.print(result);
        out.close();
        return;
      }
      String docDir = docHandler.getDocDir(docId);
      String docPageDir = docDir + "/" + "pages";
      String pageFileName = docPageDir + "/page-" + page + "-morph.xml";
      File pageFile = new File(pageFileName);
      if (page == 1 && ! (new File(docPageDir)).exists()) {
        String docFileName = docHandler.getDocFullFileName(docId);
        pageFile = new File(docFileName);  // when no page breaks are in the document then the whole document is the first page
      }
      if (! pageFile.exists()) {
        out.print("There is no page: " + page + " in document");
        out.close();
        return;
      }
      String fragment = FileUtils.readFileToString(pageFile, "utf-8");
      if (normalization.equals("norm"))
        fragment = normalizeWords(fragment);
      if (highlightElem != null || highlightQuery != null) {
        String hiQueryType = "orig";
        if (highlightQueryType.equals("morph"))
          hiQueryType = "morph";
        else
          hiQueryType = normalization;
        String language = mdRecord.getLanguage();
        fragment = highlight(fragment, highlightElem, highlightElemPos, hiQueryType, highlightQuery, language);
      }
      if (outputFormat.equals("html") || outputFormat.equals("xmlDisplay")) {
        String schemaName = mdRecord.getSchemaName();
        String title = docId + ", Page: " + page;
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        String head = "<head><title>" + title + "</title><link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/></head>";
        String namespace = "";
        String htmlRenderedPage = pageTransformer.transform(fragment, mdRecord, mode, pageStr, normalization, outputFormat);
        if (schemaName != null && schemaName.equals("echo")) {
          namespace = "xmlns:echo=\"http://www.mpiwg-berlin.mpg.de/ns/echo/1.0/\" xmlns:de=\"http://www.mpiwg-berlin.mpg.de/ns/de/1.0/\" " +
                  "xmlns:dcterms=\"http://purl.org/dc/terms\" " + "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:mml=\"http://www.w3.org/1998/Math/MathML\" " +
                  "xmlns:xlink=\"http://www.w3.org/1999/xlink\"";
        }
        result = xmlHeader + "<html " + namespace + ">" + head + "<body>" + htmlRenderedPage + "</body>" + "</html>";
      } else {
        result = fragment;
      }
      out.print(result);
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
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

  private String normalizeWords(String xmlStr) throws ApplicationException {
    try {
      WordContentHandler wordContentHandler = new WordContentHandler("norm");
      XMLReader xmlParser = new SAXParser();
      xmlParser.setContentHandler(wordContentHandler);
      StringReader strReader = new StringReader(xmlStr);
      InputSource inputSource = new InputSource(strReader);
      xmlParser.parse(inputSource);
      String result = wordContentHandler.getResult();
      return result;
    } catch (SAXException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
  }
  
  private String highlight(String xmlStr, String highlightElem, int highlightElemPos, String highlightQueryType, String highlightQuery, String language) throws ApplicationException {
    String result = null;
    try {
      HighlightContentHandler highlightContentHandler = new HighlightContentHandler(highlightElem, highlightElemPos, highlightQueryType, highlightQuery, language);
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
  
}