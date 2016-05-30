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
    String uid = request.getParameter("uid");  
    String pw = request.getParameter("pw");  
    String operation = request.getParameter("operation");  
    String projects = request.getParameter("projects"); 
    response.setContentType("text/xml");
    PrintWriter out = response.getWriter();
    if (operation == null) {
      String errorStr = "Error: required parameter \"operation\" has no value";
      out.write("<error>" + errorStr + "</error>");
      return;
    }
    if (projects == null) {
      String errorStr = "Error: required parameter \"projects\" has no value";
      out.write("<error>" + errorStr + "</error>");
      return;
    }
    String[] projectIds = null;
    String projectFrom = null;
    String projectTo = null;
    if (projects != null) {
      if (projects.contains("-")) {
        projectFrom = projects.substring(0, projects.indexOf("-"));
        projectTo = projects.substring(projects.indexOf("-") + 1, projects.length());
      } else if (projects.contains(",")) {
        projectIds = projects.split(",");
      } else {
        projectIds = new String[1];
        projectIds[0] = projects;
      }
    }
    try {
      String outputXmlStr = "";
      org.bbaw.wsp.cms.collections.ProjectManager pm = org.bbaw.wsp.cms.collections.ProjectManager.getInstance();
      if (operation.equals("update") || operation.equals("harvest") || operation.equals("annotate") || operation.equals("index") || operation.equals("delete")) {
        if (uid == null || pw == null || (! uid.equals("wsp4711") && ! pw.equals("blabla4711"))) {
          out.write("<error>" + "incorrect uid or pw" + "</error>");
          return;
        }
        if (operation.equals("update")) {
          // TODO Operation ans Job-System übergeben
          if (projectIds != null) { 
            pm.update(projectIds);  
          } else if (projectFrom != null && projectTo != null) {
            pm.update(projectFrom, projectTo);
          }
          outputXmlStr = "<message>operation is sent to operation queue</message>\n"; // TODO
        } else if (operation.equals("harvest")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("annotate")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("index")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("delete")) {
          outputXmlStr = "ToDo"; // TODO
        }
      } else if (operation.equals("status")) {
        if (projectIds != null) {
          outputXmlStr = pm.getStatusProjectXmlStr(projectIds);
        } else if (projectFrom != null && projectTo != null) {
          outputXmlStr = pm.getStatusProjectXmlStr(projectFrom, projectTo);
        }
      } else {
        String errorStr = "Error: Operation: " + operation + " is not supported";
        out.write("<error>" + errorStr + "</error>");
        return;
      }
      out.write("<result>\n");
      out.write("<operation>" + operation + "</operation>\n");
      out.write("<projects>" + projects + "</projects>\n");
      out.write("<operationResult>");
      out.write(outputXmlStr);
      out.write("</operationResult>");
      out.write("</result>\n");
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
