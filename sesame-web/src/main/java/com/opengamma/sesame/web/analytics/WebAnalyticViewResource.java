/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.analytics;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.joda.beans.impl.flexi.FlexiBean;

import com.opengamma.id.ObjectId;
import com.opengamma.sesame.engine.Results;

/**
 * RESTful resource for an analytic view.
 */
@Path("/analytics/views/{viewId}")
public class WebAnalyticViewResource extends AbstractWebAnalyticsResource {

  /**
   * Creates the resource.
   * @param parent  the parent resource, not null
   */
  public WebAnalyticViewResource(AbstractWebAnalyticsResource parent) {
    super(parent);
  }

  //-------------------------------------------------------------------------
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    Results results = data().getFunctionServer().executeSingleCycle(data().getCalculationRequest());
    FlexiBean out = createRootData();
    out.put("results", results);
    return getFreemarker().build(HTML_DIR + "view.ftl", out);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the output root data.
   * @return the output root data, not null
   */
  protected FlexiBean createRootData() {
    FlexiBean out = super.createRootData();
    return out;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a URI for this resource.
   * @param data  the data, not null
   * @return the URI, not null
   */
  public static URI uri(WebAnalyticsData data) {
    return uri(data, null);
  }

  /**
   * Builds a URI for this resource.
   * @param data  the data, not null
   * @param overrideViewId  the override view id, null uses information from data
   * @return the URI, not null
   */
  public static URI uri(WebAnalyticsData data, ObjectId overrideViewId) {
    String viewId = data.getBestViewUriId(overrideViewId);
    return data.getUriInfo().getBaseUriBuilder().path(WebAnalyticViewResource.class).build(viewId);
  }

}
