package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.jena.larq.HitLARQ;
import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemQueryHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemResultType;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.conceptsearch.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.HitGraph;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.HitGraphContainer;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.HitStatement;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.IQueryStrategy;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.QueryStrategyJena;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.detailedsearch.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openjena.atlas.json.JsonObject;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class QueryMdSystem extends HttpServlet {
  /**
   * JSON field / key for the lexical/"real" value of a literal.
   */
  private static final Object JSON_FIELD_LEXICAL_VALUE = "lexicalValue";
  /**
   * JSON field / key which indicates that the object is a blank node.
   */
  private static final Object JSON_FIELD_IS_BLANK = "isBlank";
  /**
   * JSON field / key for a general object, might be anything although literals (like blank nodes or resources)
   */
  private static final Object JSON_FIELD_OBJECT = "object";
  /**
   * JSON field / key for the resultType
   */
  private static final String JSON_FIELD_RESULT_TYPE = "resultType";
  /**
   * JSON field / key for the score
   */
  private static final String JSON_FIELD_SCORE = "score";
  /**
   * JSON field / key for the parent predicate
   */
  private static final String JSON_FIELD_PARENT_PREDICATE = "parentPredicate";
  /**
   * JSON field / key for the parent subject
   */
  private static final String JSON_FIELD_PARENT_SUBJECT = "parentSubject";
  /**
   * JSON field / key for the literal which should have a datatype and lexical type
   */
  private static final String JSON_FIELD_LITERAL = "literal";
  /**
   * JSON field / key for the predicate
   */
  private static final String JSON_FIELD_PREDICATE = "predicate";
  /**
   * JSON field / key for the subject
   */
  private static final String JSON_FIELD_SUBJECT = "subject";
  /**
   * JSON field / key for the datatype (if the object is a literal)
   */
  private static final Object JSON_FIELD_DATATYPE_URI = "datatype";
  /**
   * JSON field / key for the average score.
   */
  private static final String JSON_FIELD_AVERAGE_SCORE = "averageScore";
  /**
   * JSON field / key for the maximum score.
   */
  private static final Object JSON_FIELD_MAXIMUM_SCORE = "maximumScore";
  /**
   * JSON field / key for the hitGraphes.
   */
  private static final String JSON_FIELD_MD_HITS = "hitGraphes";
  /**
   * JSON field / key for the numberf of hits.
   */
  private static final String JSON_FIELD_NUMBER_OF_HITS = "numberOfHits";
  /**
   * JSON field / key for the search term.
   */
  private static final String JSON_FIELD_SEARCH_TERM = "searchTerm";
  /**
   * JSON field / key for the hit statements.
   */
  private static final String JSON_FIELD_STATEMENTS = "hitStatements";
  /**
   * JSON field / key for the graph name.
   */
  private static final Object JSON_FIELD_GRAPH_URL = "graphName";
  private static final long serialVersionUID = 1L;
  /**
   * The URI/name of the normdata graph as stored in the triple store.
   */
  private static final String NORMDATA_GRAPH_URI = "http://wsp.normdata.rdf/";
  /**
   * key/name for/of the parameter graphId.
   */
  private static final String PARAM_GRAPH_ID = "isGraphId";
  /**
   * key/name for/of the parameter subject.
   */
  private static final String PARAM_SUBJECT = "isSubject";
  /**
   * key for the JSON attribute for the result priority.
   */
  private static final Object PRIORITY = "priority";
  /**
   * key/name for/of the parameter projectId.
   */
  private static final String IS_PROJECT_ID = "isProjectId";
  
  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer sparqlPreloadPersonResults;
  
  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer sparqlPreloadLingResults;
  
  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer sparqlPreloadLocResults;

  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer preloadNormdata;

  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer sparqlPreloadOrgResults;

  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer sparqlPreloadMediaResults;

  /**
   * results from preloading project information by sparql 
   */
  private HitGraphContainer sparqlPreloadPerOfTimeResults;
  
  
  public QueryMdSystem() {
    super();
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    ServletContext context = getServletContext();
    final Logger logger = Logger.getLogger(QueryMdSystem.class);
    logger.info("preload normdata Metadata");
    preloadNormdata = (HitGraphContainer) context.getAttribute("preloadNormdata");
  }

  // zum testen
  // http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=marx&conceptSearch=true&outputFormat=json
  // http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=marx&detailedSearch=true&outputFormat=json[&isGraphId=true][&isSubject=true] default: subject=true und defaultGraphName = http://wsp.normdata.rdf/....

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    // HTTP response print writer
    final PrintWriter out = response.getWriter();
    // bla

    final Logger logger = Logger.getLogger(QueryMdSystem.class);
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    final String query = request.getParameter("query");
    // new RequestStatisticAnalyser(query);
    String language = request.getParameter("language");
    if (language != null && language.equals("none")) {
      language = null;
    }
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null) {
      outputFormat = "html";
    }
    if (outputFormat.equals("xml")) {
      response.setContentType("text/xml");
    } else if (outputFormat.equals("html")) {
      response.setContentType("text/html");
    } else if(outputFormat.equals("json")){
      response.setContentType("application/json");
    }
    // Suche nach Konzepten in "Vorhaben-Metadaten"
    final String conceptSearch = request.getParameter("conceptSearch");
    // Suche nach Begriffen in einzelnen Triples oder Named Graphen
    final String detailedSearch = request.getParameter("detailedSearch");
    if (query == null) {
      logger.info("no query specified: please set parameter \"query\"");
      return;
    }
    try {
      final Date begin = new Date();

      final String baseUrl = getBaseUrl(request);
      logger.info("baseUrl : " + baseUrl);
      final MdSystemQueryHandler mdQueryHandler = MdSystemQueryHandler.getInstance();

      if (conceptSearch != null && conceptSearch.equals("true")) {
        handleConceptQuery(request, response, out, logger, query, outputFormat, begin, mdQueryHandler, baseUrl);
      }

      if (detailedSearch != null && detailedSearch.equals("true")) {
        handleDetailedSearch(logger, begin, outputFormat, query, request, response, out);
      }
    } catch (final Exception e) {
      throw new ServletException(e);
    }
  }

  private void handleDetailedSearch(final Logger logger, final Date begin, final String outputFormat, final String query, final HttpServletRequest request, final HttpServletResponse response, final PrintWriter out) {
    logger.info("detailed Search");
    MdSystemQueryHandler mdqh = MdSystemQueryHandler.getInstance();
    ISparqlAdapter adapter = mdqh.getSparqlAdapter();
    // @formatter:off
    /*
     * 
     * [&graphId=true][&subject=true] default: subject=true und defaultGraphName = http://wsp.normdata.rdf/.... 
     * check, whether a parameter graphId is set. 
     * If graphId is set to true, query contains a resource.
     * 
     */
    // @formatter:on
    HitGraphContainer resultContainer = null;
    HashMap<String, List<String>> normdataHit = null;
    HashMap<String, Object> mainEles = new HashMap<String, Object>();
    if (request.getParameter(PARAM_GRAPH_ID) != null && request.getParameter(PARAM_GRAPH_ID).equals("true")) {
      // query contains the graph uri
      // call sparql adapter with the given graph uri
      URL graphUri;
      try {
        graphUri = new URL(URIUtil.decode(query));
        resultContainer = adapter.buildSparqlQuery(graphUri);
      } catch (URIException | MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else if (request.getParameter(PARAM_SUBJECT) != null && request.getParameter(PARAM_SUBJECT).equals("true")) {

      // query contains the subject uri
      // call sparql adapter and query for the given subject
      String resourceUri;
      try {
        resourceUri = URIUtil.decode(query);
        final Resource resource = new ResourceImpl("<" + resourceUri + ">");
        resultContainer = adapter.buildSparqlQuery(resource);
      } catch (final URIException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else 
      //query all project information and resolve uri
      if(request.getParameter(IS_PROJECT_ID) != null && request.getParameter(IS_PROJECT_ID).equals("true")){
        HitGraph normdatacomplete = null;
        try {
          normdatacomplete = preloadNormdata.getHitGraph(new URL("http://wsp.normdata.rdf/"));
          normdataHit = normdatacomplete.getStatementBySubject(query);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }

        List<HashMap<String, String>> hasllist = null;
        if(normdataHit.entrySet() != null){
          for (Entry<String, List<String>> entry : normdataHit.entrySet()) {
            mainEles.put(entry.getKey(), entry.getValue().get(0));
          }
        }
          // Person Project LinguisticSystem Organisation Location MediaType
          // PeriodOfTime ConferenceOrEvent
        if (normdataHit.get("contributor") != null && normdataHit.get("contributor").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> contributors = normdataHit.get("contributor");
          for (String string : contributors) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("contributor", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("contributors", hasllist);
        }
        //#############
        if (normdataHit.get("relatedCorporateBody") != null && normdataHit.get("relatedCorporateBody").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> relatedCorporateBodys = normdataHit.get("relatedCorporateBody");
          for (String string : relatedCorporateBodys) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("relatedCorporateBody", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("relatedCorporateBody", hasllist);
        }
        //#############
        if (normdataHit.get("coverage") != null && normdataHit.get("coverage").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> coverages = normdataHit.get("coverage");
          for (String string : coverages) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("coverage", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("coverage", hasllist);
        }
        //
        if (normdataHit.get("description") != null && normdataHit.get("description").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> descriptions = normdataHit.get("description");
          for (String string : descriptions) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("description", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("description", hasllist);
        }
        //
        if (normdataHit.get("language") != null && normdataHit.get("language").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> languages = normdataHit.get("description");
          for (String string : languages) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("language", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("language", hasllist);
        }
        //
        if (normdataHit.get("topic") != null && normdataHit.get("topic").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> topics = normdataHit.get("topic");
          for (String string : topics) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("topic", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("topic", hasllist); 
        }
        //
        if (normdataHit.get("contributingCorporateBody") != null && normdataHit.get("contributingCorporateBody").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> contributingCorporateBodys = normdataHit.get("contributingCorporateBody");
          for (String string : contributingCorporateBodys) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("contributingCorporateBody", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("contributingCorporateBody", hasllist);
        } 
        //
        if (normdataHit.get("fundedBy") != null && normdataHit.get("fundedBy").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> fundedBys = normdataHit.get("fundedBy");
          for (String string : fundedBys) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("fundedBy", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("fundedBy", hasllist);
        } 
        //
        if (normdataHit.get("contributingPerson") != null && normdataHit.get("contributingPerson").size() != 0) {
          hasllist = new ArrayList<HashMap<String, String>>();
          List<String> contributingPersons = normdataHit.get("contributingPerson");
          for (String string : contributingPersons) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = normdatacomplete.getStatementBySubject(string);
            if(resolved != null && resolved.entrySet() != null){
              for (Entry<String, List<String>> entrie : resolved.entrySet()) {
                resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
              }
            }else{
              resolvedValues.put("contributingPerson", string);
            }
            hasllist.add(resolvedValues);
          }
          mainEles.put("contributingPerson", hasllist);
        }
      }else { // neither graphId or subject was set
      // query contains the subject URI
      // call sparql adapter and query for the given subject within THE NORMDATA.RDF
      final URL defaultGraphName;
      try {
        final URL url = new URL(query); // is query URI? -> so it's a subjectz
        try {
          defaultGraphName = new URL(NORMDATA_GRAPH_URI);
          final String subject = "<" + URIUtil.decode(query) + ">";
          resultContainer = adapter.buildSparqlQuery(defaultGraphName, subject);
        } catch (final MalformedURLException | URIException e1) {
          e1.printStackTrace();
        }
      } catch (final Exception e) {
        // query literal
        resultContainer = adapter.buildSparqlQuery(query);
      }
    }

    /*
     * statistics
     */
    final Date end = new Date();
    final long elapsedTime = end.getTime() - begin.getTime();
    /*
     * ..:: html ::..
     */
    if (resultContainer != null && outputFormat.equals("htmlInSchoen")) {
      final StringBuilder htmlStrBuilder = new StringBuilder();
      final String cssUrl = request.getContextPath() + "/css/page.css";
      htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
      htmlStrBuilder.append("<html>");
      htmlStrBuilder.append("\n\t<head>");
      htmlStrBuilder.append("\n\t\t<title>Query: " + query + "</title>");
      htmlStrBuilder.append("\n\t\t<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/>");
      htmlStrBuilder.append("\n\t</head>");
      htmlStrBuilder.append("\n\t<body>");
      htmlStrBuilder.append("\n\t\t<table align=\"right\" valign=\"top\">");
      htmlStrBuilder.append("\n\t\t<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW CMS service\"/></a></td>");
      htmlStrBuilder.append("\n\t\t</table>");
      htmlStrBuilder.append("\n\t\t<p><strong>Search term:</strong> " + query + "</p>");
      htmlStrBuilder.append("\n\t\t<p><strong>Number of hits:</strong> " + resultContainer.size() + "</p>");
      htmlStrBuilder.append("\n\t\t<ul>");

      int counter = 0;
      for (final HitGraph hitGraph : resultContainer.getAllHits()) {
        htmlStrBuilder.append("\n\t\t\t<li><strong>QueryHit #" + (++counter) + "</strong></li>");
        htmlStrBuilder.append("\n\t\t\t\t<li><ul>");
        htmlStrBuilder.append("\n\t\t\t\t\t<li><strong>NamedGraphUrl:</strong> " + hitGraph.getNamedGraphUrl() + "</li>");
        for (final HitStatement hitStatement : hitGraph.getAllHitStatements()) {
          htmlStrBuilder.append("\n\t\t\t\t<li><ul>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Subject:</strong> " + hitStatement.getSubject() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Predicate:</strong> " + hitStatement.getPredicate() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Object:</strong> " + hitStatement.getObject() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t</li></ul>");
        }

        htmlStrBuilder.append("\n\t\t\t\t</li></ul>");
      }

      htmlStrBuilder.append("\n\t\t</ul>");
      htmlStrBuilder.append("\n\t</body>");
      htmlStrBuilder.append("\n</html>");
      out.println(htmlStrBuilder.toString()); // print html
    }
    
    /*
     * ..:: html ::..
     */
    if (resultContainer != null && outputFormat.equals("html")) {
      final StringBuilder htmlStrBuilder = new StringBuilder();
      final String cssUrl = request.getContextPath() + "/css/page.css";
      htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
      htmlStrBuilder.append("<html>");
      htmlStrBuilder.append("\n\t<head>");
      htmlStrBuilder.append("\n\t\t<title>Query: " + query + "</title>");
      htmlStrBuilder.append("\n\t\t<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/>");
      htmlStrBuilder.append("\n\t</head>");
      htmlStrBuilder.append("\n\t<body>");
      htmlStrBuilder.append("\n\t\t<table align=\"right\" valign=\"top\">");
      htmlStrBuilder.append("\n\t\t<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW CMS service\"/></a></td>");
      htmlStrBuilder.append("\n\t\t</table>");
      htmlStrBuilder.append("\n\t\t<p><strong>Search term:</strong> " + query + "</p>");
      htmlStrBuilder.append("\n\t\t<p><strong>Number of hits:</strong> " + resultContainer.size() + "</p>");
      htmlStrBuilder.append("\n\t\t<ul>");

      int counter = 0;
      for (final HitGraph hitGraph : resultContainer.getAllHits()) {
        htmlStrBuilder.append("\n\t\t\t<li><strong>QueryHit #" + (++counter) + "</strong></li>");
        htmlStrBuilder.append("\n\t\t\t\t<li><ul>");
        htmlStrBuilder.append("\n\t\t\t\t\t<li><strong>NamedGraphUrl:</strong> " + hitGraph.getNamedGraphUrl() + "</li>");
        if (hitGraph.getAvgScore() != HitGraph.DEFAULT_SCORE) {
          htmlStrBuilder.append("\n\t\t\t\t\t<li><strong>Average score:</strong>" + hitGraph.getAvgScore() + "</li>");
        }
        if (hitGraph.getHighestScore() != HitGraph.DEFAULT_SCORE) {
          htmlStrBuilder.append("\n\t\t\t\t\t<li><strong>Highest score:</strong>" + hitGraph.getHighestScore() + "</li>");
        }
        for (final HitStatement hitStatement : hitGraph.getAllHitStatements()) {
          htmlStrBuilder.append("\n\t\t\t\t<li><ul>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Subject:</strong> " + hitStatement.getSubject() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Predicate:</strong> " + hitStatement.getPredicate() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Object:</strong> " + hitStatement.getObject() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Parent subject:</strong> " + hitStatement.getSubjParent() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Parent predicate:</strong> " + hitStatement.getPredParent() + "</li>");
          if (hitStatement.getResultType().equals(MdSystemResultType.LITERAL_DEFAULT_GRAPH) || hitStatement.getResultType().equals(MdSystemResultType.LITERAL_NAMED_GRAPH)) {
            htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Score:</strong> " + hitStatement.getScore() + "</li>");
          }
          if (hitStatement.getResultType() != null) {
            htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>MdSystemResultType:</strong> " + hitStatement.getResultType() + "</li>");
          }
          htmlStrBuilder.append("\n\t\t\t\t</li></ul>");
        }

        htmlStrBuilder.append("\n\t\t\t\t</li></ul>");
      }

      htmlStrBuilder.append("\n\t\t</ul>");
      htmlStrBuilder.append("\n\t</body>");
      htmlStrBuilder.append("\n</html>");
      out.println(htmlStrBuilder.toString()); // print html
    }
    /*
     * ..:::::::::::..
     */
    /*
     * ..:: json ::..
     */
    else if (resultContainer != null && outputFormat.equals("json") && request.getParameter(IS_PROJECT_ID) == null) {
      response.setContentType("application/json"); // indicates that this
                                                   // content is pure json
      final WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.putStrings(JSON_FIELD_SEARCH_TERM, query);
      jsonEncoder.putStrings(JSON_FIELD_NUMBER_OF_HITS, resultContainer.size() + " ");
      /*
       * ..:: hitGraphes: [ ... ::..
       */
      final JSONArray jhitGraphes = new JSONArray();
      for (final HitGraph hitGraph : resultContainer.getAllHits()) {
        /*
         * ...::: singleGraph, element within the hitGraphes array :::...
         */
        try {
          final JSONObject jHitGraph = new JSONObject();

          if (hitGraph.getAvgScore() != HitGraph.DEFAULT_SCORE) {
            jHitGraph.put(JSON_FIELD_AVERAGE_SCORE, hitGraph.getAvgScore());
          }
          if (hitGraph.getHighestScore() != HitGraph.DEFAULT_SCORE) {
            jHitGraph.put(JSON_FIELD_MAXIMUM_SCORE, hitGraph.getHighestScore());
          }
          if (hitGraph.getNamedGraphUrl() != null) {
            jHitGraph.put(JSON_FIELD_GRAPH_URL, "" + hitGraph.getNamedGraphUrl());
          }
          /*
           * ....:::: hitStatements: [... ::::....
           */
          final JSONArray jHitStatements = new JSONArray();
          for (final HitStatement hitStatement : hitGraph.getAllHitStatements()) {
            final JSONObject jHitStatement = new JSONObject();
            final String encodedSubj = URIUtil.encodeQuery(hitStatement.getSubject().toString());
            jHitStatement.put(JSON_FIELD_SUBJECT, encodedSubj);
            final String encodedPred = URIUtil.encodeQuery(hitStatement.getPredicate().toString());
            jHitStatement.put(JSON_FIELD_PREDICATE, encodedPred);
            final RDFNode hitObject = hitStatement.getObject();
            if (hitObject.isLiteral()) {
              final JSONObject hitLiteral = new JSONObject();
              final String encodedLit = hitStatement.getObject().asLiteral().getLexicalForm();
              final String datatypeUri = hitStatement.getObject().asLiteral().getDatatypeURI();
              hitLiteral.put(JSON_FIELD_LEXICAL_VALUE, encodedLit);
              if (datatypeUri != null) {
                hitLiteral.put(JSON_FIELD_DATATYPE_URI, "" + datatypeUri);
              }
              jHitStatement.put(JSON_FIELD_LITERAL, hitLiteral);
            } else { // rdf node is not a literal
              if (hitObject.isAnon()) {
                jHitStatement.put(JSON_FIELD_IS_BLANK, true);
              }
              jHitStatement.put(JSON_FIELD_OBJECT, "" + hitObject);
            }
            if (hitStatement.getSubjParent() != null) {
              final String parentSubj = URIUtil.encodeQuery(hitStatement.getSubjParent().toString());
              jHitStatement.put(JSON_FIELD_PARENT_SUBJECT, parentSubj);
            }
            if (hitStatement.getPredParent() != null) {
              final String parentPred = URIUtil.encodeQuery(hitStatement.getPredParent().toString());
              jHitStatement.put(JSON_FIELD_PARENT_PREDICATE, parentPred);
            }
            if (hitStatement.getResultType().equals(MdSystemResultType.LITERAL_DEFAULT_GRAPH) || hitStatement.getResultType().equals(MdSystemResultType.LITERAL_NAMED_GRAPH)) {
              jHitStatement.put(JSON_FIELD_SCORE, hitStatement.getScore());
            }
            if (hitStatement.getResultType() != null) {
              jHitStatement.put(JSON_FIELD_RESULT_TYPE, "" + hitStatement.getResultType());
            }
            jHitStatements.add(jHitStatement);
          }
          /*
           * ....:::::::::::::::::::::::::::::....
           */
          jHitGraph.put(JSON_FIELD_STATEMENTS, jHitStatements);
          jhitGraphes.add(jHitGraph);
        } catch (final Exception e) {
          e.printStackTrace();
        }
        /*
         * ...::::::::::::::::::::::::::::::::::::::::::::::::::::::::...
         */
      }
      /*
       * ..::::::::::::::::::::..
       */
      jsonEncoder.putJsonObj(JSON_FIELD_MD_HITS, jhitGraphes);
      final String jsonString = JSONValue.toJSONString(jsonEncoder.getJsonObject());
      out.println(jsonString); // response

    }
    else if (resultContainer != null && outputFormat.equals("json") &&request.getParameter(IS_PROJECT_ID).equals("true")) {
      response.setContentType("application/json"); // indicates that this content is pure json
      final WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.putStrings(JSON_FIELD_SEARCH_TERM, query);
      jsonEncoder.putStrings(JSON_FIELD_NUMBER_OF_HITS, resultContainer.size() + " ");
    }
    /*
     * ..:::::::::::..
     */
    logger.info("elapsedTime : " + elapsedTime);
    logger.info("begin json");
    HashMap<String, HashMap<String, Object>> wrapper = new HashMap<String, HashMap<String, Object>>();
    wrapper.put(query, mainEles);
    out.println(JSONValue.toJSONString(wrapper));
  }

  /**
   * Handle a concept query.
   * 
   * @param request
   * @param response
   * @param out
   * @param logger
   * @param query
   * @param outputFormat
   *          String, 'json' or 'html'
   * @param begin
   * @param mdQueryHandler
   * @throws URIException
   */
  private void handleConceptQuery(final HttpServletRequest request, final HttpServletResponse response, final PrintWriter out, final Logger logger, final String query, final String outputFormat, final Date begin, final MdSystemQueryHandler mdQueryHandler, final String baseUrl) throws URIException {
    final List<ConceptQueryResult> conceptHits = mdQueryHandler.getConcept(query);
    final Date end = new Date();
    final long elapsedTime = end.getTime() - begin.getTime();
    logger.info("elapsedTime : " + elapsedTime);
    logger.info("begin json");

    /*
     * ..:: show json ::..
     */
    if (outputFormat.equals("json") && conceptHits != null) {
      response.setContentType("application/json"); // indicates that this
                                                   // content is pure json
      final WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.clear();
      jsonEncoder.putStrings(JSON_FIELD_SEARCH_TERM, query);
      jsonEncoder.putStrings(JSON_FIELD_NUMBER_OF_HITS, String.valueOf(conceptHits.size()));
      final JSONArray jsonOuterArray = new JSONArray();
      JSONObject jsonWrapper = null;
      for (int i = 0; i < conceptHits.size(); i++) {
        final Set<String> keys = conceptHits.get(i).getAllMDFields();
        final JSONArray jsonInnerArray = new JSONArray();
        final JSONObject simpleHitInfo = new JSONObject();
        simpleHitInfo.put(PRIORITY, "" + conceptHits.get(i).getResultPriority());
        jsonInnerArray.add(simpleHitInfo);
        for (final String s : keys) {
          jsonWrapper = new JSONObject();
          jsonWrapper.put(s, conceptHits.get(i).getValue(s));
          jsonInnerArray.add(jsonWrapper);
        }
        jsonOuterArray.add(jsonInnerArray);
      }
//      JSONArray statArray = new JSONArray();
//      JsonObject statistics = new JsonObject();
//      statistics.put("totalNumberOfTriple : ", mdQueryHandler.getTripleCount().toString());
//      statistics.put("totalNumberOfGraphs : ", mdQueryHandler.getNumberOfGraphs());
//      statArray.add(statistics);
//      jsonOuterArray.add(statArray);
      jsonEncoder.putJsonObj(JSON_FIELD_MD_HITS, jsonOuterArray);

      final String jsonString = JSONValue.toJSONString(jsonEncoder.getJsonObject());
      logger.info(jsonString);

      out.println(jsonString); // response
    }
    /*
     * ..:::::::::::::::::..
     */

    /*
     * ..:: show html ::..
     */
    if (outputFormat.equals("html") && conceptHits != null) {
      final StringBuilder htmlStrBuilder = new StringBuilder();
      final String cssUrl = request.getContextPath() + "/css/page.css";
      htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
      htmlStrBuilder.append("<html>");
      htmlStrBuilder.append("\n\t<head>");
      htmlStrBuilder.append("\n\t\t<title>Query: " + query + "</title>");
      htmlStrBuilder.append("\n\t\t<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/>");
      htmlStrBuilder.append("\n\t</head>");
      htmlStrBuilder.append("\n\t<body>");
      htmlStrBuilder.append("\n\t\t<table align=\"right\" valign=\"top\">");
      htmlStrBuilder.append("\n\t\t<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW CMS service\"/></a></td>");
      htmlStrBuilder.append("\n\t\t</table>");
      htmlStrBuilder.append("\n\t\t<p><strong>Search term:</strong> " + query + "</p>");
      htmlStrBuilder.append("\n\t\t<p><strong>Number of hits:</strong> " + conceptHits.size() + "</p>");
//      htmlStrBuilder.append("\n\t\t<p>Total Number of Triple :"+mdQueryHandler.getTripleCount().toString()+"</p>");
//      htmlStrBuilder.append("\n\t\t<p>Total Number of Graphs :"+mdQueryHandler.getNumberOfGraphs().toString()+"</p>");
      htmlStrBuilder.append("\n\t\t<ul>");
      // for (int i = 0; i < conceptHits.size(); i++) {
      // final ConceptQueryResult conceptHit = conceptHits.get(i);
      // final Set<String> mdFields = conceptHit.getAllMDFields();
      //
      // }
      int counter = 0;

      for (final ConceptQueryResult conceptHit : conceptHits) {
        htmlStrBuilder.append("\n\t\t\t<li><strong>ConceptHit #" + (++counter) + "</strong>");

        htmlStrBuilder.append("<p>Result priority:" + conceptHit.getResultPriority() + "</p>");
        htmlStrBuilder.append("\n\t\t\t\t<ul>");
        for (final String mdField : conceptHit.getAllMDFields()) {
          final ArrayList<String> detailedSearchLinkList = new ArrayList<String>();
          final int size = conceptHit.getValue(mdField).size();

          if (size > 1) {
            final ArrayList<String> values = conceptHit.getValue(mdField);
            for (final String value : values) {
              final String detailedSearchLink = baseUrl + "/query/QueryMdSystem?query=" + URIUtil.encodeQuery(value) + "&detailedSearch=true&outputFormat=html";
              
              final String nameAndLink = "<a href=\"" + detailedSearchLink + "\">" + value + "</a>";
              detailedSearchLinkList.add(nameAndLink);
            }
          } else if (size == 1) {
            final String value = conceptHit.getValue(mdField).get(0);
            final String detailedSearchLink = baseUrl + "/query/QueryMdSystem?query=" + URIUtil.encodeQuery(value) + "&detailedSearch=true&outputFormat=html";

            final String nameAndLink = "<a href=\"" + detailedSearchLink + "\">" + value + "</a>";
            detailedSearchLinkList.add(nameAndLink);
          }
          htmlStrBuilder.append("\n\t\t\t\t\t<li>" + mdField + " : " + detailedSearchLinkList);
          htmlStrBuilder.append("</li>");
          // htmlStrBuilder.append("\n\t\t\t\t\t<li>" + mdField + " : " +
          // conceptHit.getValue(mdField));
          // htmlStrBuilder.append("</li>");
        }
        htmlStrBuilder.append("\n\t\t\t\t</ul>");
        htmlStrBuilder.append("\n\t\t\t</li>");
      }

      htmlStrBuilder.append("\n\t\t</ul>");
      htmlStrBuilder.append("\n\t</body>");
      htmlStrBuilder.append("\n</html>");
      out.println(htmlStrBuilder.toString()); // print html
    }
    /*
     * ..:::::::::::::::::..
     */
  }
  
  public String cutLiteralUri(String encodedLit){
   String litReady = null;
    if (encodedLit != null ){
      if(encodedLit.startsWith("http://")){
        //cut the url for gui readability reasons
        if(encodedLit.contains("%23")){
          int chAscii = encodedLit.lastIndexOf("%23");
          litReady = encodedLit.substring(chAscii+3);
        }else if(encodedLit.contains("#")){
          int chHash = encodedLit.lastIndexOf("#");
          litReady = encodedLit.substring(chHash+1);
        }else if(encodedLit.contains("/")){
          int chSlash = encodedLit.lastIndexOf('/');
          litReady = encodedLit.substring(chSlash+1);
        }//we dont want no nullpointer
        else{
          litReady = encodedLit;
        }
    }else{
      litReady = encodedLit;
      }
      if(encodedLit.contains("^^")){
        int beginIndex = encodedLit.indexOf("^^");
        litReady = encodedLit.substring(beginIndex);
      }
    }
    return litReady;
  }

  public String checkForLiteral(RDFNode resolved){
    final Logger logger = Logger.getLogger(QueryMdSystem.class);
    String obj = null;
    if(resolved instanceof Literal){
      obj = resolved.asLiteral().getLexicalForm();
    }else{
      obj = resolved.asResource().getLocalName();
    }
    return obj;
  }
  
  public static ISparqlAdapter useFuseki() {
    URL fusekiDatasetUrl;
    try {
      fusekiDatasetUrl = new URL("http://localhost:3030/ds");
      return SparqlAdapterFactory.getDefaultAdapter(fusekiDatasetUrl);
    } catch (final MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  public static ISparqlAdapter useJena() {
    final JenaMain jenamain = new JenaMain();
    try {
      jenamain.initStore();
      final IQueryStrategy<Map<URL, ResultSet>> queryStrategy = new QueryStrategyJena(jenamain);
      final ISparqlAdapter adapter = new SparqlAdapter<>(queryStrategy);
      return adapter;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
    // jenamain.makeDefaultGraphUnion();
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private String getBaseUrl(final HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(final HttpServletRequest request) {
    if ((request.getServerPort() == 80) || (request.getServerPort() == 443)) {
      return request.getScheme() + "://" + request.getServerName();
    } else {
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
  }
}