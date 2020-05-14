/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.concurrent;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * AutoCloseable wrapper around an executor.
 * <p>
 * This shuts down the wrapped executor when it is closed, and can wait for tasks to exit.
 */
public class CloseableExecutor implements AutoCloseable {

  /**
   * The wrapped executor service.
   */
  private final ExecutorService executorService;
  /**
   * The duration to wait for tasks to terminate.
   */
  private final Duration duration;

  /**
   * Restricted constructor.
   *
   * @param executorService  the underlying executor service
   * @param duration  the duration to wait
   */
  private CloseableExecutor(ExecutorService executorService, Duration duration) {
    this.executorService = ArgChecker.notNull(executorService, "executorService");
    this.duration = ArgChecker.notNull(duration, "duration");
    ArgChecker.isFalse(duration.isNegative(), "Duration to wait must be positive or zero: {}", duration);
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
    return new CloseableExecutor(executor, Duration.ZERO);
  }

  /**
   * Returns a closeable executor that wraps a passed in executor.
   * <p>
   * The passed in executor is shut down when the returned CloseableExecutor is closed, and waits for the given 
   * duration for tasks to finish.
   *
   * @param executor  the executor to wrap
   * @param duration  the duration to wait for tasks to exit
   * @return a CloseableExecutor
   */
  public static CloseableExecutor of(ExecutorService executor, Duration duration) {
    return new CloseableExecutor(executor, duration);
  }

  @Override
  public void close() {
    Unchecked.wrap(() -> {
      executorService.shutdown();
      if (!duration.isZero()) {
        executorService.awaitTermination(duration.toMillis(), TimeUnit.MILLISECONDS);
      }
    });
  }
}
