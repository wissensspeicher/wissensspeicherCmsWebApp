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
import org.bbaw.wsp.cms.scheduler.CmsDocOperation;

public class GetDocumentJobs extends HttpServlet {
  private static final long serialVersionUID = 1L;
  public GetDocumentJobs() {
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
      ArrayList<CmsDocOperation> docOperations = new ArrayList<CmsDocOperation>();
      if (getAllJobs) {
        docOperations = scheduler.getDocOperations();
      } else {
        int jobId = Integer.parseInt(jobIdStr);
        CmsDocOperation docOperation = scheduler.getDocOperation(jobId);
        if (docOperation != null)
          docOperations.add(docOperation);
      }
      PrintWriter out = response.getWriter();
      String resultStr = "";
      if (outputFormat.equals("xml")) {
        response.setContentType("text/xml");
        resultStr = createXmlString(docOperations);
      } else { 
        response.setContentType("text/html");
        resultStr = createHtmlString(docOperations);
      }
      out.print(resultStr);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  String createXmlString(ArrayList<CmsDocOperation> docOperations) {
    StringBuilder result = new StringBuilder();
    if (docOperations != null && ! docOperations.isEmpty()) {
      result.append("<docJobs>");
      for (int i=0; i<docOperations.size(); i++) {
        CmsDocOperation docOperation = docOperations.get(i);
        result.append("<job>");
        int jobId = docOperation.getOrderId();
        result.append("<id>" + jobId + "</id>");
        result.append("<name>" + docOperation.getName() + "</name>");
        result.append("<status>");
        Date start = docOperation.getStart();
        String startStr = "No start time available because job is scheduled into server queue where other jobs have been started earlier";
        if (start != null)
          startStr = start.toString();
        result.append("<started>" + startStr + "</started>");
        Date end = docOperation.getEnd();
        String endStr = "No end time available because job is not finished yet";
        if (end != null)
          endStr = end.toString();
        result.append("<finished>" + endStr + "</finished>");
        String status = docOperation.getStatus();
        result.append("<description>" + status + "</description>");
        String errorMessage = docOperation.getErrorMessage();
        if (errorMessage == null)
          errorMessage = "no error";
        result.append("<error>" + errorMessage + "</error>");
        result.append("</status>");
        if (docOperation.getName().equals("delete")) {
          result.append("<destination>");
          result.append("<docId>" + docOperation.getDocIdentifier() + "</docId>");
          result.append("</destination>");
        } else if (docOperation.getName().equals("create") || docOperation.getName().equals("update")) {
          result.append("<source>");
          result.append("<url>" + docOperation.getSrcUrl() + "</url>");
          result.append("<uploadFileName>" + docOperation.getUploadFileName() + "</uploadFileName>");
          result.append("</source>");
          result.append("<destination>");
          result.append("<docId>" + docOperation.getDocIdentifier() + "</docId>");
          result.append("</destination>");
        }
        String desc = "Document operations are maintained on server asychronously. Each operation is scheduled into a server job queue " + 
            "and is executed when all previous started jobs in the queue are worked off. Each operation needs some execution time dependent " + 
            "on the size and the number of pages of the document, the speed of the network connection and the performance of the " +
            "server.";
        result.append("<description>" + desc + "</description>");
        result.append("</job>");
      }
      result.append("</docJobs>");
    } else {
      String message = "there are no scheduled jobs (neither finished, queued or executed)";
      result.append("<message>" + message + "</message>");
    }
    return result.toString();
  }
  
  String createHtmlString(ArrayList<CmsDocOperation> docOperations) {
    StringBuilder result = new StringBuilder();
    result.append("<html>");
    result.append("<head>");
    result.append("<title>" + "Document operation status" + "</title>");
    result.append("</head>");
    result.append("<body>");
    result.append("<table>");
    result.append("<h1>" + "Document operation status" + "</h1>");
    if (docOperations != null && ! docOperations.isEmpty()) {
      for (int i=0; i<docOperations.size(); i++) {
        result.append("<tr>");
        result.append("<td>");
        CmsDocOperation docOperation = docOperations.get(i);
        result.append("<b>Operation: </b>" + docOperation.getName());
        int jobId = docOperation.getOrderId();
        result.append("<p/>");
        result.append("<b>Job id: </b>" + jobId);
        result.append("<p/>");
        result.append("<b>Job status: </b>");
        result.append("<ul>");
        Date start = docOperation.getStart();
        String startStr = "No start time available because job is scheduled into server queue where other jobs have been started earlier";
        if (start != null)
          startStr = start.toString();
        result.append("<li>");
        result.append("<b>Started: </b>");
        result.append("<started>" + startStr + "</started>");
        result.append("</li>");
        Date end = docOperation.getEnd();
        String endStr = "No end time available because job is not finished yet";
        if (end != null)
          endStr = end.toString();
        result.append("<li>");
        result.append("<b>Finished: </b>");
        result.append("<finished>" + endStr + "</finished>");
        result.append("</li>");
        String status = docOperation.getStatus();
        result.append("<li>");
        result.append("<b>Description: </b>");
        result.append("<description>" + status + "</description>");
        result.append("</li>");
        String errorMessage = docOperation.getErrorMessage();
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
        if (docOperation.getName().equals("delete")) {
          result.append("<b>Destination</b>");
          result.append("<ul>");
          result.append("<li>");
          result.append("<b>Document identifier: </b>");
          result.append("<docId>" + docOperation.getDocIdentifier() + "</docId>");
          result.append("</li>");
          result.append("</ul>");
        } else if (docOperation.getName().equals("create") || docOperation.getName().equals("update")) {
          result.append("<b>Source</b>");
          result.append("<ul>");
          result.append("<li>");
          result.append("<url>" + docOperation.getSrcUrl() + "</url>");
          String uploadFileName = docOperation.getUploadFileName();
          if (uploadFileName != null) {
            result.append("<li>");
            result.append("<uploadFileName>" + uploadFileName + "</uploadFileName>");
            result.append("</li>");
          }
          result.append("</li>");
          result.append("</ul>");
          result.append("<b>Destination</b>");
          result.append("<ul>");
          result.append("<li>");
          result.append("<b>Document identifier: </b>");
          result.append("<docId>" + docOperation.getDocIdentifier() + "</docId>");
          result.append("</li>");
          result.append("</ul>");
        }
        String desc = "Document operations are maintained on server asychronously. Each operation is scheduled into a server job queue " + 
            "and is executed when all previous started jobs in the queue are worked off. Each operation needs some execution time dependent " + 
            "on the size and the number of pages of the document, the speed of the network connection and the performance of the " +
            "server."; 
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
