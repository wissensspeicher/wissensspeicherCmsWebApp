package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.index.Term;

import org.bbaw.wsp.cms.lucene.IndexHandler;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class GetToken extends HttpServlet {
  private static final long serialVersionUID = 1L;
  public GetToken() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("docId");
    String attribute = request.getParameter("attribute");
    if (attribute == null)
      attribute = "tokenOrig";
    String query = request.getParameter("query");
    String countStr = request.getParameter("count");
    if (countStr == null)
      countStr = "100";
    int count = Integer.parseInt(countStr);
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      ArrayList<Term> terms = null;
      if (docId == null)
        terms = indexHandler.getTerms(attribute, query, count);
      else
        terms = indexHandler.getTerms(docId, attribute, query, count);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      out.print("<result>");
      out.print("<attribute>" + attribute + "</attribute>");
      out.print("<query>" + query + "</query>");
      out.print("<count>" + count + "</count>");
      out.print("<result>");
      if (terms != null) {
        for (int i=0; i<terms.size(); i++) {
          Term term = terms.get(i);
          out.print("<token>" + term.text() + "</token>");
        }
      }
      out.print("</result>");
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
