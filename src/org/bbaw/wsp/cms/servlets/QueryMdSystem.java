package org.bbaw.wsp.cms.servlets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
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

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class QueryMdSystem extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public QueryMdSystem() {
    super();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
  }

  // zum testen
  // http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=marx&conceptSearch=true&outputFormat=json
  // http://localhost:8080/wspCmsWebApp/query/QueryMdSystem?query=marx&detailedSearch=true&outputFormat=json

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Logger logger = Logger.getLogger(QueryMdSystem.class);
    request.setCharacterEncoding("utf-8");
    response.setCharacterEncoding("utf-8");
    String query = request.getParameter("query");
    String language = request.getParameter("language");
    if (language != null && language.equals("none"))
      language = null;
    String outputFormat = request.getParameter("outputFormat");
    if (outputFormat == null)
      outputFormat = "html";
    if (outputFormat.equals("xml"))
      response.setContentType("text/xml");
    else if (outputFormat.equals("html") || outputFormat.equals("json"))
      response.setContentType("text/html");
    else 
      response.setContentType("text/xml");
    // Suche nach Konzepten in "Vorhaben-Metadaten"
    String conceptSearch = request.getParameter("conceptSearch");
    // Suche nach Begriffen in einzelnen Triples oder Named Graphen
    String detailedSearch = request.getParameter("detailedSearch");
    if (query == null) {
      logger.info("no query specified: please set parameter \"query\"");
      return;
    }
    try {
      Date begin = new Date();

      String baseUrl = getBaseUrl(request);
      MdSystemQueryHandler mdQueryHandler = MdSystemQueryHandler.getInstance();
      mdQueryHandler.init();
      logger.info("******************** ");

      if (conceptSearch != null && conceptSearch.equals("true")) {
        final ArrayList<ConceptQueryResult> conceptHits = mdQueryHandler.getConcept(query);
        Date end = new Date();
        long elapsedTime = end.getTime() - begin.getTime();
        logger.info("elapsedTime : " + elapsedTime);
        logger.info("begin json");

        if (outputFormat.equals("json") && conceptHits != null) {
          WspJsonEncoder jsonEncoder = WspJsonEncoder.getInstance();
          jsonEncoder.clear();
          jsonEncoder.putStrings("searchTerm", query);
          jsonEncoder.putStrings("numberOfHits", String.valueOf(conceptHits.size()));
          JSONArray jsonOuterArray = new JSONArray();
          JSONObject jsonWrapper = null;
          for (int i = 0; i < conceptHits.size(); i++) {
            logger.info("**************");
            logger.info("results.get(i) : " + conceptHits.get(i));
            logger.info("getSet() : " + conceptHits.get(i).getAllMDFields());
            Set<String> keys = conceptHits.get(i).getAllMDFields();
            JSONArray jsonInnerArray = new JSONArray();
            for (String s : keys) {
              logger.info("concepts.get(i).getValue(s) : " + conceptHits.get(i).getValue(s));
              jsonWrapper = new JSONObject();
              jsonWrapper.put(s, conceptHits.get(i).getValue(s));
              jsonInnerArray.add(jsonWrapper);
            }
            logger.info("*******************");
            jsonOuterArray.add(jsonInnerArray);
          }

          jsonEncoder.putJsonObj("mdHits", jsonOuterArray);

          logger.info("end json");
          logger.info(JSONValue.toJSONString(jsonEncoder.getJsonObject()));
        }
        if (outputFormat.equals("html") && conceptHits != null) {
          StringBuilder htmlStrBuilder = new StringBuilder();
          String cssUrl = request.getContextPath() + "/css/page.css";
          htmlStrBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
          htmlStrBuilder.append("<html>");
          htmlStrBuilder.append("<head>");
          htmlStrBuilder.append("<title>Query: " + query + "</title>");
          htmlStrBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssUrl + "\"/>");
          htmlStrBuilder.append("</head>");
          htmlStrBuilder.append("<body>");
          htmlStrBuilder.append("<table align=\"right\" valign=\"top\">");
          htmlStrBuilder.append("<td>[<i>This is a BBAW WSP CMS technology service</i>] <a href=\"/wspCmsWebApp/index.html\"><img src=\"/wspCmsWebApp/images/info.png\" valign=\"bottom\" width=\"15\" height=\"15\" border=\"0\" alt=\"BBAW CMS service\"/></a></td>");
          htmlStrBuilder.append("</table>");
          htmlStrBuilder.append("<p/>");
        }
      }
      if (detailedSearch != null && detailedSearch.equals("true")) {
        logger.info("detailed Search");
        final ISparqlAdapter adapter = useJena();
        final HitGraphContainer resultContainer = adapter.buildSparqlQuery("+marx");
        Date end = new Date();
        long elapsedTime = end.getTime() - begin.getTime();
        logger.info("elapsedTime : " + elapsedTime);
        logger.info("begin json");
        logger.info("resultContainer.size() : " + resultContainer.size());
        for (HitGraph hitGraph : resultContainer.getAllHits()) {
          logger.info("hitGraph : " + hitGraph);
        }
      }
    } catch (Exception e) {
      throw new ServletException(e);
    }
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
      final RdfHandler handler = new RdfHandler();
      final Dataset dataset = jenamain.getDataset();
      final IQueryStrategy<Map<URL, ResultSet>> queryStrategy = new QueryStrategyJena(handler, dataset);
      final ISparqlAdapter adapter = new SparqlAdapter<>(queryStrategy);
      return adapter;
    } catch (final ApplicationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
    // jenamain.makeDefaultGraphUnion();
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  private String getBaseUrl(HttpServletRequest request) {
    return getServerUrl(request) + request.getContextPath();
  }

  private String getServerUrl(HttpServletRequest request) {
    if ((request.getServerPort() == 80) || (request.getServerPort() == 443))
      return request.getScheme() + "://" + request.getServerName();
    else
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }
}