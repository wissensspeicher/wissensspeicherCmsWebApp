package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.scheduler.CmsChainScheduler;
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;
import org.bbaw.wsp.cms.servlets.util.ServletUtil;

public class DocumentOperation extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public DocumentOperation() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String operation = request.getParameter("operation");  // create, delete, updateCollection
    String collectionId = request.getParameter("collectionId");  // e.g. mega
    String srcUrlStr = request.getParameter("srcUrl");
    String docId = request.getParameter("docId");  // id in file system or version management system: e.g. /tei/en/Test_1789.xml
    String mainLanguage = request.getParameter("mainLanguage");  // main language for that document
    String elementNames = request.getParameter("elementNames");  // id in file system or version management system: e.g. /tei/en/Test_1789.xml
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    if (outputFormat.equals("xml")) {
      response.setContentType("text/xml");
    } else { 
      response.setContentType("text/html");
    }
    PrintWriter out = response.getWriter();
    CmsDocOperation docOperation = new CmsDocOperation(operation, srcUrlStr, null, docId); 
    if (mainLanguage != null)
      docOperation.setMainLanguage(mainLanguage);
    if (collectionId != null)
      docOperation.setCollectionId(collectionId);
    String[] elementNamesArray = null;
    if (elementNames != null)
      elementNamesArray = elementNames.split(" ");
    docOperation.setElementNames(elementNamesArray);
    try {
      if (operation.equals("create") || operation.equals("delete") || operation.equals("updateCollection")) {
        CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
        docOperation = scheduler.doOperation(docOperation);
        String jobId = "" + docOperation.getOrderId();
        String baseUrl = ServletUtil.getInstance().getBaseUrl(request);
        String docJobUrlStr = baseUrl + "/doc/GetDocumentJobs?id=" + jobId;
        if (outputFormat.equals("xml")) {
          out.write("<result>");
          out.write("<docJob>");
          out.write("<id>" + jobId + "</id>");
          out.write("<url>" + docJobUrlStr + "</url>");
          out.write("</docJob>");
          out.write("</result>");
        } else { 
          out.write("<html>");
          out.write("<h2>" + "Document operation result" + "</h2>");
          out.write("See your document operation " + jobId + " <a href=\"" + docJobUrlStr + "\">" + "here" + "</a>");
          out.write("<html>");
        }
      } else {
        String errorStr = "Error: Operation: " + operation + " is not supported";
        if (outputFormat.equals("xml")) {
          out.write("<error>" + errorStr + "</error>");
        } else { 
          out.write("<html>");
          out.write("<h2>" + "Error" + "</h2>");
          out.write(errorStr);
        }
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
