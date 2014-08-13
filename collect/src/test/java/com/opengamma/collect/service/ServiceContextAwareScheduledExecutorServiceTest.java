/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.Runnables;

/**
 * Test {@link ServiceContextAwareScheduledExecutorService}.
 */
@Test
public class ServiceContextAwareScheduledExecutorServiceTest {

  public void test_submit_Callable() throws Exception {
    ServiceContextAwareScheduledExecutorService test =
        new ServiceContextAwareScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    assertEquals(test.submit(Callables.returning("HelloWorld")).get(), "HelloWorld");
  }

  public void test_submit_Callable_bound() throws Exception {
    ServiceContextAwareScheduledExecutorService test =
        new ServiceContextAwareScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    Callable<String> c = () -> {
      assertEquals(ServiceManager.getContext().contains(Number.class), true);
      return "HelloWorld";
    };
    ServiceContext context = ServiceContext.of(Number.class, Integer.valueOf(2));
    context.run(() -> {
      try {
        assertEquals(test.submit(c).get(), "HelloWorld");
      } catch (Exception ex) {
        // checked exceptions should be shot
      }
    });
  }

  public void test_submit_Callable_unbound() throws Exception {
    ServiceContextAwareScheduledExecutorService test =
        new ServiceContextAwareScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    Callable<String> c = () -> {
      assertEquals(ServiceManager.getContext().contains(Number.class), false);
      return "HelloWorld";
    };
    assertEquals(test.submit(c).get(), "HelloWorld");
  }

  public void coverage() throws Exception {
    ServiceContextAwareScheduledExecutorService test =
        new ServiceContextAwareScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
    Callable<Object> callable = Callables.returning("");
    Runnable runnable = Runnables.doNothing();
    test.submit(callable);
    test.submit(runnable, "");
    test.submit(runnable);
    test.invokeAll(ImmutableList.of(callable));
    test.invokeAll(ImmutableList.of(callable), 1, TimeUnit.MICROSECONDS);
    test.invokeAny(ImmutableList.of(callable));
    test.invokeAny(ImmutableList.of(callable), 1, TimeUnit.MICROSECONDS);
    test.execute(runnable);
    test.schedule(runnable, 1, TimeUnit.MICROSECONDS);
    test.schedule(callable, 1, TimeUnit.MICROSECONDS);
    test.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.MICROSECONDS);
    test.scheduleWithFixedDelay(runnable, 1, 1, TimeUnit.MICROSECONDS);
    test.shutdown();
    test.shutdownNow();
    test.isShutdown();
    test.isTerminated();
    test.awaitTermination(1, TimeUnit.MICROSECONDS);
    test.toString();
  }

}
