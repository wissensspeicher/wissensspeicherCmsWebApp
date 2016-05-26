package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProjectManager extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public ProjectManager() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String operation = request.getParameter("operation");  
    String projectId = request.getParameter("projectId"); 
    String projectId2 = request.getParameter("projectId2");
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    if (outputFormat.equals("xml")) {
      response.setContentType("text/xml");
    } else { 
      response.setContentType("text/html");
    }
    PrintWriter out = response.getWriter();
    try {
      if (operation.equals("update") || operation.equals("harvest") || operation.equals("annotate") || operation.equals("index") || operation.equals("status")) {
        org.bbaw.wsp.cms.collections.ProjectManager pm = org.bbaw.wsp.cms.collections.ProjectManager.getInstance();
        String outputXmlStr = "";
        if (operation.equals("status")) {
          outputXmlStr = pm.getStatusProjectXmlStr(projectId);
        } else if (operation.equals("update")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("harvest")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("annotate")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("index")) {
          outputXmlStr = "ToDo"; // TODO
        }
        if (outputFormat.equals("xml")) {
          out.write("<result>\n");
          out.write("<operation>" + operation + "</operation>\n");
          out.write("<projectId>" + projectId + "</projectId>\n");
          if (projectId2 != null)
            out.write("<projectId2>" + projectId2 + "</projectId2>\n");
          out.write("<operationResult>");
          out.write(outputXmlStr);
          out.write("</operationResult>");
          out.write("</result>\n");
        } else {
          String statusUrlStr = "update/ProjectManager?operation=status&projectId=" + projectId;
          if (projectId2 != null)
            statusUrlStr = "update/ProjectManager?operation=status&projectId=" + projectId + "&projectId2=" + projectId2;
          out.write("<html>");
          out.write("<h2>" + "Result" + "</h2>");
          out.write("See status of your operation: " + operation + " <a href=\"" + statusUrlStr + "\">" + "here" + "</a>");
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
    doGet(request, response);
  }

}
