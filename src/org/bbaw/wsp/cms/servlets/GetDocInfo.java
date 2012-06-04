package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;

import org.bbaw.wsp.cms.document.DocumentHandler;
import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;
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
    String field = request.getParameter("field");
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
        out.print("<doc>");
        out.print("<id>" + docId + "</id>");
        String uri = mdRecord.getUri();
        if ((field == null || (field != null && field.equals("uri"))) && uri != null)
          out.print("<uri>" + uri + "</uri>");
        String author = mdRecord.getCreator();
        if ((field == null || (field != null && field.equals("author"))) && author != null)
          out.print("<author>" + author + "</author>");
        String title = mdRecord.getTitle();
        if ((field == null || (field != null && field.equals("title"))) && title != null)
          out.print("<title>" + title + "</title>");
        String language = mdRecord.getLanguage();
        if ((field == null || (field != null && field.equals("language"))) && language != null)
          out.print("<language>" + language + "</language>");
        String date = mdRecord.getYear();
        if ((field == null || (field != null && field.equals("date"))) && date != null)
          out.print("<date>" + date + "</date>");
        String rights = mdRecord.getRights();
        if ((field == null || (field != null && field.equals("rights"))) && rights != null)
          out.print("<rights>" + rights + "</rights>");
        String license = mdRecord.getLicense();
        if ((field == null || (field != null && field.equals("license"))) && license != null)
          out.print("<license>" + license + "</license>");
        String accessRights = mdRecord.getAccessRights();
        if ((field == null || (field != null && field.equals("accessRights"))) && accessRights != null)
          out.print("<accessRights>" + accessRights + "</accessRights>");
        String echoId = mdRecord.getEchoId();
        if ((field == null || (field != null && field.equals("echoId"))) && echoId != null)
          out.print("<echoId>" + echoId + "</echoId>");
        if (field == null || (field != null && ! field.equals("toc") && ! field.equals("figures") && ! field.equals("handwritten") && ! field.equals("pages")))
          out.print("<system>");
        int pageCount = mdRecord.getPageCount();
        if (field == null || (field != null && field.equals("countPages")))
          out.print("<countPages>" + pageCount + "</countPages>");
        Date lastModified = mdRecord.getLastModified();
        if ((field == null || (field != null && field.equals("lastModified"))) && lastModified != null) {
          String lastModifiedStr = new Util().toXsDate(lastModified);
          out.print("<lastModified>" + lastModifiedStr + "</lastModified>");
        }
        String schemaName = mdRecord.getSchemaName();
        if ((field == null || (field != null && field.equals("schema"))) && schemaName != null)
          out.print("<schema>" + schemaName + "</schema>");
        if (field == null || (field != null && ! field.equals("toc") && ! field.equals("figures") && ! field.equals("handwritten") && ! field.equals("pages")))
          out.print("</system>");
        if (field != null && (field.equals("toc") || field.equals("figures") || field.equals("handwritten") || field.equals("pages"))) { 
          XslResourceTransformer tocTransformer = new XslResourceTransformer("tocOut.xsl");
          DocumentHandler docHandler = new DocumentHandler();
          String docDir = docHandler.getDocDir(docId);
          String tocFileName = docDir + "/toc.xml";
          QName typeQName = new QName("type");
          XdmValue typeXdmValue = new XdmAtomicValue(field);
          tocTransformer.setParameter(typeQName, typeXdmValue);
          String tocXmlStr = tocTransformer.transform(tocFileName);
          out.print(tocXmlStr);
        }
        out.print("</doc>");
      } else if (mdRecord != null && outputFormat.equals("json")) {
        // TODO
        
      } else {
        out.print("<result>" + "no document found with id: " + docId + "</result>");
      }
      out.close();
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }
}
