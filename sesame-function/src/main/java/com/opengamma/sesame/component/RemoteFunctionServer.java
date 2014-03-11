/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;

import java.net.URI;
import java.util.List;

import com.opengamma.sesame.engine.Results;
import com.opengamma.util.rest.AbstractRemoteClient;

/**
 * Function server which executes view requests RESTfully.
 */
public class RemoteFunctionServer extends AbstractRemoteClient implements FunctionServer {

  /**
   * Creates an instance.
   *
   * @param uri the base target URI for all RESTful web services, not null
   */
  public RemoteFunctionServer(URI uri) {
    super(uri);
  }

  @Override
  public Results executeSingleCycle(FunctionServerRequest<IndividualCycleOptions> request) {

    URI uri = DataFunctionServerResource.uriExecuteSingleCycle(getBaseUri());
    return accessRemote(uri).post(Results.class, request);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Results> executeMultipleCycles(FunctionServerRequest<GlobalCycleOptions> request) {

    URI uri = DataFunctionServerResource.uriExecuteMultipleCycles(getBaseUri());
    return accessRemote(uri).post(List.class, request);
  }
}
