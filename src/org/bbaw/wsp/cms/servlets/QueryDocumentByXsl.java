package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.transform.QueryTransformer;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class QueryDocumentByXsl extends HttpServlet {
  private static final long serialVersionUID = 1L;
  public QueryDocumentByXsl() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String srcUrl = request.getParameter("srcUrl");
    String query = request.getParameter("query");
    String flags = request.getParameter("flags");
    String outputFormat = request.getParameter("outputFormat");
    if (flags == null)
      flags = "";
    if (outputFormat == null)
      outputFormat = "xml";
    try {
      QueryTransformer queryTransformer = new QueryTransformer();
      String result = queryTransformer.queryDocument(srcUrl, query, flags, outputFormat);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      out.print(result);
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
