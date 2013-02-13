package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.text.WordUtils;

import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.document.SubjectHandler;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.lt.general.Language;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class About extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private HttpClient httpClient; 
  private XslResourceTransformer aboutTransformer;

  public About() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    httpClient = new HttpClient();
    int timeout = 2 * 1000;
    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);  // TODO does that work ?
    try {
      aboutTransformer = new XslResourceTransformer("about.xsl");
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String result = "";
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String query = request.getParameter("query"); // query, e.g. "Ramones" or "Moritz, Karl Philipp"
    String type = request.getParameter("type"); // e.g. person, place, subject, swd
    if (type == null)
      type = "none";
    String lang = request.getParameter("language");
    if (lang == null)
      lang = "eng";
    String language = Language.getInstance().getLanguageId(lang);  // language with two chars, e.g. de
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "html";
    String cssUrl = request.getParameter("cssUrl");
    String baseUrl = getBaseUrl(request);
    if (cssUrl == null) {
      cssUrl = baseUrl + "/css/about.css";
    }
    if (outputFormat.equals("xml"))
      response.setContentType("text/xml");
    else if (outputFormat.equals("html"))
      response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    try {
      Person person = null;
      String dbpediaKey = WordUtils.capitalize(query);
      dbpediaKey = dbpediaKey.replaceAll(" ", "_");
      dbpediaKey = dbpediaKey.replaceAll("[,.;]", "");
      if (type.equals("person")) {
        person = new Person(query);
        String surname = person.getSurname();
        surname = surname.replaceAll(" ", "_");
        dbpediaKey = surname;
        String foreName = person.getForename();
        if (foreName != null) {
          String dbPediaForeName = foreName.replaceAll(" ", "_");
          dbpediaKey = dbPediaForeName + "_" + surname;
        }
      }
      String dbPediaXmlStr = getDBpediaXmlStr(dbpediaKey, language);
      if (dbPediaXmlStr == null)
        dbPediaXmlStr = "";
      dbPediaXmlStr = dbPediaXmlStr.replaceAll("<\\?xml.*?\\?>", "");  // remove the xml declaration if it exists
      StringBuilder aboutXmlStrBuilder = new StringBuilder();
      aboutXmlStrBuilder.append("<about>");
      aboutXmlStrBuilder.append("<query>");
      aboutXmlStrBuilder.append("<name>" + query + "</name>");
      aboutXmlStrBuilder.append("<key>" + dbpediaKey + "</key>");
      String ddcCode = "none";
      if (type.equals("ddc"))
        ddcCode = SubjectHandler.getInstance().getDdcCode(query);
      aboutXmlStrBuilder.append("<ddc>" + ddcCode + "</ddc>");
      aboutXmlStrBuilder.append("<baseUrl>" + baseUrl + "</baseUrl>");
      aboutXmlStrBuilder.append("</query>");
      aboutXmlStrBuilder.append("<dbpedia>");
      aboutXmlStrBuilder.append(dbPediaXmlStr);
      aboutXmlStrBuilder.append("</dbpedia>");
      if (type.equals("person")) {
        String surName = person.getSurname();
        String foreName = person.getForename();
        String pdrXmlStr = getPdrXmlStr(surName, foreName);
        if (pdrXmlStr == null)
          pdrXmlStr = "";
        pdrXmlStr = pdrXmlStr.replaceAll("<\\?xml.*?\\?>", "");  // remove the xml declaration if it exists
        aboutXmlStrBuilder.append("<pdr>");
        aboutXmlStrBuilder.append(pdrXmlStr);
        aboutXmlStrBuilder.append("</pdr>");
      }
      aboutXmlStrBuilder.append("</about>");
      if (outputFormat.equals("html")) {
        String title = "About";
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        String head = "<head><title>" + title + "</title><link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/></head>";
        QName typeQName = new QName("type");
        XdmValue typeXdmValue = new XdmAtomicValue(type);
        QName queryQName = new QName("query");
        XdmValue queryXdmValue = new XdmAtomicValue(query);
        QName languageQName = new QName("language");
        XdmValue languageXdmValue = new XdmAtomicValue(language);
        aboutTransformer.setParameter(typeQName, typeXdmValue);
        aboutTransformer.setParameter(queryQName, queryXdmValue);
        aboutTransformer.setParameter(languageQName, languageXdmValue);
        String htmlStr = aboutTransformer.transformStr(aboutXmlStrBuilder.toString());
        result = xmlHeader + "<html>" + head + "<body>" + htmlStr + "</body>" + "</html>";
      } else {
        result = dbPediaXmlStr;
      }
      out.print(result);
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }  

  private String getDBpediaXmlStr(String key, String language) throws ApplicationException {
    String dbPediaXmlStr = null;
    String protocol = "http"; 
    String host = language + ".dbpedia.org";
    if (language.equals("en"))
      host = "dbpedia.org"; 
    String port = "80";
    String dbPediaResource = protocol + "://" + host + "/resource/" + key;
    try {
      String keyEncoded = URLEncoder.encode(key, "utf-8");
      if (key.startsWith("Category:")) {  // english server does not recognize encoded ":" in query string
        String keyTmp = key.substring(9);
        String keyTmpEncoded = URLEncoder.encode(keyTmp, "utf-8");
        keyEncoded = "Category:" + keyTmpEncoded;
      }
      String request = "/data/" + keyEncoded + ".rdf";
      dbPediaXmlStr = performGetRequest(protocol, host, port, request);
      // redirection if necessary
      if (dbPediaXmlStr != null && dbPediaXmlStr.contains("wikiPageRedirects")) {
        XQueryEvaluator xQueryEvaluator = new XQueryEvaluator();
        String redirectXPath = "string(/*:RDF/*:Description[@*:about = '" + dbPediaResource + "']/*:wikiPageRedirects/@*:resource)";
        String redirectUrl = xQueryEvaluator.evaluateAsString(dbPediaXmlStr, redirectXPath);
        if (redirectUrl != null) {
          int index = redirectUrl.lastIndexOf("/");
          if (index != -1) {
            String redirectKey = redirectUrl.substring(index + 1);
            redirectKey = redirectKey.replaceAll("\"", "");
            redirectKey = URLEncoder.encode(redirectKey, "utf-8");
            request = "/data/" + redirectKey + ".rdf";
            dbPediaXmlStr = performGetRequest(protocol, host, port, request);
          }
        }
      }
    } catch (UnsupportedEncodingException e) {
      throw new ApplicationException(e);
    }
    return dbPediaXmlStr;
  }
  
  private String getPdrXmlStr(String personName, String otherNames) throws ApplicationException {
    String pdrXmlStr = null;
    String protocol = "http"; 
    String host = "pdrdev.bbaw.de"; 
    String port = "80"; 
    try {
      String personNameEncoded = URLEncoder.encode(personName, "utf-8");
      String request = "/concord/1-4/?n=" + personNameEncoded;
      if (otherNames != null) {
        String otherNamesEncoded = URLEncoder.encode(otherNames, "utf-8");
        request = request + "&on=" + otherNamesEncoded;
      }
      pdrXmlStr = performGetRequest(protocol, host, port, request);
    } catch (UnsupportedEncodingException e) {
      throw new ApplicationException(e);
    }
    return pdrXmlStr;
  }
  
  private String getBaseUrl(HttpServletRequest request) {
    return request.getContextPath();
  }

  private String performGetRequest(String protocol, String host, String port, String requestName) throws ApplicationException {
    String resultStr = null;
    try {
      String portPart = ":" + port;
      String urlStr = protocol + "://" + host + portPart + requestName;
      GetMethod method = new GetMethod(urlStr);
      int statusCode = httpClient.executeMethod(method);
      if (statusCode < 400) {
        byte[] resultBytes = method.getResponseBody();
        resultStr = new String(resultBytes, "utf-8");
      }
      method.releaseConnection();
    } catch (Exception e) {
      // nothing
    }
    return resultStr;
  }
}