package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class GetDocInfo extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public GetDocInfo() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("docId");
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      MetadataRecord mdRecord = indexHandler.getDocMetadata(docId);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html") || outputFormat.equals("json"))
        response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      if (mdRecord != null && outputFormat.equals("xml")) {
        out.print(mdRecord.toXmlStr());
      } else if (mdRecord != null && outputFormat.equals("json")) {
        // TODO
        out.print(mdRecord.toXmlStr());
      } else {
        out.print("<result>" + "no resource found with id: " + docId + "</result>");
      }
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
