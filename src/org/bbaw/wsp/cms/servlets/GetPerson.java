package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

import org.bbaw.wsp.cms.transform.XslResourceTransformer;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class GetPerson extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private HttpClient httpClient; 
  private XslResourceTransformer personTransformer;

  public GetPerson() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    httpClient = new HttpClient();
    try {
      personTransformer = new XslResourceTransformer("pdrPerson.xsl");
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String result = "";
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String name = request.getParameter("n"); // name (surname, official name)
    String otherNames = request.getParameter("on"); // other names (forenames, less official names, uncommon names)
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "html";
    String cssUrl = request.getParameter("cssUrl");
    if (cssUrl == null) {
      String baseUrl = getBaseUrl(request);
      cssUrl = baseUrl + "/css/person.css";
    }
    if (outputFormat.equals("xml"))
      response.setContentType("text/xml");
    else if (outputFormat.equals("html"))
      response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    try {
      String pdrXmlStr = getPdrXmlStr(name, otherNames);
      if (outputFormat.equals("html")) {
        String title = "Persons";
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        String head = "<head><title>" + title + "</title><link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/></head>";
        String pdrHtmlStr = personTransformer.transformStr(pdrXmlStr);
        result = xmlHeader + "<html>" + head + "<body>" + pdrHtmlStr + "</body>" + "</html>";
      } else {
        result = pdrXmlStr;
      }
      out.print(result);
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }  

  private String getPdrXmlStr(String personName, String otherNames) throws ApplicationException {
    String protocol = "http"; 
    String host = "pdrdev.bbaw.de"; 
    String port = "80"; 
    String request = "/concord/1-4/?n=" + personName;
    if (otherNames != null)
      request = request + "&on=" + otherNames;
    String pdrXmlStr = performGetRequest(protocol, host, port, request);
    return pdrXmlStr;
  }
  
  private String getBaseUrl(HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(HttpServletRequest request) {
    if ( ( request.getServerPort() == 80 ) || ( request.getServerPort() == 443 ) )
      return request.getScheme() + "://" + request.getServerName();
    else
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  private String performGetRequest(String protocol, String host, String port, String requestName) throws ApplicationException {
    String resultStr = null;
    try {
      String portPart = ":" + port;
      String urlStr = protocol + "://" + host + portPart + requestName;
      GetMethod method = new GetMethod(urlStr);
      httpClient.executeMethod(method);
      byte[] resultBytes = method.getResponseBody();
      resultStr = new String(resultBytes, "utf-8");
      method.releaseConnection();
    } catch (HttpException e) {
      throw new ApplicationException(e);
    } catch (IOException e) {
      throw new ApplicationException(e);
    }
    return resultStr;
  }
}