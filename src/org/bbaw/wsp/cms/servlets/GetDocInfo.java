package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbaw.wsp.cms.document.MetadataRecord;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.lucene.IndexHandler;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;
import de.mpg.mpiwg.berlin.mpdl.util.StringUtils;
import de.mpg.mpiwg.berlin.mpdl.util.Util;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

public class GetDocInfo extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private XQueryEvaluator xQueryEvaluator = null;

  public GetDocInfo() {
    super();
  }

  public void init(ServletConfig config) throws ServletException  {
    super.init(config);
    ServletContext context = getServletContext();
    xQueryEvaluator = (XQueryEvaluator) context.getAttribute("xQueryEvaluator");
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String docId = request.getParameter("docId");
    String field = request.getParameter("field");
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "xml";
    try {
      IndexHandler indexHandler = IndexHandler.getInstance();
      MetadataRecord mdRecord = indexHandler.getDocMetadata(docId);
      if (outputFormat.equals("xml"))
        response.setContentType("text/xml");
      else if (outputFormat.equals("html") || outputFormat.equals("json"))
        response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      if (mdRecord != null && outputFormat.equals("xml")) {
        out.print("<doc>");
        String collectionNames = mdRecord.getCollectionNames();
        if ((field == null || (field != null && field.equals("collectionNames"))) && collectionNames != null)
          out.print("<project>" + collectionNames + "</project>");
        out.print("<id>" + docId + "</id>");
        String uri = mdRecord.getUri();
        if ((field == null || (field != null && field.equals("uri"))) && uri != null) {
          uri = StringUtils.deresolveXmlEntities(uri);
          out.print("<uri>" + uri + "</uri>");
        }
        String webUri = mdRecord.getWebUri();
        if ((field == null || (field != null && field.equals("webUri"))) && webUri != null) {
          webUri = StringUtils.deresolveXmlEntities(webUri);
          out.print("<webUri>" + webUri + "</webUri>");
        }
        String author = mdRecord.getCreator();
        if ((field == null || (field != null && field.equals("author"))) && author != null) {
          author = StringUtils.deresolveXmlEntities(author);
          out.print("<author>" + author + "</author>");
        }
        String title = mdRecord.getTitle();
        if ((field == null || (field != null && field.equals("title"))) && title != null) {
          title = StringUtils.deresolveXmlEntities(title);
          out.print("<title>" + title + "</title>");
        }
        String language = mdRecord.getLanguage();
        if ((field == null || (field != null && field.equals("language"))) && language != null)
          out.print("<language>" + language + "</language>");
        String publisher = mdRecord.getPublisher();
        if ((field == null || (field != null && field.equals("publisher"))) && publisher != null) {
          publisher = StringUtils.deresolveXmlEntities(publisher);
          out.print("<publisher>" + publisher + "</publisher>");
        }
        String date = mdRecord.getYear();
        if ((field == null || (field != null && field.equals("date"))) && date != null)
          out.print("<date>" + date + "</date>");
        String description = mdRecord.getDescription();
        if ((field == null || (field != null && field.equals("description"))) && description != null) {
          description = StringUtils.deresolveXmlEntities(description);
          out.print("<description>" + description + "</description>");
        }
        String subject = mdRecord.getSubject();
        if ((field == null || (field != null && field.equals("subject"))) && subject != null)
          out.print("<subject>" + subject + "</subject>");
        String contributor = mdRecord.getContributor();
        if ((field == null || (field != null && field.equals("contributor"))) && contributor != null)
          out.print("<contributor>" + contributor + "</contributor>");
        String coverage = mdRecord.getCoverage();
        if ((field == null || (field != null && field.equals("coverage"))) && coverage != null)
          out.print("<coverage>" + coverage + "</coverage>");
        String swd = mdRecord.getSwd();
        if ((field == null || (field != null && field.equals("swd"))) && swd != null)
          out.print("<swd>" + swd + "</swd>");
        String ddc = mdRecord.getDdc();
        if ((field == null || (field != null && field.equals("ddc"))) && ddc != null)
          out.print("<ddc>" + ddc + "</ddc>");
        String rights = mdRecord.getRights();
        if ((field == null || (field != null && field.equals("rights"))) && rights != null)
          out.print("<rights>" + rights + "</rights>");
        String license = mdRecord.getLicense();
        if ((field == null || (field != null && field.equals("license"))) && license != null)
          out.print("<license>" + license + "</license>");
        String accessRights = mdRecord.getAccessRights();
        if ((field == null || (field != null && field.equals("accessRights"))) && accessRights != null)
          out.print("<accessRights>" + accessRights + "</accessRights>");
        String baseUrl = getBaseUrl(request);
        String entitiesDetailsStr = mdRecord.getEntitiesDetails();
        if (entitiesDetailsStr != null) {
          out.print("<entitiesDetails>" + entitiesDetailsStr + "</entitiesDetails>");
        }
        String personsStr = mdRecord.getPersons();
        if (personsStr != null) {
          out.print("<persons>");
          String personsDetails = mdRecord.getPersonsDetails();
          if (personsDetails != null) {
            ArrayList<Person> persons = Person.fromXmlStr(xQueryEvaluator, personsDetails);
            for (int i=0; i<persons.size(); i++) {
              Person person = persons.get(i);
              String aboutPersonLink = baseUrl + "/query/About?query=" + person.getName() + "&type=person";
              if (language != null && ! language.isEmpty())
                aboutPersonLink = aboutPersonLink + "&language=" + language;
              person.setAboutLink(aboutPersonLink);
              String xmlStrPerson = person.toXmlStr();
              out.print(xmlStrPerson);
            }
          } else {
            String[] persons = personsStr.split("###");  // separator of persons
            for (int i=0; i<persons.length; i++) {
              Person person = new Person();
              String personName = persons[i];
              person.setName(personName);
              String aboutPersonLink = baseUrl + "/query/About?query=" + personName + "&type=person";
              if (language != null && ! language.isEmpty())
                aboutPersonLink = aboutPersonLink + "&language=" + language;
              person.setAboutLink(aboutPersonLink);
              String xmlStrPerson = person.toXmlStr();
              out.print(xmlStrPerson);
            }
          }
          out.print("</persons>");
        }
        String placesStr = mdRecord.getPlaces();
        if (placesStr != null) {
          out.print("<places>");
          String[] places = placesStr.split("###");  // separator of places
          for (int i=0; i<places.length; i++) {
            String placeName = places[i];
            placeName = StringUtils.deresolveXmlEntities(placeName);
            out.print("<place>");
            out.print("<name>" + placeName + "</name>");
            out.print("</place>");
          }
          out.print("</places>");
        }
        out.print("<system>");
        int id = mdRecord.getId();
        if ((field == null || (field != null && field.equals("id"))) && id != -1)
          out.print("<id>" + id + "</id>");
        String databaseName = mdRecord.getDatabaseName();
        if ((field == null || (field != null && field.equals("databaseName"))) && databaseName != null)
          out.print("<database>" + databaseName + "</database>");
        String type = mdRecord.getType();
        if ((field == null || (field != null && field.equals("type"))) && type != null)
          out.print("<type>" + type + "</type>");
        String systemType = mdRecord.getSystem();
        if ((field == null || (field != null && field.equals("systemType"))) && systemType != null)
          out.print("<systemType>" + systemType + "</systemType>");
        String encoding = mdRecord.getEncoding();
        if ((field == null || (field != null && field.equals("encoding"))) && encoding != null)
          out.print("<encoding>" + encoding + "</encoding>");
        int pageCount = mdRecord.getPageCount();
        if (field == null || (field != null && field.equals("countPages")))
          out.print("<countPages>" + pageCount + "</countPages>");
        Date lastModified = mdRecord.getLastModified();
        if ((field == null || (field != null && field.equals("lastModified"))) && lastModified != null) {
          String lastModifiedStr = new Util().toXsDate(lastModified);
          out.print("<lastModified>" + lastModifiedStr + "</lastModified>");
        }
        String schemaName = mdRecord.getSchemaName();
        if ((field == null || (field != null && field.equals("schema"))) && schemaName != null)
          out.print("<schema>" + schemaName + "</schema>");
        out.print("</system>");
        out.print("</doc>");
      } else if (mdRecord != null && outputFormat.equals("json")) {
        // TODO
        
      } else {
        out.print("<result>" + "no resource found with id: " + docId + "</result>");
      }
    } catch (ApplicationException e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private String getBaseUrl(HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName();
  }
}
