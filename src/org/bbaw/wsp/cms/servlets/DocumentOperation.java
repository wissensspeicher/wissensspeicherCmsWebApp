package org.bbaw.wsp.cms.servlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.document.DocumentHandler;
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
    String srcUrlStr = request.getParameter("srcUrl");
    String docId = request.getParameter("docId");  // id in file system or version management system: e.g. /tei/en/Test_1789.xml
    String mainLanguage = request.getParameter("mainLanguage");  // main language for that document
    String elementNames = request.getParameter("elementNames");  // id in file system or version management system: e.g. /tei/en/Test_1789.xml
    String operation = request.getParameter("operation");
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    if (outputFormat.equals("xml")) {
      response.setContentType("text/xml");
    } else { 
      response.setContentType("text/html");
    }
    CmsDocOperation docOperation = new CmsDocOperation(operation, srcUrlStr, null, docId); 
    if (mainLanguage != null)
      docOperation.setMainLanguage(mainLanguage);
    String[] elementNamesArray = null;
    if (elementNames != null)
      elementNamesArray = elementNames.split(" ");
    docOperation.setElementNames(elementNamesArray);
    try {
      if (docId == null || docId.isEmpty()) {
        write(response, "Parameter: \"docId\" is not set. Please set parameter \"docId\".");
        return;
      }
      if (operation.equals("get")) {
        DocumentHandler docHandler = new DocumentHandler();
        String docFileName = docHandler.getDocFullFileName(docId);
        File docFile = new File(docFileName);
        if (docFile.exists())
          write(response, docFile);
        else
          write(response, "Document: " + docId + " does not exist");
      } else if (operation.equals("create") || operation.equals("delete")) {
        CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
        docOperation = scheduler.doOperation(docOperation);
        String jobId = "" + docOperation.getOrderId();
        String baseUrl = ServletUtil.getInstance().getBaseUrl(request);
        String docJobUrlStr = baseUrl + "/doc/GetDocumentJobs?id=" + jobId;
        if (outputFormat.equals("xml")) {
          write(response, "<result>");
          write(response, "<docJob>");
          write(response, "<id>" + jobId + "</id>");
          write(response, "<url>" + docJobUrlStr + "</url>");
          write(response, "</docJob>");
          write(response, "</result>");
        } else { 
          write(response, "<html>");
          write(response, "<h2>" + "Document operation result" + "</h2>");
          write(response, "See your document operation " + jobId + " <a href=\"" + docJobUrlStr + "\">" + "here" + "</a>");
          write(response, "<html>");
        }
      } else {
        String errorStr = "Error: Operation: " + operation + " is not supported";
        if (outputFormat.equals("xml")) {
          write(response, "<error>" + errorStr + "</error>");
        } else { 
          write(response, "<html>");
          write(response, "<h2>" + "Error" + "</h2>");
          write(response, errorStr);
        }
      }
      PrintWriter out = response.getWriter();
      out.close();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

  private void write(HttpServletResponse response, File file) throws IOException {
    String fileName = file.getName();
    OutputStream out = response.getOutputStream();
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
    String contentType = URLConnection.guessContentTypeFromName(fileName);  // other methods: URLConnection.guessContentTypeFromStream(is); or MIMEUtils.getMIMEType(file);
    if (contentType != null)
      response.setContentType(contentType);
    response.setHeader("Content-Disposition", "filename=" + fileName);
    byte[] buf = new byte[20000*1024]; // 20MB buffer
    int bytesRead;
    while ((bytesRead = is.read(buf)) != -1) {
      out.write(buf, 0, bytesRead);
    }
    is.close();
    out.flush();
  }

  private void write(HttpServletResponse response, String str) throws IOException {
    PrintWriter out = response.getWriter();
    out.write(str);
  }
  
}
