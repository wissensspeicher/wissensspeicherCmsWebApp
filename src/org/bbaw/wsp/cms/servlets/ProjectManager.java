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
import org.bbaw.wsp.cms.collections.ProjectReader;
import org.bbaw.wsp.cms.scheduler.CmsChainScheduler;
import org.bbaw.wsp.cms.scheduler.CmsOperation;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ProjectManager extends HttpServlet {
  private static final long serialVersionUID = 4711L;

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
    String jobId = request.getParameter("jobId"); 
    response.setContentType("text/xml");
    PrintWriter out = response.getWriter();
    if (operation == null) {
      String errorStr = "Error: required parameter \"operation\" has no value";
      out.write("<error>" + errorStr + "</error>");
      return;
    }
    if ((operation.equals("update") || operation.equals("harvest") || operation.equals("annotate") || operation.equals("index") || 
        operation.equals("delete")) && projects == null) {
      String errorStr = "Error: required parameter \"projects\" has no value";
      out.write("<error>" + errorStr + "</error>");
      return;
    }
    try {
      String updateCycleProjects = null;
      if (operation.equals("updateCycle") || operation.equals("status")) {
        updateCycleProjects = getUpdateCycleProjectsIdsStr();
      }
      if (operation.equals("update") || operation.equals("harvest") || operation.equals("annotate") || operation.equals("index") || operation.equals("delete") || operation.equals("updateCycle") || operation.equals("indexProjects")) {
        if (uid == null || pw == null || (! uid.equals("wsp4711") && ! pw.equals("blabla4711"))) {
          out.write("<error>" + "incorrect uid or pw" + "</error>");
          return;
        }
        ArrayList<String> parameters = null;
        if (projects != null) {
          parameters = new ArrayList<String>();
          parameters.add(projects);
        }
        CmsOperation cmsOperation = new CmsOperation("ProjectManager", operation, parameters); 
        CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
        cmsOperation = scheduler.doOperation(cmsOperation);
        String jobbbbId = "" + cmsOperation.getOrderId();
        String baseUrl = getBaseUrl(request);
        String docJobUrlStr = baseUrl + "/query/GetCmsJobs?id=" + jobbbbId;
        out.write("<result>\n");
        out.write("<operation>" + operation + "</operation>\n");
        if (projects != null)
          out.write("<projects>" + projects + "</projects>\n");
        out.write("<operationResult>\n");
        out.write("<docJob>\n");
        out.write("<id>" + jobbbbId + "</id>\n");
        out.write("<url>" + docJobUrlStr + "</url>\n");
        if (operation.equals("updateCycle") && updateCycleProjects != null) {
          out.write("<updateCycleProjects>" + updateCycleProjects + "</updateCycleProjects>\n");
        }
        out.write("</docJob>\n");
        out.write("</operationResult>\n");
        out.write("</result>\n");
      } else if (operation.equals("status")) {
        org.bbaw.wsp.cms.collections.ProjectManager pm = org.bbaw.wsp.cms.collections.ProjectManager.getInstance();
        String projectStatusXmlStr = null;
        if (projects == null)
          projectStatusXmlStr = pm.getStatusProjectXmlStr();
        else          
          projectStatusXmlStr = pm.getStatusProjectXmlStr(projects);
        out.write("<result>\n");
        out.write("<operation>" + operation + "</operation>\n");
        if (projects != null)
          out.write("<queryProjects>" + projects + "</queryProjects>\n");
        out.write("<status>\n");
        out.write(projectStatusXmlStr + "\n");
        out.write("</status>\n");
        if (updateCycleProjects != null)
          out.write("<updateCycleProjects>" + updateCycleProjects + "</updateCycleProjects>\n");
        out.write("</result>\n");
      } else if (operation.equals("killJob")) {
        CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
        Integer jobIdInt = Integer.parseInt(jobId);
        boolean success = scheduler.killOperation(jobIdInt);
        String status = "job killed";
        if (! success)
          status = "job could not be killed";
        String baseUrl = getBaseUrl(request);
        String docJobUrlStr = baseUrl + "/query/GetCmsJobs";
        out.write("<result>\n");
        out.write("<operation>" + operation + "</operation>\n");
        out.write("<jobId>" + jobId + "</jobId>\n");
        out.write("<operationResult>\n");
        out.write("<status>" + status + "</status>\n");
        out.write("<remainingJobsUrl>" + docJobUrlStr + "</remainingJobsUrl>\n");
        out.write("</operationResult>\n");
        out.write("</result>\n");
      } else {
        String errorStr = "Error: Operation: " + operation + " is not supported";
        out.write("<error>" + errorStr + "</error>");
        return;
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private String getUpdateCycleProjectsIdsStr() throws ApplicationException {
    String retStr = null;
    ProjectReader projectReader = ProjectReader.getInstance();
    ArrayList<Project> projects = projectReader.getUpdateCycleProjects();
    if (projects != null) {
      ArrayList<String> cycleProjectIds = new ArrayList<String>();
      for (int i=0; i<projects.size(); i++) {
        cycleProjectIds.add(projects.get(i).getId());
      }
      retStr = cycleProjectIds.toString();
    }
    return retStr; 
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  protected String getBaseUrl( HttpServletRequest request ) {
    return request.getScheme() + "://" + request.getServerName() + request.getContextPath();
  }
  
}
