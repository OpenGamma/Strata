/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.fail;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

@Test
public class ListenerWrapperTest {

  // Tests that a listener is only invoked by a single thread at any time even if multiple threads are
  // invoking the wrapper concurrently.
  public void concurrentExecution() throws InterruptedException {
    int nThreads = Runtime.getRuntime().availableProcessors();
    int resultsPerThread = 10;
    ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();
    CountDownLatch latch = new CountDownLatch(1);
    int expectedResultCount = nThreads * resultsPerThread;
    Listener listener = new Listener(errors, latch);
    Consumer<CalculationResults> wrapper = new ListenerWrapper(listener, expectedResultCount);
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    CalculationResult result = CalculationResult.of(0, 0, Result.failure(FailureReason.ERROR, "foo"));
    CalculationTarget target = new CalculationTarget() {};
    CalculationResults results = CalculationResults.of(target, ImmutableList.of(result));
    IntStream.range(0, expectedResultCount).forEach(i -> executor.submit(() -> wrapper.accept(results)));

    latch.await();
    executor.shutdown();

    if (!errors.isEmpty()) {
      String allErrors = errors.stream().collect(joining("\n"));
      fail(allErrors);
    }
  }

  public static final class Listener implements CalculationListener {

    /**
     * Calling fail() on a different thread from the one running the test won't cause the test to fail.
     * If any failures occur in the listener the failure message is put on this queue.
     * The test can check the queue at the end of the test and fail if it is non-empty.
     */
    private final Queue<String> errors;

    /** Latch that prevents the test method returning until all calculations have completed. */
    private final CountDownLatch latch;

    /** The name of the thread currently invoking this listener. */
    private volatile String threadName;

    public Listener(Queue<String> errors, CountDownLatch latch) {
      this.errors = errors;
      this.latch = latch;
    }

    @Override
    public void resultReceived(CalculationTarget target, CalculationResult result) {
      if (threadName != null) {
        errors.add("Expected threadName to be null but it was " + threadName);
      }
      threadName = Thread.currentThread().getName();

      try {
        // Give other threads a chance to get into this method
        Thread.sleep(5);
      } catch (InterruptedException e) {
        // Won't ever happen
      }
      threadName = null;
    }

    @Override
    public void calculationsComplete() {
      if (threadName != null) {
        errors.add("Expected threadName to be null but it was " + threadName);
      }
      threadName = Thread.currentThread().getName();

      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        // Won't ever happen
      }
      threadName = null;
      latch.countDown();
    }
  }
}
