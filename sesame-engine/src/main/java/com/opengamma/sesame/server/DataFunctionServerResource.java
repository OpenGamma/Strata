/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

import java.net.URI;
import java.util.List;

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
   * REST path for single cycle execution.
   */
  public static final String EXECUTE_SINGLE_CYCLE_PATH = "executeSingleCycle";

  /**
   * REST path for multiple cycle execution.
   */
  public static final String EXECUTE_MULTIPLE_CYCLES_PATH = "executeMultipleCycles";

  /**
   * The function server on which to execute a calculation request.
   */
  private final FunctionServer _server;

  /**
   * Creates the resource.
   *
   * @param server the function server on which to execute the request, not null
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
  @Path(EXECUTE_SINGLE_CYCLE_PATH)
  public Results executeSingleCycle(FunctionServerRequest<IndividualCycleOptions> request) {
    return _server.executeSingleCycle(request);
  }

  /**
   * Execute the request against the function server.
   *
   * @param request the request to be executed
   * @return the results of the execution
   */
  @POST
  @Path(EXECUTE_MULTIPLE_CYCLES_PATH)
  public List<Results> executeMultipleCycles(FunctionServerRequest<GlobalCycleOptions> request) {
    return _server.executeMultipleCycles(request);
  }

  /**
   * Retrieve the URI for the {@link #executeSingleCycle(FunctionServerRequest)} method.
   *
   * @param baseUri the base URI for all requests
   * @return the URI for the method
   */
  public static URI uriExecuteSingleCycle(URI baseUri) {
    return createUri(baseUri, EXECUTE_SINGLE_CYCLE_PATH);
  }

  /**
   * Retrieve the URI for the {@link #executeSingleCycle(FunctionServerRequest)} method.
   *
   * @param baseUri the base URI for all requests
   * @return the URI for the method
   */
  public static URI uriExecuteMultipleCycles(URI baseUri) {
    return createUri(baseUri, EXECUTE_MULTIPLE_CYCLES_PATH);
  }

  private static URI createUri(URI baseUri, String path) {
    final String fullPath = "/functionServer/" + path;
    return UriBuilder.fromUri(baseUri).path(fullPath).build();
  }
}
