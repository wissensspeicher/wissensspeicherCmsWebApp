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

import org.bbaw.wsp.cms.document.Token;
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
      ArrayList<Token> token = null;
      if (docId == null)
        token = indexHandler.getToken(attribute, query, count);
      else
        token = indexHandler.getToken(docId, attribute, query, count);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      out.print("<result>");
      out.print("<attribute>" + attribute + "</attribute>");
      if (query != null)
        out.print("<query>" + query + "</query>");
      out.print("<count>" + count + "</count>");
      out.print("<result>");
      if (token != null) {
        for (int i=0; i<token.size(); i++) {
          Token t = token.get(i);
          Term term = t.getTerm();
          int freq = t.getFreq();
          out.print("<token>");
          out.print("<text>" + term.text() + "</text>");
          if (freq != -1)
            out.print("<freq>" + freq + "</freq>");
          out.print("</token>");
        }
      }
      out.print("</result>");
      out.print("</result>");
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
