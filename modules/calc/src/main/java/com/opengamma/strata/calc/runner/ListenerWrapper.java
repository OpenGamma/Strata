/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.Column;
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

  /** This lock is never contended; it is used to guarantee the listener state is visible to all threads. */
  private final Lock listenerLock = new ReentrantLock();

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

  /** The number of task results that have been received. */
  private int tasksReceived;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance wrapping the specified listener.
   *  @param listener  the underlying listener wrapped by this object
   * @param tasksExpected  the number of tasks to be executed
   * @param columns  the columns for which values are being calculated
   */
  ListenerWrapper(CalculationListener listener, int tasksExpected, List<CalculationTarget> targets, List<Column> columns) {
    this.listener = ArgChecker.notNull(listener, "listener");
    this.tasksExpected = ArgChecker.notNegative(tasksExpected, "tasksExpected");

    listenerLock.lock();
    try {
      listener.calculationsStarted(targets, columns);

      if (tasksExpected == 0) {
        listener.calculationsComplete();
      }
    } finally {
      listenerLock.unlock();
    }
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
    CalculationResults nextResult;

    // Multiple calculation threads can try to acquire this lock at the same time.
    // The thread which acquires the lock will set the executing flag and proceed into
    // the body of the method.
    // If another thread acquires the lock while the first thread is executing it will
    // add an item to the queue and return.
    // The lock also ensures the state of the executing flag and the queue are visible
    // to any thread acquiring the lock.
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

    // The logic in the block above guarantees that there will never be more than one thread in the
    // rest of the method below this point.

    // Loop until the nextResult and all the results from the queue have been delivered
    for (;;) {
      // The logic above means this lock is never contended; the executing flag means
      // only one thread will ever be in this loop at any given time.
      // This lock is required to ensure any state changes in the listener are visible to all threads
      listenerLock.lock();
      try {
        // Invoke the listener while not protected by lock. This allows other threads
        // to queue results while this thread is delivering them to the listener.
        for (CalculationResult cell : nextResult.getCells()) {
          listener.resultReceived(nextResult.getTarget(), cell);
        }
      } catch (RuntimeException e) {
        log.warn("Exception invoking listener.resultReceived", e);
      } finally {
        listenerLock.unlock();
      }

      // The following code must be executed whilst holding the lock to guarantee any changes
      // to the executing flag and to the state of the queue are visible to all threads
      lock.lock();
      try {
        if (++tasksReceived == tasksExpected) {
          // The expected number of results have been received, inform the listener.
          // The listener lock must be acquired to ensure any state changes in the listener are
          // visible to all threads
          listenerLock.lock();
          try {
            listener.calculationsComplete();
          } catch (RuntimeException e) {
            log.warn("Exception invoking listener.calculationsComplete", e);
          } finally {
            listenerLock.unlock();
          }
          return;
        } else if (queue.isEmpty()) {
          // There are no more results to deliver. Unset the executing flag and return.
          // This allows the next calling thread to deliver results.
          executing = false;
          return;
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
  }
}
