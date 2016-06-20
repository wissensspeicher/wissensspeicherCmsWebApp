package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.scheduler.CmsChainScheduler;
import org.bbaw.wsp.cms.scheduler.CmsOperation;

public class GetCmsJobs extends HttpServlet {
  private static final long serialVersionUID = 4711L;
  public GetCmsJobs() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    response.setContentType("text/xml");
    String jobIdStr = request.getParameter("id");
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    try {
      boolean getAllJobs = false;
      if (jobIdStr == null)
        getAllJobs = true;
      CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
      ArrayList<CmsOperation> operations = new ArrayList<CmsOperation>();
      if (getAllJobs) {
        operations = scheduler.getOperations();
      } else {
        int jobId = Integer.parseInt(jobIdStr);
        CmsOperation operation = scheduler.getOperation(jobId);
        if (operation != null)
          operations.add(operation);
      }
      PrintWriter out = response.getWriter();
      String resultStr = "";
      if (outputFormat.equals("xml")) {
        response.setContentType("text/xml");
        resultStr = createXmlString(operations);
      } else { 
        response.setContentType("text/html");
        resultStr = createHtmlString(operations);
      }
      out.print(resultStr);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  String createXmlString(ArrayList<CmsOperation> operations) {
    StringBuilder result = new StringBuilder();
    if (operations != null && ! operations.isEmpty()) {
      result.append("<jobs>");
      for (int i=0; i<operations.size(); i++) {
        CmsOperation operation = operations.get(i);
        result.append("<job>");
        int jobId = operation.getOrderId();
        result.append("<id>" + jobId + "</id>");
        result.append("<type>" + operation.getType() + "</type>");
        result.append("<name>" + operation.getName() + "</name>");
        ArrayList<String> parameters = operation.getParameters();
        if (parameters != null) {
          result.append("<parameters>");
          for (int j=0; j<parameters.size(); j++) {
            result.append("<parameter>" + parameters.get(j) + "</parameter>");
          }
          result.append("</parameters>");
        }
        result.append("<status>");
        Date start = operation.getStart();
        String startStr = "No start time available because job is scheduled into server queue where other jobs have been started earlier";
        if (start != null)
          startStr = start.toString();
        result.append("<started>" + startStr + "</started>");
        Date end = operation.getEnd();
        String endStr = "No end time available because job is not finished yet";
        if (end != null)
          endStr = end.toString();
        result.append("<finished>" + endStr + "</finished>");
        String status = operation.getStatus();
        result.append("<description>" + status + "</description>");
        String errorMessage = operation.getErrorMessage();
        if (errorMessage == null)
          errorMessage = "no error";
        result.append("<error>" + errorMessage + "</error>");
        result.append("</status>");
        String desc = "Operations are maintained on server asychronously. Each operation is scheduled into a server job queue " + 
            "and is executed when all previous started jobs in the queue are worked off. Each operation needs some execution time dependent " + 
            "on the operation, the speed of the network connection and the performance of the server.";
        result.append("<description>" + desc + "</description>");
        result.append("</job>");
      }
      result.append("</jobs>");
    } else {
      String message = "there are no scheduled jobs (neither finished, queued or executed)";
      result.append("<message>" + message + "</message>");
    }
    return result.toString();
  }
  
  String createHtmlString(ArrayList<CmsOperation> operations) {
    StringBuilder result = new StringBuilder();
    result.append("<html>");
    result.append("<head>");
    result.append("<title>" + "Operation status" + "</title>");
    result.append("</head>");
    result.append("<body>");
    result.append("<table>");
    result.append("<h1>" + "Operation status" + "</h1>");
    if (operations != null && ! operations.isEmpty()) {
      for (int i=0; i<operations.size(); i++) {
        result.append("<tr>");
        result.append("<td>");
        CmsOperation operation = operations.get(i);
        result.append("<b>Operation: </b>" + operation.getName());
        ArrayList<String> parameters = operation.getParameters();
        if (parameters != null) {
          result.append(" (parameters: " + parameters.toString() + ")");
        }
        int jobId = operation.getOrderId();
        result.append("<p/>");
        result.append("<b>Job id: </b>" + jobId);
        result.append("<p/>");
        result.append("<b>Job status: </b>");
        result.append("<ul>");
        Date start = operation.getStart();
        String startStr = "No start time available because job is scheduled into server queue where other jobs have been started earlier";
        if (start != null)
          startStr = start.toString();
        result.append("<li>");
        result.append("<b>Started: </b>");
        result.append("<started>" + startStr + "</started>");
        result.append("</li>");
        Date end = operation.getEnd();
        String endStr = "No end time available because job is not finished yet";
        if (end != null)
          endStr = end.toString();
        result.append("<li>");
        result.append("<b>Finished: </b>");
        result.append("<finished>" + endStr + "</finished>");
        result.append("</li>");
        String status = operation.getStatus();
        result.append("<li>");
        result.append("<b>Description: </b>");
        result.append("<description>" + status + "</description>");
        result.append("</li>");
        String errorMessage = operation.getErrorMessage();
        result.append("<li>");
        if (errorMessage != null) {
          result.append("<font color=\"#FF0000\"><b>Error: </b></font>");
          result.append("<error>" + errorMessage + "</error>");
        } else { 
          result.append("<b>Error: </b>");
          result.append("<error>" + "no error" + "</error>");
        }
        result.append("</li>");
        result.append("</ul>");
        String desc = "Operations are maintained on server asychronously. Each operation is scheduled into a server job queue " + 
            "and is executed when all previous started jobs in the queue are worked off. Each operation needs some execution time dependent " + 
            "on the operation, the speed of the network connection and the performance of the server."; 
        result.append("<b>Description: </b>" + desc);
        result.append("<p/>");
        result.append("<hr/>");
      }
      result.append("</td>");
      result.append("</tr>");
    } else {
      String message = "there are no scheduled jobs (neither finished, queued or executed)";
      result.append("<message>" + message + "</message>");
    }
    result.append("</table>");
    result.append("</body>");
    result.append("</html>");
    return result.toString();
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
