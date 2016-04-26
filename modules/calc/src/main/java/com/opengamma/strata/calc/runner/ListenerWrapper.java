/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Wrapper around a listener for thread-safety.
 * <p>
 * This is a wrapper around a {@link CalculationListener} that ensures the listener
 * is only invoked by a single thread at a time. When the calculations are complete,
 * it calls {@link CalculationListener#calculationsComplete() calculationsComplete}.
 * <p>
 * Calculations may be performed in bulk for a given target.
 * The logic in this class unwraps the {@link CalculationResults}, calling the
 * listener with each individual {@link CalculationResult}.
 */
final class ListenerWrapper implements Consumer<CalculationResults> {

  private static final Logger log = LoggerFactory.getLogger(ListenerWrapper.class);

  /** The wrapped listener. */
  private final CalculationListener listener;

  /** Queue of actions to perform on the delegate. */
  private final Queue<CalculationResults> queue = new LinkedList<>();

  /** Protects the queue and the executing flag. */
  private final Lock lock = new ReentrantLock();

  /** The total number of tasks to be executed. */
  private final int tasksExpected;

  // Mutable state -----------------------------------------------------

  /**
   * Flags whether a call to the underlying listener is executing.
   * If this flag is set when {@link #accept} is called, the result is added to
   * the queue and the calling thread returns. The executing thread will ensure
   * all queued results are delivered.
   */
  private boolean executing;

  /**
   * Flags whether the calculations are complete.
   * This is set when the number of results received equals {@link #tasksExpected}.
   * This causes a call to {@link CalculationListener#calculationsComplete()}.
   */
  private boolean complete;

  /** The number of task results that have been received. */
  private int tasksReceived;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance wrapping the specified listener.
   * 
   * @param listener  the underlying listener wrapped by this object
   * @param tasksExpected  the number of tasks to be executed
   */
  ListenerWrapper(CalculationListener listener, int tasksExpected) {
    this.listener = ArgChecker.notNull(listener, "listener");
    this.tasksExpected = ArgChecker.notNegativeOrZero(tasksExpected, "tasksExpected");
  }

  //-------------------------------------------------------------------------
  /**
   * Accepts a calculation result and delivers it to the listener
   * <p>
   * This method can be invoked concurrently by multiple threads.
   * Only one of them will invoke the listener directly to ensure that
   * it is not accessed concurrently by multiple threads.
   * <p>
   * The other threads do not block while the listener is invoked. They
   * add their results to a queue and return quickly. Their results are
   * delivered by the thread invoking the listener.
   *
   * @param result the result of a calculation
   */
  @Override
  public void accept(CalculationResults result) {
    // This is mutated while protected by the lock and accessed while not protected.
    // This is safe because the executing flag ensures the thread that accesses the
    // variable while unlocked is the same thread that set its value while guarded by the lock.
    CalculationResults nextResult;

    lock.lock();
    try {
      if (executing) {
        // Another thread is already invoking the listener. Add the result to
        // the queue and return. The other thread will ensure the queued results
        // are delivered.
        queue.add(result);
        return;
      } else {
        // There is no thread invoking the listener. Set the executing flag to
        // ensure no other thread passes this point and invoke the listener.
        executing = true;
        nextResult = result;
      }
    } finally {
      lock.unlock();
    }
    // Loop until the nextResult and all the results from the queue have been delivered
    for (;;) {
      try {
        // Invoke the listener while not protected by the lock. This allows other threads
        // to queue results while this thread is delivering them to the listener.
        for (CalculationResult cell : nextResult.getCells()) {
          listener.resultReceived(nextResult.getTarget(), cell);
        }
      } catch (RuntimeException e) {
        log.warn("Exception invoking listener.resultReceived", e);
      }
      lock.lock();
      try {
        if (++tasksReceived == tasksExpected) {
          // The expected number of results have been received. Set the complete
          // flag to trigger a call to listener.calculationsComplete after unlocking
          complete = true;
          break;
        } else if (queue.isEmpty()) {
          // There are no more results to deliver. Unset the executing flag and return.
          // This allows the next calling thread to deliver results.
          executing = false;
          break;
        } else {
          // There are results on the queue. This means another thread called accept(),
          // added a result to the queue and returned while this thread was invoking the listener.
          // This thread must deliver the results from the queue.
          nextResult = queue.remove();
        }
      } finally {
        lock.unlock();
      }
    }
    if (complete) {
      try {
        listener.calculationsComplete();
      } catch (RuntimeException e) {
        log.warn("Exception invoking listener.calculationsComplete", e);
      }
    }
  }
}
