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
import org.bbaw.wsp.cms.test.QueryMdSystemTest;
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
   * JSON field / key for a general object, might be anything although literals
   * (like blank nodes or resources)
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
   * JSON field / key for the literal which should have a datatype and lexical
   * type
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
  /**
   * The URI/name of the normdata graph as stored in the triple store.
   */
  private static final String NORMDATA_GRAPH_URI = "http://wsp.normdata.rdf/";
  /**
   * key/name for/of the parameter graphId.
   */
  private static final String PARAM_GRAPH_ID = "false";
  /**
   * key/name for/of the parameter subject.
   */
  private static final String PARAM_SUBJECT = "false";
  /**
   * key for the JSON attribute for the result priority.
   */
  private static final Object PRIORITY = "priority";
  /**
   * key/name for/of the parameter projectId.
   */
  private static final String IS_PROJECT_ID = "true";

  public  void init() {
    final Logger logger = Logger.getLogger(QueryMdSystemTest.class);
    logger.info("project information preloaded by sparql "); 
    String url = "http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=http://wsp.normdata.rdf/DTA&detailedSearch=true&outputFormat=json&isProjectId=true";
    try {
      doGet("false", "true", "http://wsp.normdata.rdf/DTA", "json", "http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void doGet(String conceptSearch, String detailedSearch, String query, String outputFormat, String baseUrl) throws IOException {

    final Logger logger = Logger.getLogger(QueryMdSystemTest.class);

    final Date begin = new Date();

    final MdSystemQueryHandler mdQueryHandler = MdSystemQueryHandler.getInstance();
    logger.info("******************** ");

    if (conceptSearch != null && conceptSearch.equals("true")) {
      handleConceptQuery(logger, query, outputFormat, begin, mdQueryHandler, baseUrl);
    }

    if (detailedSearch != null && detailedSearch.equals("true")) {
      handleDetailedSearch(logger, begin, outputFormat, query);
    }

  }

  private void handleDetailedSearch(final Logger logger, final Date begin, final String outputFormat, final String query) {
    logger.info("detailed Search");
    MdSystemQueryHandler mdqh = MdSystemQueryHandler.getInstance();
    ISparqlAdapter adapter = mdqh.getSparqlAdapter();
    // logger.info("preloadNormdata : "+preloadNormdata.toString());
    HitGraphContainer preloadNormdata = mdqh.preloadAllProjectInf();
    Collection<HitGraph> hitGraphsAsMaps = preloadNormdata.getAllHits();
    // @formatter:off
    /*
     * 
     * [&graphId=true][&subject=true] default: subject=true und defaultGraphName
     * = http://wsp.normdata.rdf/.... check, whether a parameter graphId is set.
     * If graphId is set to true, query contains a resource.
     */
    // @formatter:on
    HitGraphContainer resultContainer = null;
    HashMap<String, List<String>> thatVeryHit = null;
    HashMap<String, List<HashMap<String, String>>> mainEles = new HashMap<String, List<HashMap<String, String>>>();
    if (PARAM_GRAPH_ID != null && PARAM_GRAPH_ID.equals("true")) {
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
    } else if (PARAM_SUBJECT != null && PARAM_SUBJECT.equals("true")) {

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
    // query all project information and resolve uri
    if (IS_PROJECT_ID != null && IS_PROJECT_ID.equals("true")) {
      HitGraph thatVeryNormdata = null;
      try {
        thatVeryNormdata = preloadNormdata.getHitGraph(new URL("http://wsp.normdata.rdf/"));
        thatVeryHit = thatVeryNormdata.getStatementBySubject(query);
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }

      List<HashMap<String, String>> hasllist = new ArrayList<HashMap<String, String>>();
      HashMap<String, String> hash = new HashMap<String, String>();
      ArrayList<HashMap<String, String>> hashWrapper = new ArrayList<HashMap<String, String>>();
      for (Entry<String, List<String>> entry : thatVeryHit.entrySet()) {
        hash.put(entry.getKey(), entry.getValue().get(0));
      }
        // Person Project LinguisticSystem Organisation Location MediaType
        // PeriodOfTime ConferenceOrEvent
        if (thatVeryHit.get("contributor").size() != 0) {
          List<String> contributors = thatVeryHit.get("contributor");
          for (String string : contributors) {
            HashMap<String, String> resolvedValues = new HashMap<String, String>();
            HashMap<String, List<String>> resolved = thatVeryNormdata.getStatementBySubject(string);
            for (Entry<String, List<String>> entrie : resolved.entrySet()) {
              resolvedValues.put(entrie.getKey(), entrie.getValue().get(0));
            }
            logger.info("resolvedValues : " + resolvedValues);
//            if (resolvedValues.size() != 0) {
//            }
            hasllist.add(resolvedValues);
          }
        }
        hashWrapper.add(hash);
      mainEles.put("single Elements",hashWrapper);
      mainEles.put("contributors", hasllist);
      // resultContainer = adapter.buildSparqlQuery(query, true);
    } else { // neither graphId or subject was set
      // query contains the subject URI
      // call sparql adapter and query for the given subject within THE
      // NORMDATA.RDF
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
     * ..:::::::::::..
     */
    /*
     * ..:: json ::..
     */
    if (resultContainer != null && outputFormat.equals("json") && IS_PROJECT_ID == null) {
      logger.info(JSON_FIELD_SEARCH_TERM + " " + query);
      logger.info(JSON_FIELD_NUMBER_OF_HITS + " " + resultContainer.size() + " ");
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
      logger.info(JSON_FIELD_MD_HITS + " " + jhitGraphes);
      // final String jsonString = JSONValue.toJSONString();
      // logger.info(jsonString); // response

    } else if (resultContainer != null && outputFormat.equals("json") && IS_PROJECT_ID.equals("true")) {
      /*
       * ..:: hitGraphes: [ ... ::..
       */
      final JSONArray jhitGraphes = new JSONArray();
      //
    }

    /*
     * ..:::::::::::..
     */
    logger.info("elapsedTime : " + elapsedTime);
    logger.info("begin json");
    HashMap<String, HashMap<String, List<HashMap<String, String>>>> wrapper = new HashMap<String, HashMap<String, List<HashMap<String, String>>>>();
    wrapper.put(query, mainEles);
    logger.info(JSONValue.toJSONString(wrapper));
    logger.info("JSONValue.toJSONString(mainelements) : " + JSONValue.toJSONString(wrapper));
    // logger.info("resultContainer.size() : " + resultContainer.size());
    // for (final HitGraph hitGraph : resultContainer.getAllHits()) {
    // logger.info("hitGraph : " + hitGraph);
    // }
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
  private void handleConceptQuery(final Logger logger, final String query, final String outputFormat, final Date begin, final MdSystemQueryHandler mdQueryHandler, final String baseUrl) throws URIException {
    final List<ConceptQueryResult> conceptHits = mdQueryHandler.getConcept(query);
    final Date end = new Date();
    final long elapsedTime = end.getTime() - begin.getTime();
    logger.info("elapsedTime : " + elapsedTime);
    logger.info("begin json");

    /*
     * ..:: show json ::..
     */
    if (outputFormat.equals("json") && conceptHits != null) {
      final WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
      jsonEncoder.clear();
      jsonEncoder.putStrings(JSON_FIELD_SEARCH_TERM, query);
      jsonEncoder.putStrings(JSON_FIELD_NUMBER_OF_HITS, String.valueOf(conceptHits.size()));
      final JSONArray jsonOuterArray = new JSONArray();
      JSONObject jsonWrapper = null;
      for (int i = 0; i < conceptHits.size(); i++) {
        logger.info("**************");
        logger.info("results.get(i) : " + conceptHits.get(i));
        logger.info("getSet() : " + conceptHits.get(i).getAllMDFields());
        final Set<String> keys = conceptHits.get(i).getAllMDFields();
        final JSONArray jsonInnerArray = new JSONArray();
        final JSONObject simpleHitInfo = new JSONObject();
        simpleHitInfo.put(PRIORITY, "" + conceptHits.get(i).getResultPriority());
        jsonInnerArray.add(simpleHitInfo);
        for (final String s : keys) {
          logger.info("concepts.get(i).getValue(s) : " + conceptHits.get(i).getValue(s));
          jsonWrapper = new JSONObject();
          jsonWrapper.put(s, conceptHits.get(i).getValue(s));
          jsonInnerArray.add(jsonWrapper);
        }
        logger.info("*******************");
        jsonOuterArray.add(jsonInnerArray);
      }
      // JSONArray statArray = new JSONArray();
      // JsonObject statistics = new JsonObject();
      // statistics.put("totalNumberOfTriple : ",
      // mdQueryHandler.getTripleCount().toString());
      // statistics.put("totalNumberOfGraphs : ",
      // mdQueryHandler.getNumberOfGraphs());
      // statArray.add(statistics);
      // jsonOuterArray.add(statArray);
      jsonEncoder.putJsonObj(JSON_FIELD_MD_HITS, jsonOuterArray);

      logger.info("end json");
      final String jsonString = JSONValue.toJSONString(jsonEncoder.getJsonObject());
      logger.info(jsonString);

      logger.info(jsonString); // response
    }
  }

  public String cutLiteralUri(String encodedLit) {
    String litReady = null;
    if (encodedLit != null) {
      if (encodedLit.startsWith("http://")) {
        // cut the url for gui readability reasons
        if (encodedLit.contains("%23")) {
          int chAscii = encodedLit.lastIndexOf("%23");
          litReady = encodedLit.substring(chAscii + 3);
        } else if (encodedLit.contains("#")) {
          int chHash = encodedLit.lastIndexOf("#");
          litReady = encodedLit.substring(chHash + 1);
        } else if (encodedLit.contains("/")) {
          int chSlash = encodedLit.lastIndexOf('/');
          litReady = encodedLit.substring(chSlash + 1);
        }// we dont want no nullpointer
        else {
          litReady = encodedLit;
        }
      } else {
        litReady = encodedLit;
      }
      if (encodedLit.contains("^^")) {
        int beginIndex = encodedLit.indexOf("^^");
        litReady = encodedLit.substring(beginIndex);
      }
    }
    return litReady;
  }

  public String checkForLiteral(RDFNode resolved) {
    final Logger logger = Logger.getLogger(QueryMdSystemTest.class);
    String obj = null;
    if (resolved instanceof Literal) {
      obj = resolved.asLiteral().getLexicalForm();
    } else {
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