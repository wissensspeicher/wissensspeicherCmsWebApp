package org.bbaw.wsp.cms.servlets.util;

import javax.servlet.http.HttpServletRequest;

import de.mpg.mpiwg.berlin.mpdl.exception.ApplicationException;

public class ServletUtil {
  private static ServletUtil instance;

  public static ServletUtil getInstance() throws ApplicationException {
    if (instance == null) {
      instance = new ServletUtil();
    }
    return instance;
  }

  public ServletUtil() {
  }

  public String getBaseUrl( HttpServletRequest request ) {
    if (request.getServerPort() == 80 || request.getServerPort() == 443)
      return request.getScheme() + "://" + request.getServerName() + request.getContextPath();
    else
      return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
  }
  
}
