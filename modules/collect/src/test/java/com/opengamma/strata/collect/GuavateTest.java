/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.Guavate.entriesToImmutableMap;
import static com.opengamma.strata.collect.Guavate.pairsToImmutableMap;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.opengamma.strata.collect.tuple.ObjIntPair;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test Guavate.
 */
@Test
public class GuavateTest {

  //-------------------------------------------------------------------------
  public void test_concatToList() {
    Iterable<String> iterable1 = Arrays.asList("a", "b", "c");
    Iterable<String> iterable2 = Arrays.asList("d", "e", "f");
    List<String> test = Guavate.concatToList(iterable1, iterable2);
    assertEquals(test, ImmutableList.of("a", "b", "c", "d", "e", "f"));
  }

  public void test_concatToList_differentTypes() {
    Iterable<Integer> iterable1 = Arrays.asList(1, 2, 3);
    Iterable<Double> iterable2 = Arrays.asList(10d, 20d, 30d);
    ImmutableList<Number> test = Guavate.concatToList(iterable1, iterable2);
    assertEquals(test, ImmutableList.of(1, 2, 3, 10d, 20d, 30d));
  }

  //-------------------------------------------------------------------------
  public void test_firstNonEmpty_supplierMatch1() {
    Optional<Number> test = Guavate.firstNonEmpty(
        () -> Optional.of(Integer.valueOf(1)),
        () -> Optional.of(Double.valueOf(2d)));
    assertEquals(test, Optional.of(Integer.valueOf(1)));
  }

  public void test_firstNonEmpty_supplierMatch2() {
    Optional<Number> test = Guavate.firstNonEmpty(
        () -> Optional.empty(),
        () -> Optional.of(Double.valueOf(2d)));
    assertEquals(test, Optional.of(Double.valueOf(2d)));
  }

  public void test_firstNonEmpty_supplierMatchNone() {
    Optional<Number> test = Guavate.firstNonEmpty(
        () -> Optional.empty(),
        () -> Optional.empty());
    assertEquals(test, Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_firstNonEmpty_optionalMatch1() {
    Optional<Number> test = Guavate.firstNonEmpty(Optional.of(Integer.valueOf(1)), Optional.of(Double.valueOf(2d)));
    assertEquals(test, Optional.of(Integer.valueOf(1)));
  }

  public void test_firstNonEmpty_optionalMatch2() {
    Optional<Number> test = Guavate.firstNonEmpty(Optional.empty(), Optional.of(Double.valueOf(2d)));
    assertEquals(test, Optional.of(Double.valueOf(2d)));
  }

  public void test_firstNonEmpty_optionalMatchNone() {
    Optional<Number> test = Guavate.firstNonEmpty(Optional.empty(), Optional.empty());
    assertEquals(test, Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_stream_Iterable() {
    Iterable<String> iterable = Arrays.asList("a", "b", "c");
    List<String> test = Guavate.stream(iterable)
        .collect(Collectors.toList());
    assertEquals(test, ImmutableList.of("a", "b", "c"));
  }

  public void test_stream_Optional() {
    Optional<String> optional = Optional.of("foo");
    List<String> test1 = Guavate.stream(optional).collect(Collectors.toList());
    assertEquals(test1, ImmutableList.of("foo"));

    Optional<String> empty = Optional.empty();
    List<String> test2 = Guavate.stream(empty).collect(Collectors.toList());
    assertEquals(test2, ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  public void test_zipWithIndex() {
    Stream<String> base = Stream.of("a", "b", "c");
    List<ObjIntPair<String>> test = Guavate.zipWithIndex(base).collect(Collectors.toList());
    assertEquals(test, ImmutableList.of(ObjIntPair.of("a", 0), ObjIntPair.of("b", 1), ObjIntPair.of("c", 2)));
  }

  public void test_zipWithIndex_empty() {
    Stream<String> base = Stream.of();
    List<ObjIntPair<String>> test = Guavate.zipWithIndex(base).collect(Collectors.toList());
    assertEquals(test, ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  public void test_zip() {
    Stream<String> base1 = Stream.of("a", "b", "c");
    Stream<Integer> base2 = Stream.of(1, 2, 3);
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertEquals(test, ImmutableList.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3)));
  }

  public void test_zip_firstLonger() {
    Stream<String> base1 = Stream.of("a", "b", "c");
    Stream<Integer> base2 = Stream.of(1, 2);
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertEquals(test, ImmutableList.of(Pair.of("a", 1), Pair.of("b", 2)));
  }

  public void test_zip_secondLonger() {
    Stream<String> base1 = Stream.of("a", "b");
    Stream<Integer> base2 = Stream.of(1, 2, 3);
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertEquals(test, ImmutableList.of(Pair.of("a", 1), Pair.of("b", 2)));
  }

  public void test_zip_empty() {
    Stream<String> base1 = Stream.of();
    Stream<Integer> base2 = Stream.of();
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertEquals(test, ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  public void test_not_Predicate() {
    List<String> data = Arrays.asList("a", "", "c");
    List<String> test = data.stream()
        .filter(Guavate.not(String::isEmpty))
        .collect(Collectors.toList());
    assertEquals(test, ImmutableList.of("a", "c"));
  }

  //-------------------------------------------------------------------------
  public void test_ensureOnlyOne() {
    assertEquals(Stream.empty().reduce(Guavate.ensureOnlyOne()), Optional.empty());
    assertEquals(Stream.of("a").reduce(Guavate.ensureOnlyOne()), Optional.of("a"));
    assertThrowsIllegalArg(() -> Stream.of("a", "b").reduce(Guavate.ensureOnlyOne()));
  }

  //-------------------------------------------------------------------------
  public void test_toImmutableList() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableList<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableList());
    assertEquals(test, ImmutableList.of("a", "b", "c", "a"));
  }

  public void test_toImmutableSet() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSet<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableSet());
    assertEquals(test, ImmutableSet.of("a", "b", "c"));
  }

  public void test_toImmutableSortedSet() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSortedSet<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableSortedSet());
    assertEquals(test, ImmutableSortedSet.of("a", "b", "c"));
  }

  public void test_toImmutableSortedSet_comparator() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSortedSet<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableSortedSet(Ordering.natural().reverse()));
    assertEquals(test, ImmutableSortedSet.reverseOrder().add("a").add("b").add("c").build());
  }

  public void test_toImmutableMultiset() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableMultiset<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableMultiset());
    assertEquals(test, ImmutableMultiset.of("a", "a", "b", "c"));
  }

  //-------------------------------------------------------------------------
  public void test_toImmutableMap_key() {
    List<String> list = Arrays.asList("a", "ab", "bob");
    ImmutableMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableMap(s -> s.length()));
    assertEquals(test, ImmutableMap.builder().put(1, "a").put(2, "ab").put(3, "bob").build());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_toImmutableMap_key_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    list.stream().collect(Guavate.toImmutableMap(s -> s.length()));
  }

  public void test_toImmutableMap_mergeFn() {
    List<String> list = Arrays.asList("b", "a", "b", "b", "c", "a");
    Map<String, Integer> result = list.stream()
        .collect(Guavate.toImmutableMap(s -> s, s -> 1, (s1, s2) -> s1 + s2));
    Map<String, Integer> expected = ImmutableMap.of("a", 2, "b", 3, "c", 1);
    assertEquals(result, expected);
    Iterator<String> iterator = result.keySet().iterator();
    assertEquals(iterator.next(), "b");
    assertEquals(iterator.next(), "a");
    assertEquals(iterator.next(), "c");
  }

  public void test_toImmutableMap_keyValue() {
    List<String> list = Arrays.asList("a", "ab", "bob");
    ImmutableMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableMap(s -> s.length(), s -> "!" + s));
    assertEquals(test, ImmutableMap.builder().put(1, "!a").put(2, "!ab").put(3, "!bob").build());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_toImmutableMap_keyValue_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    list.stream().collect(Guavate.toImmutableMap(s -> s.length(), s -> "!" + s));
  }

  //-------------------------------------------------------------------------
  public void test_toImmutableSortedMap_key() {
    List<String> list = Arrays.asList("bob", "a", "ab");
    ImmutableSortedMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSortedMap(s -> s.length()));
    assertEquals(test, ImmutableSortedMap.naturalOrder().put(1, "a").put(2, "ab").put(3, "bob").build());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_toImmutableSortedMap_key_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "c", "bb", "b", "a");
    list.stream().collect(Guavate.toImmutableSortedMap(s -> s.length()));
  }

  public void test_toImmutableSortedMap_keyValue() {
    List<String> list = Arrays.asList("bob", "a", "ab");
    ImmutableSortedMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSortedMap(s -> s.length(), s -> "!" + s));
    assertEquals(test, ImmutableSortedMap.naturalOrder().put(1, "!a").put(2, "!ab").put(3, "!bob").build());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_toImmutableSortedMap_keyValue_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "c", "bb", "b", "a");
    list.stream().collect(Guavate.toImmutableSortedMap(s -> s.length(), s -> "!" + s));
  }

  //-------------------------------------------------------------------------
  public void test_toImmutableListMultimap_key() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableListMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableListMultimap(s -> s.length()));
    ImmutableListMultimap<Object, Object> expected = ImmutableListMultimap.builder()
        .put(1, "a").put(2, "ab").put(1, "b").put(2, "bb").put(1, "c").put(1, "a").build();
    assertEquals(test, expected);
  }

  public void test_toImmutableListMultimap_keyValue() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableListMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableListMultimap(s -> s.length(), s -> "!" + s));
    ImmutableListMultimap<Object, Object> expected = ImmutableListMultimap.builder()
        .put(1, "!a").put(2, "!ab").put(1, "!b").put(2, "!bb").put(1, "!c").put(1, "!a").build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_toImmutableSetMultimap_key() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSetMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSetMultimap(s -> s.length()));
    ImmutableSetMultimap<Object, Object> expected = ImmutableSetMultimap.builder()
        .put(1, "a").put(2, "ab").put(1, "b").put(2, "bb").put(1, "c").put(1, "a").build();
    assertEquals(test, expected);
  }

  public void test_toImmutableSetMultimap_keyValue() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSetMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSetMultimap(s -> s.length(), s -> "!" + s));
    ImmutableSetMultimap<Object, Object> expected = ImmutableSetMultimap.builder()
        .put(1, "!a").put(2, "!ab").put(1, "!b").put(2, "!bb").put(1, "!c").build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapEntriesToImmutableMap() {
    Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
    Map<String, Integer> expected = ImmutableMap.of("a", 1, "c", 3, "e", 5);
    ImmutableMap<String, Integer> output =
        input.entrySet()
            .stream()
            .filter(e -> e.getValue() % 2 == 1)
            .collect(entriesToImmutableMap());
    assertEquals(output, expected);
  }

  public void test_pairsToImmutableMap() {
    Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4);
    Map<String, Double> expected = ImmutableMap.of("A", 1.0, "B", 4.0, "C", 9.0, "D", 16.0);

    ImmutableMap<String, Double> output =
        input.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey().toUpperCase(Locale.ENGLISH), Math.pow(e.getValue(), 2)))
            .collect(pairsToImmutableMap());
    assertEquals(output, expected);
  }

  //-------------------------------------------------------------------------
  public void test_entry() {
    Map.Entry<String, Integer> test = Guavate.entry("A", 1);
    assertEquals(test.getKey(), "A");
    assertEquals(test.getValue(), (Integer) 1);
  }

  //-------------------------------------------------------------------------
  public void test_combineFuturesAsList() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    List<CompletableFuture<String>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<String>> test = Guavate.combineFuturesAsList(input);

    assertEquals(test.isDone(), false);
    latch.countDown();
    List<String> combined = test.join();
    assertEquals(test.isDone(), true);
    assertEquals(combined.size(), 2);
    assertEquals(combined.get(0), "A");
    assertEquals(combined.get(1), "B");
  }

  public void test_combineFuturesAsList_exception() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      throw new IllegalStateException("Oops");
    });
    List<CompletableFuture<String>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<String>> test = Guavate.combineFuturesAsList(input);

    assertEquals(test.isDone(), false);
    latch.countDown();
    assertThrows(CompletionException.class, () -> test.join());
    assertEquals(test.isDone(), true);
    assertEquals(test.isCompletedExceptionally(), true);
  }

  public void test_toCombinedFuture() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    List<CompletableFuture<String>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<String>> test = input.stream().collect(Guavate.toCombinedFuture());

    assertEquals(test.isDone(), false);
    latch.countDown();
    List<String> combined = test.join();
    assertEquals(test.isDone(), true);
    assertEquals(combined.size(), 2);
    assertEquals(combined.get(0), "A");
    assertEquals(combined.get(1), "B");
  }

  //-------------------------------------------------------------------------
  public void test_combineFuturesAsMap() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    Map<String, CompletableFuture<String>> input = ImmutableMap.of("a", future1, "b", future2);

    CompletableFuture<Map<String, String>> test = Guavate.combineFuturesAsMap(input);

    assertEquals(test.isDone(), false);
    latch.countDown();
    Map<String, String> combined = test.join();
    assertEquals(test.isDone(), true);
    assertEquals(combined.size(), 2);
    assertEquals(combined.get("a"), "A");
    assertEquals(combined.get("b"), "B");
  }

  public void test_combineFuturesAsMap_exception() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      throw new IllegalStateException("Oops");
    });
    Map<String, CompletableFuture<String>> input = ImmutableMap.of("a", future1, "b", future2);

    CompletableFuture<Map<String, String>> test = Guavate.combineFuturesAsMap(input);

    assertEquals(test.isDone(), false);
    latch.countDown();
    assertThrows(CompletionException.class, () -> test.join());
    assertEquals(test.isDone(), true);
    assertEquals(test.isCompletedExceptionally(), true);
  }

  public void test_toCombinedFutureMap() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    Map<String, CompletableFuture<String>> input = ImmutableMap.of("a", future1, "b", future2);

    CompletableFuture<Map<String, String>> test = input.entrySet().stream().collect(Guavate.toCombinedFutureMap());

    assertEquals(test.isDone(), false);
    latch.countDown();
    Map<String, String> combined = test.join();
    assertEquals(test.isDone(), true);
    assertEquals(combined.size(), 2);
    assertEquals(combined.get("a"), "A");
    assertEquals(combined.get("b"), "B");
  }

  //-------------------------------------------------------------------------
  public void test_poll() {
    AtomicInteger counter = new AtomicInteger();
    Supplier<String> pollingFn = () -> {
      switch (counter.incrementAndGet()) {
        case 1:
          return null;
        case 2:
          return "Yes";
        default:
          throw new AssertionError("Test failed");
      }
    };

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    CompletableFuture<String> future = Guavate.poll(executor, Duration.ofMillis(100), Duration.ofMillis(100), pollingFn);
    assertEquals(future.join(), "Yes");
  }

  public void test_poll_exception() {
    AtomicInteger counter = new AtomicInteger();
    Supplier<String> pollingFn = () -> {
      switch (counter.incrementAndGet()) {
        case 1:
          return null;
        case 2:
          throw new IllegalStateException("Expected");
        default:
          throw new AssertionError("Test failed");
      }
    };

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    try {
      CompletableFuture<String> future = Guavate.poll(executor, Duration.ofMillis(100), Duration.ofMillis(100), pollingFn);
      assertThrows(() -> future.join(), CompletionException.class, "java.lang.IllegalStateException: Expected");
    } finally {
      executor.shutdown();
    }
  }

  //-------------------------------------------------------------------------
  public void test_validUtilityClass() {
    assertUtilityClass(Guavate.class);
  }

}
