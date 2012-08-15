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

import org.bbaw.wsp.cms.document.DocumentHandler;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
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
      Hits hits = xQueryEvaluator.evaluate(docFileUrl, query, from, to);
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
      if (outputFormat.equals("xml"))
        resultStr = createXmlString(docMetadataRecord, query, page, pageSize, hits);
      else if (outputFormat.equals("html"))
        resultStr = createHtmlString(docMetadataRecord, query, page, pageSize, hits);
      out.print(resultStr);
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  private String createXmlString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, Hits hits) throws ApplicationException {
    String docId = docMetadataRecord.getDocId();
    ArrayList<String> xmlStrHits = null;
    if (hits != null)
      xmlStrHits = hits.getHits();
    int hitsSize = -1;
    int xmlStrHitsSize = -1;
    if (hits != null)
      hitsSize = hits.getSize();
    if (xmlStrHits != null)
      xmlStrHitsSize = xmlStrHits.size();
    StringBuilder xmlStrBuilder = new StringBuilder();
    xmlStrBuilder.append("<document>");
    xmlStrBuilder.append("<id>" + docId + "</id>");
    xmlStrBuilder.append("<query>");
    xmlStrBuilder.append("<queryText>" + query + "</queryText>");
    xmlStrBuilder.append("<resultPage>" + page + "</resultPage>");
    xmlStrBuilder.append("<resultPageSize>" + pageSize + "</resultPageSize>");
    xmlStrBuilder.append("</query>");
    xmlStrBuilder.append("<hitsSize>" + hitsSize + "</hitsSize>");
    xmlStrBuilder.append("<hits>");
    for (int i=0; i<xmlStrHitsSize; i++) {
      xmlStrBuilder.append("<hit>");
      String xmlStrHit = xmlStrHits.get(i);
      xmlStrBuilder.append(xmlStrHit);
      xmlStrBuilder.append("</hit>");
    }
    xmlStrBuilder.append("</hits>");
    xmlStrBuilder.append("</document>");
    return xmlStrBuilder.toString();   
  }
  
  private String createHtmlString(MetadataRecord docMetadataRecord, String query, int page, int pageSize, Hits hits) throws ApplicationException {
    String docId = docMetadataRecord.getDocId();
    ArrayList<String> xmlStrHits = null;
    if (hits != null)
      xmlStrHits = hits.getHits();
    int xmlStrHitsSize = -1;
    if (xmlStrHits != null)
      xmlStrHitsSize = xmlStrHits.size();
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
    xmlStrBuilder.append("<h1>Query: " + "\"" + query + "</h1>");
    xmlStrBuilder.append("<h3>Document: " + docId + "</h3>");
    xmlStrBuilder.append("<table>");
    for (int i=0; i<xmlStrHitsSize; i++) {
      xmlStrBuilder.append("<tr valign=\"top\">");
      String xmlStrHit = xmlStrHits.get(i);
      int num = (page - 1) * pageSize + i + 1;
      xmlStrBuilder.append("<td>" + num + ". " + "</td>");
      xmlStrBuilder.append("<td align=\"left\">");
      xmlStrBuilder.append(xmlStrHit);
      xmlStrBuilder.append("</td>");
      xmlStrBuilder.append("</tr>");
    }
    xmlStrBuilder.append("</table>");
    xmlStrBuilder.append("</body>");
    xmlStrBuilder.append("</html>");
    return xmlStrBuilder.toString();   
  }
}
