package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;

import org.bbaw.wsp.cms.lucene.IndexHandler;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

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
      ArrayList<Document> docs = indexHandler.queryDocuments(query, language);
      int docsSize = 0;
      if (docs != null)
        docsSize = docs.size();
      if (to >= docsSize)
        to = docsSize - 1;
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      out.print("<result>");
      out.print("<query>");
      out.print("<queryText>" + query + "</queryText>");
      out.print("<resultPage>" + page + "</resultPage>");
      out.print("<resultPageSize>" + pageSize + "</resultPageSize>");
      out.print("</query>");
      out.print("<hitsSize>" + docsSize + "</hitsSize>");
      out.print("<hits>");
      for (int i=from; i<=to; i++) {
        Document doc = docs.get(i);
        String docId = doc.getFieldable("docId").stringValue();
        out.print("<doc>" + docId + "</doc>");
      }
      out.print("</hits>");
      out.print("</result>");
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
