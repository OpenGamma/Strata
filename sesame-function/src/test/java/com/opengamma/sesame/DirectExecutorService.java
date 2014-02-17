/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Executor service that uses the calling thread to run all tasks.
 * Nice and simple for unit tests.
 */
public class DirectExecutorService extends AbstractExecutorService {

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException("shutdown not supported");
  }

  @Override
  public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException("shutdownNow not supported");
  }

  @Override
  public boolean isShutdown() {
    throw new UnsupportedOperationException("isShutdown not supported");
  }

  @Override
  public boolean isTerminated() {
    throw new UnsupportedOperationException("isTerminated not supported");
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException("awaitTermination not supported");
  }

}
