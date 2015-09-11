/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A simple wrapper around a {@link CalculationListener} that uses an executor service with a single thread to ensure
 * the listener is only invoked by a single thread at a time. It also calls
 * {@link CalculationListener#calculationsComplete() calculationsComplete} when all calculations have finished.
 */
final class SingleThreadListenerWrapper implements Consumer<CalculationResult> {

  private static final Logger log = LoggerFactory.getLogger(SingleThreadListenerWrapper.class);

  /** The wrapped listener. */
  private final CalculationListener listener;

  /** The total number of expected results. */
  private final int expectedResultCount;

  /** The executor that dispatches results to the listener. */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /** The number of results received. */
  private int resultCount;

  /**
   * @param listener  the wrapped listener
   * @param expectedResultCount  the total number of expected results
   */
  SingleThreadListenerWrapper(CalculationListener listener, int expectedResultCount) {
    this.listener = ArgChecker.notNull(listener, "listener");
    this.expectedResultCount = ArgChecker.notNegativeOrZero(expectedResultCount, "expectedResultCount");
  }

  @Override
  public void accept(CalculationResult calculationResult) {
    executor.execute(() -> deliverResult(calculationResult));
  }

  /**
   * Delivers a result to the listener and calls {@code calculationsComplete} if all results have been
   * received.
   *
   * @param calculationResult  the result
   */
  private void deliverResult(CalculationResult calculationResult) {
    try {
      listener.resultReceived(calculationResult);
    } catch (RuntimeException e) {
      log.warn("Exception invoking listener.resultReceived", e);
    }
    if (++resultCount == expectedResultCount) {
      try {
        listener.calculationsComplete();
      } catch (RuntimeException e) {
        log.warn("Exception invoking listener.calculationsComplete", e);
      }
      executor.shutdown();
    }
  }
}
