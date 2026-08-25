/*
 * Copyright (C) 2025 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ExtendedEnum} thread safety and initialization patterns in concurrent contexts.
 * <p>
 * These tests verify that ExtendedEnum can be safely initialized from various threading
 * constructs including ForkJoinPool, parallel streams, and CompletableFuture. This is
 * particularly important in production environments where complex classloader hierarchies
 * may cause initialization failures when the thread context classloader is unavailable.
 * <p>
 * <b>Important:</b> These JUnit tests run within Maven's Surefire plugin, which uses a
 * simplified classloader hierarchy where all dependencies (including Joda-Convert and
 * Strata modules) are loaded by the same classloader. This means these tests cannot
 * reproduce the production ClassNotFoundException reported in issue #2748.
 * <p>
 * The actual bug reproduction requires a separate JVM process with an isolated parent/child
 * classloader hierarchy. See {@link ExtendedEnumClassLoaderIsolationTest} which runs during
 * the Maven verify phase to reproduce the issue. That test creates the exact classloader
 * structure found in application servers (Tomcat, WebLogic) and OSGi containers where the
 * bug occurs.
 * <p>
 * These tests remain valuable for verifying thread-safety patterns and ensuring ExtendedEnum
 * works correctly in various concurrent execution contexts.
 * <p>
 * These tests use {@link ThreadSafeSampleNamed} to ensure clean initialization scenarios
 * independent of other tests in the suite.
 * 
 * @see <a href="https://github.com/OpenGamma/Strata/issues/2748">Issue #2748</a>
 */
public class ExtendedEnumThreadSafetyTest {

  /**
   * Test that ExtendedEnum can be initialized from within a ForkJoinPool.
   * <p>
   * This test verifies the pattern that fails in production: when DayCounts (or any ExtendedEnum)
   * is first accessed from a ForkJoinPool worker thread. However, this test cannot reproduce
   * issue #2748 because Maven's classloader hierarchy doesn't match production environments.
   * See the class-level javadoc for details on why {@link ExtendedEnumClassLoaderIsolationTest}
   * is needed.
   */
  @Test
  public void test_initialization_in_forkjoin_pool() throws Exception {
    ForkJoinPool pool = new ForkJoinPool(4);
    try {
      // Submit a task that accesses ExtendedEnum for the first time
      ForkJoinTask<ExtendedEnum<ThreadSafeSampleNamed>> task = pool.submit(
          () -> ExtendedEnum.of(ThreadSafeSampleNamed.class));
      
      ExtendedEnum<ThreadSafeSampleNamed> result = task.get(5, TimeUnit.SECONDS);
      
      // Verify the ExtendedEnum was properly initialized
      assertThat(result).isNotNull();
      assertThat(result.lookupAll()).isNotEmpty();
      assertThat(result.lookup("ThreadStandard")).isNotNull();
      assertThat(result.lookup("ThreadStandard").getName()).isEqualTo("ThreadStandard");
    } finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * Test that ExtendedEnum can be used in parallel streams.
   * <p>
   * Parallel streams use ForkJoinPool.commonPool() which has the same classloader issue.
   */
  @Test
  public void test_initialization_in_parallel_stream() {
    List<String> names = Arrays.asList("ThreadStandard", "ThreadMore", "ThreadStandard");
    
    // Use parallel stream which internally uses ForkJoinPool.commonPool()
    List<ThreadSafeSampleNamed> results = names.parallelStream()
        .map(name -> ThreadSafeSampleNamed.extendedEnum().lookup(name))
        .collect(Collectors.toList());
    
    assertThat(results).hasSize(3);
    assertThat(results.get(0).getName()).isEqualTo("ThreadStandard");
    assertThat(results.get(1).getName()).isEqualTo("ThreadMore");
    assertThat(results.get(2).getName()).isEqualTo("ThreadStandard");
  }

  /**
   * Test that ExtendedEnum can be initialized from CompletableFuture with ForkJoinPool.
   * <p>
   * CompletableFuture.supplyAsync() by default uses ForkJoinPool.commonPool().
   */
  @Test
  public void test_initialization_in_completable_future() throws Exception {
    // CompletableFuture.supplyAsync() uses ForkJoinPool.commonPool() by default
    CompletableFuture<ThreadSafeSampleNamed> future = CompletableFuture.supplyAsync(() -> {
      ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
      return enumInstance.lookup("ThreadStandard");
    });
    
    ThreadSafeSampleNamed result = future.get(5, TimeUnit.SECONDS);
    
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("ThreadStandard");
  }

  /**
   * Test that ExtendedEnum can be initialized from CompletableFuture with explicit ForkJoinPool.
   */
  @Test
  public void test_initialization_in_completable_future_with_custom_pool() throws Exception {
    ForkJoinPool pool = new ForkJoinPool(2);
    try {
      CompletableFuture<ThreadSafeSampleNamed> future = CompletableFuture.supplyAsync(() -> {
        ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
        return enumInstance.lookup("ThreadMore");
      }, pool);
      
      ThreadSafeSampleNamed result = future.get(5, TimeUnit.SECONDS);
      
      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo("ThreadMore");
    } finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * Test concurrent access from multiple threads in ForkJoinPool.
   * <p>
   * This tests that even under high concurrency, ExtendedEnum initialization
   * works correctly when accessed from ForkJoinPool threads.
   */
  @Test
  public void test_concurrent_access_in_forkjoin_pool() throws Exception {
    ForkJoinPool pool = new ForkJoinPool(10);
    try {
      List<Callable<ThreadSafeSampleNamed>> tasks = new ArrayList<>();
      
      // Create 100 concurrent tasks
      for (int i = 0; i < 100; i++) {
        String name = (i % 2 == 0) ? "ThreadStandard" : "ThreadMore";
        tasks.add(() -> ThreadSafeSampleNamed.extendedEnum().lookup(name));
      }
      
      // Submit all tasks and wait for completion
      List<Future<ThreadSafeSampleNamed>> futures = new ArrayList<>();
      for (Callable<ThreadSafeSampleNamed> task : tasks) {
        futures.add(pool.submit(task));
      }
      
      // Verify all completed successfully
      for (Future<ThreadSafeSampleNamed> future : futures) {
        ThreadSafeSampleNamed result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getName()).matches("Thread(Standard|More)");
      }
    } finally {
      pool.shutdown();
      pool.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  /**
   * Test that ExtendedEnum initialization works in regular ExecutorService.
   * <p>
   * This should work even before the fix, as regular ExecutorService threads
   * typically have the correct context classloader. This test serves as a
   * sanity check that the issue is specific to ForkJoinPool.
   */
  @Test
  public void test_initialization_in_executor_service() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(4);
    try {
      Future<ThreadSafeSampleNamed> future = executor.submit(() -> {
        ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
        return enumInstance.lookup("ThreadStandard");
      });
      
      ThreadSafeSampleNamed result = future.get(5, TimeUnit.SECONDS);
      
      assertThat(result).isNotNull();
      assertThat(result.getName()).isEqualTo("ThreadStandard");
    } finally {
      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * Test using parallel IntStream which also uses ForkJoinPool.
   */
  @Test
  public void test_initialization_in_parallel_int_stream() {
    List<ThreadSafeSampleNamed> results = IntStream.range(0, 50)
        .parallel()
        .mapToObj(i -> {
          ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
          return enumInstance.lookup(i % 2 == 0 ? "ThreadStandard" : "ThreadMore");
        })
        .collect(Collectors.toList());
    
    assertThat(results).hasSize(50);
    assertThat(results).allMatch(item -> item != null);
    assertThat(results).allMatch(item -> 
        "ThreadStandard".equals(item.getName()) || "ThreadMore".equals(item.getName()));
  }

  /**
   * Test simulating the real-world usage pattern from the issue report.
   * <p>
   * This simulates bond pricing calculations running in parallel via ForkJoinPool,
   * where each calculation needs to access ExtendedEnum (e.g., DayCount). This is
   * the exact usage pattern that triggered the ClassNotFoundException in production.
   * <p>
   * Note: Uses ThreadSafeSampleNamed instead of actual DayCount to keep test isolated.
   */
  @Test
  public void test_real_world_scenario_bond_pricing() throws Exception {
    ForkJoinPool pool = new ForkJoinPool(4);
    try {
      // Simulate processing multiple bonds in parallel
      List<String> bonds = Arrays.asList("Bond1", "Bond2", "Bond3", "Bond4");
      
      List<Future<String>> futures = new ArrayList<>();
      for (String bond : bonds) {
        futures.add(pool.submit(() -> calculateAccruedInterest(bond)));
      }
      
      // Verify all calculations completed
      for (Future<String> future : futures) {
        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result).contains("DayCount:");
      }
    } finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * Simulates a bond pricing calculation that uses ExtendedEnum.
   * <p>
   * When called from ForkJoinPool, this triggers ExtendedEnum initialization in a thread
   * where the context classloader may be null or inappropriate. In production, this would
   * be code like: {@code DayCount dayCount = DayCounts.ACT_360;}
   */
  private String calculateAccruedInterest(String bondId) {
    // Simulates the real production code path that caused the issue
    ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
    ThreadSafeSampleNamed dayCount = enumInstance.lookup("ThreadStandard");
    
    return bondId + " DayCount: " + dayCount.getName();
  }

  /**
   * Test that multiple ForkJoinPools can safely initialize different ExtendedEnum instances.
   */
  @Test
  public void test_multiple_pools_multiple_enums() throws Exception {
    ForkJoinPool pool1 = new ForkJoinPool(2);
    ForkJoinPool pool2 = new ForkJoinPool(2);
    
    try {
      // Pool 1 initializes ThreadSafeSampleNamed
      Future<ThreadSafeSampleNamed> future1 = pool1.submit(() -> {
        ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
        return enumInstance.lookup("ThreadStandard");
      });
      
      // Pool 2 also initializes ThreadSafeSampleNamed
      Future<ThreadSafeSampleNamed> future2 = pool2.submit(() -> {
        ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
        return enumInstance.lookup("ThreadMore");
      });
      
      // Both should succeed
      assertThat(future1.get(5, TimeUnit.SECONDS).getName()).isEqualTo("ThreadStandard");
      assertThat(future2.get(5, TimeUnit.SECONDS).getName()).isEqualTo("ThreadMore");
    } finally {
      pool1.shutdown();
      pool2.shutdown();
      pool1.awaitTermination(5, TimeUnit.SECONDS);
      pool2.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /**
   * Diagnostic test examining the thread context classloader (TCCL) in ForkJoinPool.
   * <p>
   * In production environments with complex classloader hierarchies (application servers,
   * OSGi), ForkJoinPool worker threads often have a null or inappropriate TCCL. This is
   * the root cause of the ClassNotFoundException reported in issue #2748 when RenameHandler
   * tries to load classes using the TCCL.
   * <p>
   * In this Maven test environment, the TCCL may not be null because all classes share
   * the same classloader, preventing reproduction of the issue.
   */
  @Test
  public void test_diagnostic_thread_context_classloader() throws Exception {
    ForkJoinPool pool = new ForkJoinPool(1);
    try {
      Future<ClassLoader> future = pool.submit(() -> Thread.currentThread().getContextClassLoader());
      
      ClassLoader tccl = future.get(5, TimeUnit.SECONDS);
      
      // In Maven's test environment, TCCL is typically not null, but in production
      // application servers and OSGi containers, it often is null or points to the
      // wrong classloader, causing ClassNotFoundException (issue #2748).
      
      // Verify ExtendedEnum initialization works in this test environment
      assertThatCode(() -> {
        ExtendedEnum<ThreadSafeSampleNamed> enumInstance = ExtendedEnum.of(ThreadSafeSampleNamed.class);
        enumInstance.lookup("ThreadStandard");
      }).doesNotThrowAnyException();
    } finally {
      pool.shutdown();
      pool.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}

