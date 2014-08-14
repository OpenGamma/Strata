/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.service;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Callables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Runnables;

/**
 * Test {@link ServiceContextAwareExecutorService}.
 */
@Test
public class ServiceContextAwareExecutorServiceTest {

  public void test_submit_Callable() throws Exception {
    ServiceContextAwareExecutorService test = new ServiceContextAwareExecutorService(MoreExecutors.sameThreadExecutor());
    assertEquals(test.submit(Callables.returning("HelloWorld")).get(), "HelloWorld");
  }

  public void test_submit_Callable_bound() throws Exception {
    ServiceContextAwareExecutorService test = new ServiceContextAwareExecutorService(MoreExecutors.sameThreadExecutor());
    Callable<String> c = () -> {
      assertEquals(ServiceContext.getMap().contains(Number.class), true);
      return "HelloWorld";
    };
    ServiceContextMap context = ServiceContextMap.of(Number.class, Integer.valueOf(2));
    context.run(() -> {
      try {
        assertEquals(test.submit(c).get(), "HelloWorld");
      } catch (Exception ex) {
        // checked exceptions should be shot
      }
    });
  }

  public void test_submit_Callable_unbound() throws Exception {
    ServiceContextAwareExecutorService test = new ServiceContextAwareExecutorService(MoreExecutors.sameThreadExecutor());
    Callable<String> c = () -> {
      assertEquals(ServiceContext.getMap().contains(Number.class), false);
      return "HelloWorld";
    };
    assertEquals(test.submit(c).get(), "HelloWorld");
  }

  public void coverage() throws Exception {
    ServiceContextAwareExecutorService test = new ServiceContextAwareExecutorService(MoreExecutors.sameThreadExecutor());
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
    test.shutdown();
    test.shutdownNow();
    test.isShutdown();
    test.isTerminated();
    test.awaitTermination(1, TimeUnit.MICROSECONDS);
    test.toString();
  }

}
