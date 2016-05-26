package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.bbaw.wsp.cms.collections.Collection;
import org.bbaw.wsp.cms.collections.CollectionReader;

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
    String[] projectIds = request.getParameterValues("projectId"); 
    String projectId2 = request.getParameter("projectId2");
    response.setContentType("text/xml");
    PrintWriter out = response.getWriter();
    try {
      String outputXmlStr = "";
      org.bbaw.wsp.cms.collections.ProjectManager pm = org.bbaw.wsp.cms.collections.ProjectManager.getInstance();
      if (operation.equals("update") || operation.equals("harvest") || operation.equals("annotate") || operation.equals("index")) {
        if (uid == null || pw == null || (! uid.equals("wsp4711") && ! pw.equals("blabla4711"))) {
          out.write("<error>" + "incorrect userId or password" + "</error>");
          return;
        }
        if (operation.equals("update")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("harvest")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("annotate")) {
          outputXmlStr = "ToDo"; // TODO
        } else if (operation.equals("index")) {
          outputXmlStr = "ToDo"; // TODO
        }
      } else if (operation.equals("status")) {
        ArrayList<Collection> projects = null;
        if (projectIds != null && projectId2 == null) {
          projects = CollectionReader.getInstance().getCollections(projectIds);
        } else if (projectIds != null && projectIds.length == 1 && projectId2 != null) {
          projects = CollectionReader.getInstance().getCollections(projectIds[0], projectId2);
        } 
        String[] projectIdsTmp = new String[projects.size()];
        for (int i=0; i<projects.size(); i++) {
          projectIdsTmp[i] = projects.get(i).getId();
        }
        outputXmlStr = pm.getStatusProjectXmlStr(projectIdsTmp);
      } else {
        String errorStr = "Error: Operation: " + operation + " is not supported";
        out.write("<error>" + errorStr + "</error>");
        return;
      }
      out.write("<result>\n");
      out.write("<operation>" + operation + "</operation>\n");
      String projectIdsStr = StringUtils.join(projectIds, ",");
      out.write("<projectId>" + projectIdsStr + "</projectId>\n");
      if (projectId2 != null)
        out.write("<projectId2>" + projectId2 + "</projectId2>\n");
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
