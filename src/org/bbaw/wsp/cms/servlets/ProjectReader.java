package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.index.IndexableField;
import org.bbaw.wsp.cms.collections.Organization;
import org.bbaw.wsp.cms.collections.Project;
import org.bbaw.wsp.cms.collections.ProjectCollection;
import org.bbaw.wsp.cms.collections.Subject;
import org.bbaw.wsp.cms.document.Document;
import org.bbaw.wsp.cms.document.Facets;
import org.bbaw.wsp.cms.document.Hits;
import org.bbaw.wsp.cms.document.Person;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
        ArrayList<Project> projects = projectReader.getProjects();
        String jsonProjectsStr = toJsonStringProjects(projects);
        out.println(jsonProjectsStr);
        return;
      } else if (operation.equals("getProjectsSorted")) {
        String sortBy = request.getParameter("sortBy"); 
        ArrayList<Project> projects = projectReader.getProjectsSorted(sortBy);
        String jsonProjectsStr = toJsonStringProjects(projects);
        out.println(jsonProjectsStr);
        return;
      } else if (operation.equals("getProjectsByProjectType")) {
        String projectType = request.getParameter("projectType"); 
        ArrayList<Project> projects = projectReader.getProjectsByProjectType(projectType);
        String jsonProjectsStr = toJsonStringProjects(projects);
        out.println(jsonProjectsStr);
        return;
      } else if (operation.equals("getProjectsByStatus")) {
        String status = request.getParameter("status"); 
        ArrayList<Project> projects = projectReader.getProjectsByStatus(status);
        String jsonProjectsStr = toJsonStringProjects(projects);
        out.println(jsonProjectsStr);
        return;
      } else if (operation.equals("getCollections")) {
        String projectRdfId = request.getParameter("projectRdfId"); 
        ArrayList<ProjectCollection> collections = projectReader.getCollections(projectRdfId);
        String jsonStr = toJsonStringCollections(collections);
        out.println(jsonStr);
        return;
      } else if (operation.equals("getSubjects")) {
        String projectRdfId = request.getParameter("projectRdfId"); 
        if (projectRdfId != null) {
          ArrayList<Subject> subjects = projectReader.getSubjects(projectRdfId);
          String jsonStr = toJsonStringSubjects(subjects);
          out.println(jsonStr);
          return;
        } else {
          ArrayList<Subject> subjects = projectReader.getSubjects();
          String jsonStr = toJsonStringSubjects(subjects);
          out.println(jsonStr);
          return;
        }
      } else if (operation.equals("getStaff")) {
        String projectRdfId = request.getParameter("projectRdfId"); 
        if (projectRdfId != null) {
          ArrayList<Person> persons = projectReader.getStaff(projectRdfId);
          String jsonStr = toJsonStringPersons(persons);
          out.println(jsonStr);
          return;
        } else {
          ArrayList<Person> persons = projectReader.getStaff();
          String jsonStr = toJsonStringPersons(persons);
          out.println(jsonStr);
          return;
        }
      } else if (operation.equals("queryProjects")) {
        String queryLanguage = request.getParameter("queryLanguage"); 
        if (queryLanguage == null)
          queryLanguage = "gl";  // google like
        String query = request.getParameter("query"); 
        String sortBy = request.getParameter("sortBy");
        String[] sortFields = null;
        if (sortBy != null && ! sortBy.trim().isEmpty())
          sortFields = sortBy.split(" ");
        String fieldExpansion = request.getParameter("fieldExpansion");
        if (fieldExpansion == null)
          fieldExpansion = "all";
        String language = request.getParameter("language");
        if (language != null && language.equals("none"))
          language = null;
        String translate = request.getParameter("translate");
        Boolean translateBool = false;
        if (translate != null && translate.equals("true"))
          translateBool = true;
        String pageStr = request.getParameter("page");
        if (pageStr == null)
          pageStr = "1";
        int page = Integer.parseInt(pageStr);
        String pageSizeStr = request.getParameter("pageSize");
        if (pageSizeStr == null)
          pageSizeStr = "10";
        int pageSize = Integer.parseInt(pageSizeStr);
        int from = (page * pageSize) - pageSize;  // e.g. 0
        int to = page * pageSize - 1;  // e.g. 9
        IndexHandler indexHandler = IndexHandler.getInstance();
        Hits hits = indexHandler.queryProjects(queryLanguage, query, sortFields, fieldExpansion, language, from, to, translateBool);
        int sizeTotalDocuments = hits.getSizeTotalDocuments();
        ArrayList<Document> docs = hits.getHits();
        String luceneQuery = hits.getQuery().toString();
        JSONObject jsonOutput = new JSONObject();
        jsonOutput.put("query", query);
        jsonOutput.put("luceneQuery", luceneQuery);
        jsonOutput.put("numberOfHits", String.valueOf(hits.getSize()));
        Facets facets = hits.getFacets();
        if (facets != null && facets.size() > 0) {
          String baseUrl = getBaseUrl(request);
          facets.setBaseUrl(baseUrl);
          facets.setOutputOptions("showAllFacets");
          JSONObject jsonFacets = facets.toJsonObject();
          jsonOutput.put("facets", jsonFacets);
        }
        jsonOutput.put("sizeTotalDocuments", String.valueOf(sizeTotalDocuments));
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<hits.getSize(); i++) {
          JSONObject jsonHit = new JSONObject();
          org.bbaw.wsp.cms.document.Document projectDoc = docs.get(i);
          Float score = projectDoc.getScore();
          if (score != null) {
            jsonHit.put("luceneScore", score);
          }
          IndexableField projectRdfIdField = projectDoc.getField("rdfId");
          if (projectRdfIdField != null) {
            String projectRdfId = projectRdfIdField.stringValue();
            Project project = org.bbaw.wsp.cms.collections.ProjectReader.getInstance().getProjectByRdfId(projectRdfId);
            if (project != null)
              jsonHit.put("project", project.toJsonObject());
          }
          jsonArray.add(jsonHit);
        }
        jsonOutput.put("hits", jsonArray);
        String jsonStr = jsonOutput.toJSONString();
        out.println(jsonStr);
        return;
      } else if (operation.equals("getOrganizations")) {
        ArrayList<Organization> organizations = projectReader.getOrganizations();
        String jsonStr = toJsonStringOrganizations(organizations);
        out.println(jsonStr);
        return;
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private String toJsonStringProjects(ArrayList<Project> projects) throws ApplicationException {
    JSONArray jsonProjects = new JSONArray();
    for (int i=0; i<projects.size(); i++) {
       Project project = projects.get(i);
       jsonProjects.add(project.toJsonObject());
    }
    String jsonStr = jsonProjects.toJSONString();
    return jsonStr;
  }
  
  private String toJsonStringCollections(ArrayList<ProjectCollection> collections) throws ApplicationException {
    JSONArray jsonCollections = new JSONArray();
    for (int i=0; i<collections.size(); i++) {
      ProjectCollection collection = collections.get(i);
      jsonCollections.add(collection.toJsonObject());
    }
    String jsonStr = jsonCollections.toJSONString();
    return jsonStr;
  }

  private String toJsonStringOrganizations(ArrayList<Organization> organizations) throws ApplicationException {
    JSONArray jsonOrganizations = new JSONArray();
    for (int i=0; i<organizations.size(); i++) {
      Organization organization = organizations.get(i);
       jsonOrganizations.add(organization.toJsonObject());
    }
    String jsonStr = jsonOrganizations.toJSONString();
    return jsonStr;
  }
  
  private String toJsonStringSubjects(ArrayList<Subject> subjects) throws ApplicationException {
    JSONArray jsonSubjects = new JSONArray();
    for (int i=0; i<subjects.size(); i++) {
      Subject subject = subjects.get(i);
      jsonSubjects.add(subject.toJsonObject());
    }
    String jsonStr = jsonSubjects.toJSONString();
    return jsonStr;
  }

  private String toJsonStringPersons(ArrayList<Person> persons) throws ApplicationException {
    JSONArray jsonPersons = new JSONArray();
    for (int i=0; i<persons.size(); i++) {
      Person subject = persons.get(i);
      jsonPersons.add(subject.toJsonObject());
    }
    String jsonStr = jsonPersons.toJSONString();
    return jsonStr;
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
