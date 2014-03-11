/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import com.opengamma.id.UniqueId;

/**
 * A streaming client is created by a {@link StreamingFunctionServer} and is
 * responsible for streaming the results of a calculation to a registered
 * listener. Once created, the client can be stopped by calling the
 * {@link #stop()} method. Once stopped a client cannot be restarted.
 */
public interface StreamingClient {

  /**
   * Gets the unique identifier for the streaming client.
   *
   * @return the unique identifier, not null
   */
  UniqueId getUniqueId();

  /**
   * Register the supplied listener to receive streaming results.
   *
   * @param listener the listener to send results to, not null
   */
  void registerListener(StreamingClientResultListener listener);

  /**
   * Stop the streaming client and clean up all resources associated
   * with it. Once stopped, there is no way to restart a client.
   */
  void stop();

  /**
   * Indicates if the client has been stopped. It may have been stopped
   * due to errors, the completion of all results, or user request.
   *
   * @return true if the client has stopped
   */
  boolean isStopped();

  /**
   * Indicates if the client is running. If the client is running, it
   * will only be streaming results if a listener has been registered with it.
   *
   * @return true if the client is running
   */
  boolean isRunning();
}
