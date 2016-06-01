package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.scheduler.CmsChainScheduler;
import org.bbaw.wsp.cms.scheduler.CmsOperation;
import org.bbaw.wsp.cms.servlets.util.ServletUtil;

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
    try {
      String outputXmlStr = "";
      if (operation.equals("update") || operation.equals("harvest") || operation.equals("annotate") || operation.equals("index") || operation.equals("delete")) {
        if (uid == null || pw == null || (! uid.equals("wsp4711") && ! pw.equals("blabla4711"))) {
          out.write("<error>" + "incorrect uid or pw" + "</error>");
          return;
        }
        ArrayList<String> parameters = new ArrayList<String>();
        parameters.add(projects);
        CmsOperation cmsOperation = new CmsOperation("ProjectManager", operation, parameters); 
        CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
        cmsOperation = scheduler.doOperation(cmsOperation);
        String jobId = "" + cmsOperation.getOrderId();
        String baseUrl = ServletUtil.getInstance().getBaseUrl(request);
        String docJobUrlStr = baseUrl + "/update/GetCmsJobs?id=" + jobId;
        out.write("<result>\n");
        out.write("<operation>" + operation + "</operation>\n");
        out.write("<projects>" + projects + "</projects>\n");
        out.write("<operationResult>\n");
        out.write("<docJob>\n");
        out.write("<id>" + jobId + "</id>\n");
        out.write("<url>" + docJobUrlStr + "</url>\n");
        out.write("</docJob>\n");
        out.write("</operationResult>\n");
        out.write("</result>\n");
      } else if (operation.equals("status")) {
        org.bbaw.wsp.cms.collections.ProjectManager pm = org.bbaw.wsp.cms.collections.ProjectManager.getInstance();
        if (projects != null) {
          outputXmlStr = pm.getStatusProjectXmlStr(projects);
          out.write("<result>\n");
          out.write("<operation>" + operation + "</operation>\n");
          out.write("<projects>" + projects + "</projects>\n");
          out.write("<operationResult>\n");
          out.write(outputXmlStr);
          out.write("</operationResult>\n");
          out.write("</result>\n");
        }
      } else {
        String errorStr = "Error: Operation: " + operation + " is not supported";
        out.write("<error>" + errorStr + "</error>");
        return;
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
