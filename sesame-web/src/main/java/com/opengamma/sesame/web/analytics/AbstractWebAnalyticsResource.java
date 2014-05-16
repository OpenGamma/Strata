/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.analytics;

import org.joda.beans.impl.flexi.FlexiBean;

import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.web.AbstractPerRequestWebResource;

/**
 * Abstract base class for RESTful analytics resources.
 */
public abstract class AbstractWebAnalyticsResource
    extends AbstractPerRequestWebResource<WebAnalyticsData> {

  /**
   * HTML ftl directory
   */
  protected static final String HTML_DIR = "analytics/html/";

  /**
   * Creates the resource.
   * 
   * @param functionServer  the function server, not null
   */
  protected AbstractWebAnalyticsResource(final FunctionServer functionServer) {
    super(new WebAnalyticsData());
    ArgumentChecker.notNull(functionServer, "functionServer");
    data().setFunctionServer(functionServer);
  }

  /**
   * Creates the resource.
   * 
   * @param parent  the parent resource, not null
   */
  protected AbstractWebAnalyticsResource(final AbstractWebAnalyticsResource parent) {
    super(parent);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the output root data.
   * 
   * @return the output root data, not null
   */
  @Override
  protected FlexiBean createRootData() {
    FlexiBean out = super.createRootData();
    out.put("uris", new WebAnalyticsUris(data()));
    return out;
  }

}
