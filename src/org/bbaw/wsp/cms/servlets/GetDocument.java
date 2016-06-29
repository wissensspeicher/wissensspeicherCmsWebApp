package org.bbaw.wsp.cms.servlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.collections.Harvester;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class GetDocument extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private OutputStream out = null;

  public GetDocument() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("id");  
    try {
      Harvester harvester = Harvester.getInstance();
      String fullFileName = harvester.getDocFullFileName(docId);
      File file = new File(fullFileName);
      if (file.exists()) {
        write(response, file);
      } else {
        write(response, "Document: " + docId + " does not exist");
      }
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO 
  }

  private void write(HttpServletResponse response, File file) throws IOException {
    String fileName = file.getName();
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
    String contentType = URLConnection.guessContentTypeFromName(fileName);  // other methods: URLConnection.guessContentTypeFromStream(is); or MIMEUtils.getMIMEType(file);
    if (contentType != null)
      response.setContentType(contentType);
    response.setHeader("Content-Disposition", "filename=" + fileName);
    out = response.getOutputStream();
    byte[] buf = new byte[20000*1024]; // 20MB buffer
    int bytesRead;
    while ((bytesRead = is.read(buf)) != -1) {
      out.write(buf, 0, bytesRead);
    }
    is.close();
    out.flush();
  }

  private void write(HttpServletResponse response, String str) throws IOException {
    out = response.getOutputStream();
    byte[] strBytes = str.getBytes("utf-8");
    out.write(strBytes, 0, strBytes.length);
    out.flush();
  }
}
