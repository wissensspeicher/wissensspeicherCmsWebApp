package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.ConceptQueryResult;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.MdSystemQueryHandler;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.HitGraph;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.HitGraphContainer;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.HitStatement;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.IQueryStrategy;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.ISparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.QueryStrategyJena;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapter;
import org.bbaw.wsp.cms.mdsystem.metadata.mdqueryhandler.adapter.SparqlAdapterFactory;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.RdfHandler;
import org.bbaw.wsp.cms.servlets.util.WspJsonEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ResultSet;
import com.sun.org.apache.bcel.internal.generic.FCONST;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class QueryMdSystem extends HttpServlet {
  private static final String MD_HITS = "mdHits";
  private static final String NUMBER_OF_HITS = "numberOfHits";
  private static final String SEARCH_TERM = "searchTerm";
  private static final long serialVersionUID = 1L;

  public QueryMdSystem() {
    super();
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
  }

  // zum testen
  // http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=marx&conceptSearch=true&outputFormat=json
  // http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=marx&detailedSearch=true&outputFormat=json

  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
    // HTTP response print writer
    final PrintWriter out = response.getWriter();

    final Logger logger = Logger.getLogger(QueryMdSystem.class);
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    final String query = request.getParameter("query");
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
    } else if (outputFormat.equals("html") || outputFormat.equals("json")) {
      response.setContentType("text/html");
    } else {
      response.setContentType("text/xml");
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
      final MdSystemQueryHandler mdQueryHandler = MdSystemQueryHandler.getInstance();
      mdQueryHandler.init();
      logger.info("******************** ");

      if (conceptSearch != null && conceptSearch.equals("true")) {
        handleConceptQuery(request, response, out, logger, query, outputFormat, begin, mdQueryHandler);
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
    final ISparqlAdapter adapter = useFuseki();
    // final ISparqlAdapter adapter = useJena();
    final HitGraphContainer resultContainer = adapter.buildSparqlQuery("+" + query);
    final Date end = new Date();
    final long elapsedTime = end.getTime() - begin.getTime();
    /*
     * ..:: html ::..
     */
    if (outputFormat.equals("html")) {
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
        htmlStrBuilder.append("\n\t\t\t\t\t<li><strong>Average score:</strong>" + hitGraph.getAvgScore() + "</li>");
        htmlStrBuilder.append("\n\t\t\t\t\t<li><strong>Highest score:</strong>" + hitGraph.getHighestScore() + "</li>");
        for (final HitStatement hitStatement : hitGraph.getAllHitStatements()) {
          htmlStrBuilder.append("\n\t\t\t\t<li><ul>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Subject:</strong> " + hitStatement.getSubject() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Predicate:</strong> " + hitStatement.getPredicate() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Literal:</strong> " + hitStatement.getObject().asLiteral() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Parent subject:</strong> " + hitStatement.getSubjParent() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Parent predicate:</strong> " + hitStatement.getPredParent() + "</li>");
          htmlStrBuilder.append("\n\t\t\t\t\t\t<li><strong>Score:</strong> " + hitStatement.getScore() + "</li>");
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
    else if (outputFormat.equals("json")) {
      response.setContentType("application/json"); // indicates that this content is pure json
      final WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.putStrings(SEARCH_TERM, query);
      jsonEncoder.putStrings(NUMBER_OF_HITS, resultContainer.size() + " ");
      final JSONArray jResultContainers = new JSONArray();
      for (final HitGraph hitGraph : resultContainer.getAllHits()) {
        final JSONArray jHitGraphes = new JSONArray();
        final JSONObject avgScoreJsonObj = new JSONObject();
        avgScoreJsonObj.put("averageScore", hitGraph.getAvgScore());
        jHitGraphes.add(avgScoreJsonObj);
        final JSONObject maxScoreJsonObj = new JSONObject();
        maxScoreJsonObj.put("maximumScore", hitGraph.getHighestScore());
        jHitGraphes.add(maxScoreJsonObj);
        for (final HitStatement hitStatement : hitGraph.getAllHitStatements()) {
          final JSONObject jHitStatement = new JSONObject();
          jHitStatement.put("subject", hitStatement.getSubject());
          jHitStatement.put("predicate", hitStatement.getPredicate().toString());
          jHitStatement.put("literal", hitStatement.getObject().asLiteral());
          jHitStatement.put("parentSubject", hitStatement.getSubjParent());
          jHitStatement.put("parentPredicate", hitStatement.getPredParent());
          jHitStatement.put("score", hitStatement.getScore());
          jHitGraphes.add(jHitStatement);
        }
        jResultContainers.add(jHitGraphes);
      }
      jsonEncoder.putJsonObj(MD_HITS, jResultContainers);
      final String jsonString = JSONValue.toJSONString(jsonEncoder.getJsonObject());
      out.println(jsonString); // response

    }
    /*
     * ..:::::::::::..
     */
    logger.info("elapsedTime : " + elapsedTime);
    logger.info("begin json");
    logger.info("resultContainer.size() : " + resultContainer.size());
    for (final HitGraph hitGraph : resultContainer.getAllHits()) {
      logger.info("hitGraph : " + hitGraph);
    }
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
   */
  private void handleConceptQuery(final HttpServletRequest request, final HttpServletResponse response, final PrintWriter out, final Logger logger, final String query, final String outputFormat, final Date begin, final MdSystemQueryHandler mdQueryHandler) {
    final ArrayList<ConceptQueryResult> conceptHits = mdQueryHandler.getConcept(query);
    final Date end = new Date();
    final long elapsedTime = end.getTime() - begin.getTime();
    logger.info("elapsedTime : " + elapsedTime);
    logger.info("begin json");

    /*
     * ..:: show json ::..
     */
    if (outputFormat.equals("json") && conceptHits != null) {
      response.setContentType("application/json"); // indicates that this content is pure json
      final WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.clear();
      jsonEncoder.putStrings(SEARCH_TERM, query);
      jsonEncoder.putStrings(NUMBER_OF_HITS, String.valueOf(conceptHits.size()));
      final JSONArray jsonOuterArray = new JSONArray();
      JSONObject jsonWrapper = null;
      for (int i = 0; i < conceptHits.size(); i++) {
        logger.info("**************");
        logger.info("results.get(i) : " + conceptHits.get(i));
        logger.info("getSet() : " + conceptHits.get(i).getAllMDFields());
        final Set<String> keys = conceptHits.get(i).getAllMDFields();
        final JSONArray jsonInnerArray = new JSONArray();
        for (final String s : keys) {
          logger.info("concepts.get(i).getValue(s) : " + conceptHits.get(i).getValue(s));
          jsonWrapper = new JSONObject();
          jsonWrapper.put(s, conceptHits.get(i).getValue(s));
          jsonInnerArray.add(jsonWrapper);
        }
        logger.info("*******************");
        jsonOuterArray.add(jsonInnerArray);
      }

      jsonEncoder.putJsonObj(MD_HITS, jsonOuterArray);

      logger.info("end json");
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
      htmlStrBuilder.append("\n\t\t<ul>");
      // for (int i = 0; i < conceptHits.size(); i++) {
      // final ConceptQueryResult conceptHit = conceptHits.get(i);
      // final Set<String> mdFields = conceptHit.getAllMDFields();
      //
      // }
      int counter = 0;

      for (final ConceptQueryResult conceptHit : conceptHits) {
        htmlStrBuilder.append("\n\t\t\t<li><strong>ConceptHit #" + (++counter) + "</strong> is of type : "+conceptHit.getValue("type"));

        htmlStrBuilder.append("\n\t\t\t\t<ul>");
        for (final String mdField : conceptHit.getAllMDFields()) {
          htmlStrBuilder.append("\n\t\t\t\t\t<li>" + mdField + " : " + conceptHit.getValue(mdField));
          htmlStrBuilder.append("</li>");
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