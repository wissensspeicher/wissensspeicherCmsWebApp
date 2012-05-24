package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.Util;

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
      else if (outputFormat.equals("html"))
        response.setContentType("text/html");
      else 
        response.setContentType("text/xml");
      PrintWriter out = response.getWriter();
      if (mdRecord != null) {
        out.print("<doc>");
        out.print("<id>" + docId + "</id>");
        String echoId = mdRecord.getEchoId();
        if (echoId != null)
          out.print("<echoId>" + echoId + "</echoId>");
        String author = mdRecord.getCreator();
        if (author != null)
          out.print("<author>" + author + "</author>");
        String title = mdRecord.getTitle();
        if (title != null)
          out.print("<title>" + title + "</title>");
        String language = mdRecord.getLanguage();
        if (language != null)
          out.print("<language>" + language + "</language>");
        String date = mdRecord.getYear();
        if (date != null)
          out.print("<date>" + date + "</date>");
        String rights = mdRecord.getRights();
        if (rights != null)
          out.print("<rights>" + rights + "</rights>");
        String license = mdRecord.getLicense();
        if (license != null)
          out.print("<license>" + license + "</license>");
        String accessRights = mdRecord.getAccessRights();
        if (accessRights != null)
          out.print("<accessRights>" + accessRights + "</accessRights>");
        int pageCount = mdRecord.getPageCount();
        out.print("<countPages>" + pageCount + "</countPages>");
        Date lastModified = mdRecord.getLastModified();
        if (lastModified != null) {
          String lastModifiedStr = new Util().toXsDate(lastModified);
          out.print("<lastModified>" + lastModifiedStr + "</lastModified>");
        }
        String schemaName = mdRecord.getSchemaName();
        if (schemaName != null)
          out.print("<schema>" + schemaName + "</schema>");
        out.print("</doc>");
        out.close();
      } else {
        out.print("<result>" + "no document found with id: " + docId + "</result>");
      }
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
