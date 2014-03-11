/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.component;


import java.util.concurrent.ExecutorService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.opengamma.financial.rest.AbstractRestfulJmsResultPublisher;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.jms.JmsConnector;

/**
 * RESTful resource for a streaming client. Results will be
 * streamed to the remote client over JMS.
 */
public class DataStreamingClientResource extends AbstractRestfulJmsResultPublisher {

  //CSOFF: just constants
  public static final String UNIQUE_ID_PATH = "uniqueId";
  public static final String IS_SERVER_STOPPED_PATH = "serverStopped";
  public static final String IS_SERVER_RUNNING_PATH = "serverRunning";
  //CSON: just constants

  /**
   * The underlying streaming client.
   */
  private final StreamingClient _client;

  /**
   * Creates the resource for the streaming client.
   *
   * @param client the streaming client, not null
   * @param jmsConnector the jms connector, not null
   * @param executor the executor to dispatch the streamed data, not null
   */
  public DataStreamingClientResource(StreamingClient client,
                                     JmsConnector jmsConnector,
                                     ExecutorService executor) {
    super(createJmsResultPublisher(client, jmsConnector), executor);
    _client = ArgumentChecker.notNull(client, "client");
  }


  private static StreamingClientJmsResultPublisher createJmsResultPublisher(StreamingClient client,
                                                                            JmsConnector jmsConnector) {
    return new StreamingClientJmsResultPublisher(client, OpenGammaFudgeContext.getInstance(), jmsConnector);
  }

  @Override
  protected boolean isTerminated() {
    return _client.isStopped();
  }

  @Override
  protected void expire() {
    // No heartbeats received so we stop the client
    stop();
  }

  @GET
  @Path(UNIQUE_ID_PATH)
  public Response getUniqueId() {
    updateLastAccessed();
    return responseOkObject(_client.getUniqueId());
  }

  @GET
  @Path(IS_SERVER_RUNNING_PATH)
  public Response isRunning() {
    updateLastAccessed();
    return responseOkObject(_client.isRunning());
  }

  @GET
  @Path(IS_SERVER_STOPPED_PATH)
  public Response isStopped() {
    updateLastAccessed();
    return responseOkObject(_client.isStopped());
  }

  @DELETE
  public void stop() {
    _client.stop();
    stopResultStream();
  }
}
