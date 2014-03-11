/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import org.fudgemsg.FudgeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.financial.rest.AbstractJmsResultPublisher;
import com.opengamma.sesame.engine.Results;
import com.opengamma.util.jms.JmsConnector;

/**
 * Publishes {@link StreamingClient} results over JMS.
 */
public class StreamingClientJmsResultPublisher extends AbstractJmsResultPublisher implements StreamingClientResultListener  {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(StreamingClientJmsResultPublisher.class);

  /**
   * The view client.
   */
  private final StreamingClient _streamingClient;

  /**
   * Creates an instance.
   *
   * @param streamingClient  the view client, not null
   * @param fudgeContext  the Fudge context, not null
   * @param jmsConnector  the JMS connector, not null
   */
  public StreamingClientJmsResultPublisher(StreamingClient streamingClient, FudgeContext fudgeContext, JmsConnector jmsConnector) {
    super(fudgeContext, jmsConnector);
    _streamingClient = streamingClient;
  }

  //-------------------------------------------------------------------------
  @Override
  protected void startListener() {
    s_logger.debug("Setting listener {} on client {}'s results", this, _streamingClient);
    _streamingClient.registerListener(this);
  }

  @Override
  protected void stopListener() {
    s_logger.debug("Removing listener {} on client {}'s results", this, _streamingClient);
    _streamingClient.stop();
  }

  @Override
  public void resultsReceived(final Results results) {
    send(new ResultsReceivedMessage(results));
  }

  @Override
  public void processCompleted() {
    send(ProcessCompletedMessage.INSTANCE);
  }

  @Override
  public void serverConnectionFailed(Exception e) {
    throw new UnsupportedOperationException("This method is only intended for use by the remote client");
  }
}
