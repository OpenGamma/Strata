/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

import javax.jms.JMSException;

import org.fudgemsg.FudgeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.financial.rest.AbstractRestfulJmsResultConsumer;
import com.opengamma.id.UniqueId;
import com.opengamma.util.jms.JmsConnector;

/**
 * A remote implementation of the streaming client. Allows results to
 * be received by a registered listener.
 */
public class RemoteStreamingClient extends AbstractRestfulJmsResultConsumer<StreamingClientResultListener>
    implements StreamingClient {

  /**
   * Logger for the class.
   */
  private static final Logger s_logger = LoggerFactory.getLogger(RemoteStreamingClient.class);

  /**
   * The result listener supplied by a client process on which they
   * are listening for results.
   */
  private StreamingClientResultListener _externalResultListener;

  public RemoteStreamingClient(URI clientLocation,
                               FudgeContext instance,
                               JmsConnector jmsConnector,
                               ScheduledExecutorService heartbeatScheduler) {
    super(clientLocation,
          instance,
          jmsConnector,
          heartbeatScheduler,
          DataStreamingFunctionServerResource.CLIENT_TIMEOUT_MILLIS / 2);
    new StreamingClientJmsResultPublisher(this, instance, jmsConnector);
  }

  @Override
  public void heartbeatFailed(Exception e) {
    super.heartbeatFailed(e);
    stopHeartbeating();

    if (_externalResultListener != null) {
      _externalResultListener.serverConnectionFailed(e);
    }
  }

  @Override
  public void stop() {

    try {
      decrementListenerDemand();
    } catch (JMSException e) {
      throw new OpenGammaRuntimeException("JMS error configuring result listener", e);
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Interrupted before result listener was configured", e);
    }
  }

  @Override
  public UniqueId getUniqueId() {
    URI uri = getUri(getBaseUri(), DataStreamingClientResource.UNIQUE_ID_PATH);
    return getClient().accessFudge(uri).get(UniqueId.class);
  }

  @Override
  public boolean isStopped() {
    return checkClientStatus(DataStreamingClientResource.IS_SERVER_STOPPED_PATH);
  }

  @Override
  public boolean isRunning() {
    return checkClientStatus(DataStreamingClientResource.IS_SERVER_RUNNING_PATH);
  }

  private boolean checkClientStatus(String path) {
    URI uri = getUri(getBaseUri(), path);
    return getClient().accessFudge(uri).get(Boolean.class);
  }

  @Override
  public void registerListener(StreamingClientResultListener listener) {

    if (_externalResultListener != null) {
      throw new IllegalStateException("Listener can only be registered once");
    }

    _externalResultListener = listener;
    try {
      incrementListenerDemand();
    } catch (JMSException e) {
      throw new OpenGammaRuntimeException("JMS error configuring result listener", e);
    } catch (InterruptedException e) {
      throw new OpenGammaRuntimeException("Interrupted before result listener was configured", e);
    }
  }

  @Override
  protected void dispatchListenerCall(Function<StreamingClientResultListener, ?> message) {
    // No null check required as we won't start streaming until we have a listener
    message.apply(_externalResultListener);
  }
}
