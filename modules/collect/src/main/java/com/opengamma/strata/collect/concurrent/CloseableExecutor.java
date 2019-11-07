/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.concurrent;

import java.util.concurrent.ExecutorService;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * AutoCloseable wrapper around an executor.
 * <p>
 * This shuts down the wrapped executor when it is closed.
 */
public class CloseableExecutor implements AutoCloseable {

  /**
   * The wrapped executor service.
   */
  private final ExecutorService executorService;

  /**
   * Restricted constructor.
   *
   * @param executorService  the underlying executor service
   */
  private CloseableExecutor(ExecutorService executorService) {
    this.executorService = ArgChecker.notNull(executorService, "executorService");
  }

  /**
   * Returns a closeable executor that wraps a passed in executor.
   * <p>
   * The passed in executor is shut down when the returned CloseableExecutor is closed.
   *
   * @param executor  the executor to wrap
   * @return a CloseableExecutor
   */
  public static CloseableExecutor of(ExecutorService executor) {
    return new CloseableExecutor(executor);
  }

  @Override
  public void close() {
    Unchecked.wrap(executorService::shutdown);
  }
}
