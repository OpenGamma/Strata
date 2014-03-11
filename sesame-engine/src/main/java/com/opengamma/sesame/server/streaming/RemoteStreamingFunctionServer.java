/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.core.Response;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.server.DataFunctionServerResource;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.GlobalCycleOptions;
import com.opengamma.sesame.server.IndividualCycleOptions;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.jms.JmsConnector;
import com.opengamma.util.rest.AbstractRemoteClient;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Function server which executes view requests RESTfully.
 */
public class RemoteStreamingFunctionServer extends AbstractRemoteClient implements StreamingFunctionServer {

  /**
   * JMS connector used for listening for streamed results.
   */
  private final JmsConnector _jmsConnector;

  /**
   * Executor service used to broadcast a heartbeat message so that the server
   * is aware that we are still expecting results.
   */
  private final ScheduledExecutorService _heartbeatScheduler;

  /**
   * Creates an instance.
   *
   * @param uri the base target URI for all RESTful web services, not null
   */
  public RemoteStreamingFunctionServer(URI uri, JmsConnector jmsConnector, ScheduledExecutorService heartbeatScheduler) {
    super(uri);
    _heartbeatScheduler = heartbeatScheduler;
    _jmsConnector = jmsConnector;
  }

  @Override
  public StreamingClient createStreamingClient(FunctionServerRequest request) {
    URI uri = DataStreamingFunctionServerResource.uriCreateStreamingClient(getBaseUri());
    ClientResponse response = getRestClient().accessFudge(uri).post(
        ClientResponse.class, ArgumentChecker.notNull(request, "request"));

    if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
      URI clientLocation = response.getLocation();
      return new RemoteStreamingClient(clientLocation, OpenGammaFudgeContext.getInstance(), _jmsConnector, _heartbeatScheduler);
    } else {
      throw new OpenGammaRuntimeException("Could not create streaming client: " + response);
    }
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
