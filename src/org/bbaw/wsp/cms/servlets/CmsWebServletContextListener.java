package org.bbaw.wsp.cms.servlets;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.servlets.CmsWebServletContextListener;

import de.mpg.mpiwg.berlin.mpdl.lt.dict.db.LexHandler;
import de.mpg.mpiwg.berlin.mpdl.lt.morph.app.MorphologyCache;
import de.mpg.mpiwg.berlin.mpdl.xml.transform.FragmentTransformer;
import de.mpg.mpiwg.berlin.mpdl.xml.xquery.XQueryEvaluator;

import org.bbaw.wsp.cms.general.Constants;
import org.bbaw.wsp.cms.lucene.IndexHandler;
import org.bbaw.wsp.cms.scheduler.CmsChainScheduler;
import org.bbaw.wsp.cms.transform.XslResourceTransformer;
import org.bbaw.wsp.cms.transform.PageTransformer;

public class CmsWebServletContextListener implements ServletContextListener {
  private ServletContext context = null;
  private FragmentTransformer fragmentTransformer = null;
  private PageTransformer pageTransformer = null;
  private XslResourceTransformer highlightTransformer = null;
  private XQueryEvaluator xQueryEvaluator = null;  
  private static Logger LOGGER = Logger.getLogger(CmsWebServletContextListener.class);
  
  public void contextInitialized(ServletContextEvent event) {
    try {
      this.context = event.getServletContext();
      String webInfDir = context.getRealPath("/WEB-INF"); 
      de.mpg.mpiwg.berlin.mpdl.lt.general.Constants.getInstance(webInfDir);  // needed to initialize also the lt Constants
      Constants constants = Constants.getInstance(webInfDir);
      String documentsDirectory = constants.getDocumentsDir();
      String luceneDocumentsDirectory = constants.getLuceneDocumentsDir();
      String luceneNodesDirectory = constants.getLuceneNodesDir();
      context.setAttribute("documentDirectory", documentsDirectory);
      context.setAttribute("luceneDocumentsDirectory", luceneDocumentsDirectory);
      context.setAttribute("luceneNodesDirectory", luceneNodesDirectory);
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (documentsDirectory= \"" + documentsDirectory + "\", set in constants.properties)");
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (luceneDocumentsDirectory= \"" + luceneDocumentsDirectory + "\", set in constants.properties)");
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (luceneNodesDirectory= \"" + luceneNodesDirectory + "\", set in constants.properties)");
      fragmentTransformer = new FragmentTransformer();
      context.setAttribute("fragmentTransformer", fragmentTransformer);
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (fragmentTransformer)");
      pageTransformer = new PageTransformer();
      context.setAttribute("pageTransformer", pageTransformer);
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (pageTransformer)");
      highlightTransformer = new XslResourceTransformer("highlight.xsl");
      context.setAttribute("highlightTransformer", highlightTransformer);
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (highlightTransformer)");
      xQueryEvaluator = new XQueryEvaluator();
      context.setAttribute("xQueryEvaluator", xQueryEvaluator);
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextInitialized (xQueryEvaluator)");
    } catch (Exception e) {
      LOGGER.error(e);
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    try {
      this.context = null;
      LexHandler.getInstance().end();
      MorphologyCache.getInstance().end();
      IndexHandler.getInstance().end();
      CmsChainScheduler scheduler = CmsChainScheduler.getInstance();
      scheduler.end();
      Thread.sleep(1000);  // with this, also the scheduler worker threads could be closed
      LOGGER.info(CmsWebServletContextListener.class.getName() + ": contextDestroyed");
    } catch (Exception e) {
      LOGGER.error(e);
    }
  }
}