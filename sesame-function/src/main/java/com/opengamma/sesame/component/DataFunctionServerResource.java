/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import com.opengamma.sesame.engine.Results;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.rest.AbstractDataResource;

/**
 * RESTful resource for a FunctionServer.
 */
@Path("functionServer")
public class DataFunctionServerResource extends AbstractDataResource {

  /**
   * The function server on which to execute the request.
   */
  private final FunctionServer _server;

  /**
   * Creates the resource.
   *
   * @param server  the function server on which to execute the request, not null
   */
  public DataFunctionServerResource(FunctionServer server) {
    _server = ArgumentChecker.notNull(server, "server");
  }

  /**
   * Execute the request against the function server.
   *
   * @param request the request to be executed
   * @return the results of the execution
   */
  @POST
  @Path("request")
  public Results executeOnce(FunctionServerRequest request) {
    return _server.executeOnce(request);
  }

  /**
   * Retrieve the URI for the {@link #executeOnce(FunctionServerRequest)} method.
   *
   * @param baseUri the base URI for all requests
   * @return the URI for the method
   */
  public static URI uriExecuteOnce(URI baseUri) {
    return UriBuilder.fromUri(baseUri).path("/functionServer/request").build();
  }
}
