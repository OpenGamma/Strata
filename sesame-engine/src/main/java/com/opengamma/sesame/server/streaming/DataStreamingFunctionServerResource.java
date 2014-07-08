/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.opengamma.financial.rest.RestfulJmsResultPublisherExpiryJob;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.GlobalCycleOptions;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.jms.JmsConnector;
import com.opengamma.util.rest.AbstractDataResource;

/**
 * RESTful resource for a FunctionServer which supports streaming of results.
 */
@Path("streamingFunctionServer")
public class DataStreamingFunctionServerResource extends AbstractDataResource {

  /**
   * Period after which, if no message has been received from the
   * client, the streaming process will be terminated and disposed of.
   */
  public static final long CLIENT_TIMEOUT_MILLIS = 30000;

  /**
   * The function server on which to execute a calculation request.
   */
  private final StreamingFunctionServer _server;

  /**
   * The JMS connector used for streaming result messages.
   */
  private final JmsConnector _jmsConnector;

  /**
   * The executor service.
   */
  private final ScheduledExecutorService _scheduledExecutor;

  /**
   * Map containing the clients who are consuming streaming results.
   */
  private final Map<UniqueId, DataStreamingClientResource> _streamingClients = new ConcurrentHashMap<>();

  /**
   * Creates the resource.
   *
   * @param server the function server on which to execute the request, not null
   * @param jmsConnector the JMS connection used for streaming results, not null
   * @param scheduledExecutor the executor, used for streaming results, not null
   */
  public DataStreamingFunctionServerResource(StreamingFunctionServer server,
                                             JmsConnector jmsConnector,
                                             ScheduledExecutorService scheduledExecutor) {
    _server = ArgumentChecker.notNull(server, "server");
    _scheduledExecutor = ArgumentChecker.notNull(scheduledExecutor, "scheduledExecutor");
    _jmsConnector = ArgumentChecker.notNull(jmsConnector, "jmsConnector");
    _scheduledExecutor.scheduleAtFixedRate(createExpiryJob(), CLIENT_TIMEOUT_MILLIS, CLIENT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
  }

  private Runnable createExpiryJob() {
    return new RestfulJmsResultPublisherExpiryJob<>(_streamingClients.values(), CLIENT_TIMEOUT_MILLIS);
  }

  /**
   *
   * @param uriInfo  the URI info
   * @param request  the request
   * @return the response
   */
  @POST
  @Path("clients")
  public Response createStreamingClient(@Context final UriInfo uriInfo, FunctionServerRequest<GlobalCycleOptions> request) {

    StreamingClient streamingClient = _server.createStreamingClient(request);
    UniqueId clientId = streamingClient.getUniqueId();
    _streamingClients.put(clientId, new DataStreamingClientResource(streamingClient, _jmsConnector, _scheduledExecutor));
    URI createdUri = uriClient(uriInfo.getRequestUri(), clientId);
    return responseCreated(createdUri);
  }

  @Path("clients/{streamingClientId}")
  public DataStreamingClientResource getStreamingClient(@Context final UriInfo uriInfo,
                                                        @PathParam("streamingClientId") final String streamingClientId) {

    // todo - null if it doesn't exist - is that a problem?
    return _streamingClients.get(UniqueId.parse(streamingClientId));
  }

  /**
   * Get the URI used to refer to an existing streaming client.
   *
   * @param baseUri the base URI of the REST service
   * @param streamingClientId the unique identifier of the client
   * @return the URI for the client
   */
  public static URI uriClient(URI baseUri, UniqueId streamingClientId) {
    return UriBuilder.fromUri(baseUri).segment(streamingClientId.toString()).build();
  }

  /**
   * Get the URI use to create a new streaming client.
   *
   * @param baseUri the base URI of the REST service
   * @return the URI to create the client
   */
  public static URI uriCreateStreamingClient(URI baseUri) {
    return UriBuilder.fromUri(baseUri).path("/streamingFunctionServer/clients").build();
  }
}
