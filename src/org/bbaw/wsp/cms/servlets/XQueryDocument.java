package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.dochandler.DocumentHandler;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.Hit;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.Hits;

public class XQueryDocument extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private XQueryEvaluator xQueryEvaluator = null;
  
  public XQueryDocument() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    ServletContext context = getServletContext();
    xQueryEvaluator = (XQueryEvaluator) context.getAttribute("xQueryEvaluator");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }  

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("docId");
    String query = request.getParameter("query");
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
      DocumentHandler docHandler = new DocumentHandler();
      String docFileName = docHandler.getDocFullFileName(docId);
      URL docFileUrl = new URL("file:" + docFileName);
      Hits hits = null;
      String errorStr = null;
      try {
        hits = xQueryEvaluator.evaluate(docFileUrl, query, from, to);
      } catch (ApplicationException e) {
        errorStr = e.getLocalizedMessage();
      }
      IndexHandler indexHandler = IndexHandler.getInstance();
      MetadataRecord docMetadataRecord = indexHandler.getDocMetadata(docId);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      String resultStr = "";
      if (errorStr == null) {
        if (outputFormat.equals("xml"))
          resultStr = createXmlString(docMetadataRecord, query, page, pageSize, hits);
        else if (outputFormat.equals("html"))
          resultStr = createHtmlString(docMetadataRecord, query, page, pageSize, hits, request);
      } else {
        resultStr = "Saxon XQuery Error: " + errorStr;
      }
      out.print(resultStr);
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  private String createXmlString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, Hits hits) throws ApplicationException {
    String docId = docMetadataRecord.getDocId();
    ArrayList<Hit> hitsArray = null;
    if (hits != null)
      hitsArray = hits.getHits();
    int hitsSize = -1;
    int hitsArraySize = -1;
    if (hits != null)
      hitsSize = hits.getSize();
    if (hitsArray != null)
      hitsArraySize = hitsArray.size();
    StringBuilder xmlStrBuilder = new StringBuilder();
    xmlStrBuilder.append("<document>");
    xmlStrBuilder.append("<id>" + docId + "</id>");
    xmlStrBuilder.append("<query>");
    String queryXml = StringUtils.deresolveXmlEntities(query);
    xmlStrBuilder.append("<queryText>" + queryXml + "</queryText>");
    xmlStrBuilder.append("<resultPage>" + page + "</resultPage>");
    xmlStrBuilder.append("<resultPageSize>" + pageSize + "</resultPageSize>");
    xmlStrBuilder.append("</query>");
    xmlStrBuilder.append("<hitsSize>" + hitsSize + "</hitsSize>");
    xmlStrBuilder.append("<hits>");
    for (int i=0; i<hitsArraySize; i++) {
      int num = (page - 1) * pageSize + i + 1;
      xmlStrBuilder.append("<hit n=\"" + num + "\">");
      Hit hit = hitsArray.get(i);
      String name = hit.getName();
      String typeStr = "ELEMENT";
      int type = hit.getType();
      if (type == Hit.TYPE_ATTRIBUTE)
        typeStr = "ATTRIBUTE";
      else if (type == Hit.TYPE_ATOMIC_VALUE)
        typeStr = "ATOMIV_VALUE";
      int docPage = hit.getPage();
      int hitPagePosition = hit.getHitPagePosition();
      String xmlContent = hit.getContent();
      if (name != null)
        xmlStrBuilder.append("<name>" + name + "</name>");
      xmlStrBuilder.append("<type>" + typeStr + "</type>");
      if (docPage != -1) {
        xmlStrBuilder.append("<page>" + docPage + "</page>");
        xmlStrBuilder.append("<posInPage>" + hitPagePosition + "</posInPage>");
      }
      xmlStrBuilder.append("<content>" + xmlContent + "</content>");
      xmlStrBuilder.append("</hit>");
    }
    xmlStrBuilder.append("</hits>");
    xmlStrBuilder.append("</document>");
    return xmlStrBuilder.toString();   
  }
  
  private String createHtmlString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, Hits hits, HttpServletRequest request) throws ApplicationException {
    String docId = docMetadataRecord.getDocId();
    ArrayList<Hit> hitsArray = null;
    if (hits != null)
      hitsArray = hits.getHits();
    int hitsArraySize = -1;
    if (hitsArray != null)
      hitsArraySize = hitsArray.size();
    int hitsSize = hits.getSize();
    int from = (page * pageSize) - pageSize;  // e.g. 0
    int to = page * pageSize - 1;  // e.g. 9
    int fromDisplay = from + 1;
    int toDisplay = to + 1;
    if (hitsSize < to)
      toDisplay = hitsSize;
    StringBuilder xmlStrBuilder = new StringBuilder();
    xmlStrBuilder.append("<html>");
    xmlStrBuilder.append("<head>");
    xmlStrBuilder.append("<title>Document: \"" + query + "\"</title>");
    xmlStrBuilder.append("</head>");
    xmlStrBuilder.append("<body>");
    xmlStrBuilder.append("<table align=\"right\" valign=\"top\">");
    xmlStrBuilder.append("<td>[<i>This is a MPIWG CMS technology service</i>] <a href=\"/mpiwg-mpdl-cms-web/index.html\"><img src=\"/mpiwg-mpdl-cms-web/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"MPIWG CMS service\"/></a></td>");
    xmlStrBuilder.append("</table>");
    xmlStrBuilder.append("<p/>");
    xmlStrBuilder.append("<h3>XQuery (in document: " + docId + "):</h3>");
    xmlStrBuilder.append(query);
    xmlStrBuilder.append("<h3>Result: (" + fromDisplay + " - " + toDisplay + " of " + hitsSize + " hits)</h3>");
    xmlStrBuilder.append("<table>");
    if (hitsSize == 1 && hitsArray.get(0).getType() == Hit.TYPE_ATOMIC_VALUE) {
      Hit hit = hitsArray.get(0);
      String xmlContent = hit.getContent();
      xmlContent = StringUtils.deresolveXmlEntities(xmlContent);
      xmlStrBuilder.append(xmlContent);
    } else {
      for (int i=0; i<hitsArraySize; i++) {
        xmlStrBuilder.append("<tr valign=\"top\">");
        Hit hit = hitsArray.get(i);
        int docPage = hit.getPage();
        String hitName = hit.getName();
        int hitType = hit.getType();
        int hitPagePosition = hit.getHitPagePosition();
        String baseUrl = getBaseUrl(request);
        String getPageLink = baseUrl + "/query/GetPage?docId=" + docId + "&page=" + docPage + "&outputFormat=" + "xmlDisplay" + "&highlightElem=" + hitName + "&highlightElemPos=" + hitPagePosition;
        String hitPres = hitName + "[" + hitPagePosition + "]";
        if (hitType == Hit.TYPE_ATTRIBUTE) {
          hitPres = "@" + hitName;
          getPageLink = baseUrl + "/query/GetPage?docId=" + docId + "&page=" + docPage;
        }
        String posStr = "Page " + docPage + ", " + hitPres + ":";
        int num = (page - 1) * pageSize + i + 1;
        xmlStrBuilder.append("<td>" + num + ". " + "</td>");
        xmlStrBuilder.append("<td align=\"left\">");
        if (docPage != -1) {
          xmlStrBuilder.append("<a href=\"" + getPageLink + "\">" + posStr + "</a>");
          xmlStrBuilder.append("</br>");
        }
        String xmlContent = hit.getContent();
        xmlContent = StringUtils.deresolveXmlEntities(xmlContent);
        xmlStrBuilder.append(xmlContent);
        xmlStrBuilder.append("</td>");
        xmlStrBuilder.append("</tr>");
      }
    }
    xmlStrBuilder.append("</table>");
    xmlStrBuilder.append("</body>");
    xmlStrBuilder.append("</html>");
    return xmlStrBuilder.toString();   
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
