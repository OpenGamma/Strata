/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculation;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A simple wrapper around a {@link CalculationListener} that uses a lock to ensure the listener is only invoked
 * by a single thread at a time. It also calls {@link CalculationListener#calculationsComplete() calculationsComplete}
 * when all calculations have finished.
 */
final class LockingListenerWrapper implements Consumer<CalculationResult> {

  private static final Logger log = LoggerFactory.getLogger(LockingListenerWrapper.class);

  /** The wrapped listener. */
  private final CalculationListener listener;

  /** The total number of expected results. */
  private final int expectedResultCount;

  /** The lock that guards the listener. */
  private final Lock lock = new ReentrantLock();

  /** The number of results received. */
  private int resultCount;

  /**
   * @param listener  the wrapped listener
   * @param expectedResultCount  the number of results expected
   */
  LockingListenerWrapper(CalculationListener listener, int expectedResultCount) {
    this.listener = ArgChecker.notNull(listener, "listener");
    this.expectedResultCount = ArgChecker.notNegativeOrZero(expectedResultCount, "expectedResultCount");
  }

  @Override
  public void accept(CalculationResult calculationResult) {
    lock.lock();
    try {
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
      }
    } finally {
      lock.unlock();
    }
  }
}
