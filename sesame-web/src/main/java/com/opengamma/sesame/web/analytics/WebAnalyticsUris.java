/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.analytics;

import java.net.URI;

import com.opengamma.id.ObjectId;

/**
 * URIs for web-based analytics.
 */
public class WebAnalyticsUris {

  /**
   * The data.
   */
  private final WebAnalyticsData _data;

  /**
   * Creates an instance.
   * @param data  the web data, not null
   */
  public WebAnalyticsUris(WebAnalyticsData data) {
    _data = data;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the URI.
   * @return the URI
   */
  public URI base() {
    return views();
  }

  /**
   * Gets the URI.
   * @return the URI
   */
  public URI views() {
    return WebAnalyticsViewsResource.uri(_data);
  }

  /**
   * Gets the URI.
   * @return the URI
   */
  public URI view() {
    return WebAnalyticViewResource.uri(_data);
  }

  /**
   * Gets the URI.
   * @param objectId  the object identifier, not null
   * @return the URI
   */
  public URI view(ObjectId objectId) {
    return WebAnalyticViewResource.uri(_data, objectId);
  }

}
