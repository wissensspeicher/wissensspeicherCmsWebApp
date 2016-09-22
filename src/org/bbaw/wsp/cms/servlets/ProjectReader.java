package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.collections.Project;
import org.json.simple.JSONArray;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectReader extends HttpServlet {
  private static final long serialVersionUID = 4711L;

  public ProjectReader() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "json";
    if (outputFormat.equals("xml"))
      response.setContentType("text/xml");
    else if (outputFormat.equals("html"))
      response.setContentType("text/html");
    else if (outputFormat.equals("json"))
      response.setContentType("application/json");
    else 
      response.setContentType("application/json");
    String operation = request.getParameter("operation");  
    PrintWriter out = response.getWriter();
    if (operation == null) {
      String errorStr = "Error: required parameter \"operation\" has no value";
      out.write("<error>" + errorStr + "</error>");
      return;
    }
    try {
      org.bbaw.wsp.cms.collections.ProjectReader projectReader = org.bbaw.wsp.cms.collections.ProjectReader.getInstance();
      if (operation.equals("getProjects")) {
        String sortBy = request.getParameter("sortBy"); 
        if (sortBy == null) {
          ArrayList<Project> projects = projectReader.getProjects();
          String jsonProjectsStr = toJsonString(projects);
          out.println(jsonProjectsStr);
          return;
        }
      } else if (operation.equals("getProjectsByProjectType")) {
        String projectType = request.getParameter("projectType"); 
        ArrayList<Project> projects = projectReader.getProjectsByProjectType(projectType);
        String jsonProjectsStr = toJsonString(projects);
        out.println(jsonProjectsStr);
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private String toJsonString(ArrayList<Project> projects) throws ApplicationException {
    JSONArray jsonProjects = new JSONArray();
    for (int i=0; i<projects.size(); i++) {
       Project project = projects.get(i);
       jsonProjects.add(project.toJsonObject());
    }
    String jsonProjectsStr = jsonProjects.toJSONString();
    return jsonProjectsStr;
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
