/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server.streaming;

import com.opengamma.sesame.engine.Results;

/**
 * Listener interface for use with a streaming client. Once the
 * listener is registered, the client will report progress via
 * this interface.
 */
public interface StreamingClientResultListener {

  /**
   * Called when results have been received from the streaming
   * client.
   *
   * @param results the results received, not null.
   */
  void resultsReceived(Results results);

  /**
   * Called when the streaming client has successfully produced all
   * the results it was originally asked to. No further results
   * will be produced after this message and the streaming client
   * will no longer be usable.
   */
  void processCompleted();

  /**
   * Called when the connection to the server process has been lost. No
   * further attempts will be made to reconnect.
   *
   * @param e the exception captured during the failure, may be null
   */
  void serverConnectionFailed(Exception e);


  /*
  These are the methods from the ViewResultListener from the old engine. Not all
   of them make sense for the new engine, but a number of them  are similar to
   what the new engine needs.

@Override
  public void viewDefinitionFailed(Instant valuationTime, Exception exception) {
    send(new ViewDefinitionCompilationFailedCall(valuationTime, exception));
  }

  @Override
  public void clientShutdown(Exception e) {
    send(new ClientShutdownCall(e));
  }

  @Override
  public void cycleStarted(ViewCycleMetadata cycleMetadata) {
    send(new CycleStartedCall(cycleMetadata));
  }

  @Override
  public void cycleCompleted(ViewComputationResultModel fullResult, ViewDeltaResultModel deltaResult) {
    send(new CycleCompletedCall(fullResult, deltaResult));
  }

  @Override
  public void cycleExecutionFailed(ViewCycleExecutionOptions executionOptions, Exception exception) {
    send(new CycleExecutionFailedCall(executionOptions, exception));
  }

  @Override
  public void processTerminated(boolean executionInterrupted) {
    send(new ProcessTerminatedCall(executionInterrupted));
  }

  @Override
  public void clientShutdown(Exception e) {
    send(new ClientShutdownCall(e));
  }*/
}
