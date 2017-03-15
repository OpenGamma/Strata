/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Test
public class MapStreamTest {

  private final Map<String, Integer> map = ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4);

  public void filter() {
    Map<String, Integer> expected = ImmutableMap.of("one", 1, "two", 2);
    Map<String, Integer> result = MapStream.of(map).filter((k, v) -> k.equals("one") || v == 2).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void filterKeys() {
    Map<String, Integer> expected = ImmutableMap.of("one", 1, "two", 2);
    Map<String, Integer> result = MapStream.of(map).filterKeys(k -> k.length() == 3).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void filterValues() {
    Map<String, Integer> expected = ImmutableMap.of("one", 1, "two", 2);
    Map<String, Integer> result = MapStream.of(map).filterValues(v -> v < 3).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void mapKeysToKeys() {
    Map<String, Integer> expected = ImmutableMap.of("ONE", 1, "TWO", 2, "THREE", 3, "FOUR", 4);
    Map<String, Integer> result = MapStream.of(map).mapKeys(k -> k.toUpperCase(Locale.ENGLISH)).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void mapKeysAndValuesToKeys() {
    Map<String, Integer> expected = ImmutableMap.of("one1", 1, "two2", 2, "three3", 3, "four4", 4);
    Map<String, Integer> result = MapStream.of(map).mapKeys((k, v) -> k + v).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void mapValuesToValues() {
    Map<String, Integer> expected = ImmutableMap.of("one", 2, "two", 4, "three", 6, "four", 8);
    Map<String, Integer> result = MapStream.of(map).mapValues(v -> v * 2).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void mapKeysAndValuesToValues() {
    Map<String, String> expected = ImmutableMap.of("one", "one1", "two", "two2", "three", "three3", "four", "four4");
    Map<String, String> result = MapStream.of(map).mapValues((k, v) -> k + v).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void map() {
    List<String> expected = ImmutableList.of("one1", "two2", "three3", "four4");
    List<String> result = MapStream.of(map).map((k, v) -> k + v).collect(toList());
    assertThat(result).isEqualTo(expected);
  }

  public void forEach() {
    HashMap<Object, Object> mutableMap = new HashMap<>();
    MapStream.of(map).forEach((k, v) -> mutableMap.put(k, v));
    assertThat(mutableMap).isEqualTo(map);
  }

  public void ofCollection() {
    List<String> letters = ImmutableList.of("a", "b", "c");
    Map<String, String> expected = ImmutableMap.of("A", "a", "B", "b", "C", "c");
    Map<String, String> result = MapStream.of(letters, letter -> letter.toUpperCase(Locale.ENGLISH)).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void ofStream() {
    Stream<String> letters = Stream.of("a", "b", "c");
    Map<String, String> expected = ImmutableMap.of("A", "a", "B", "b", "C", "c");
    Map<String, String> result = MapStream.of(letters, letter -> letter.toUpperCase(Locale.ENGLISH)).toMap();
    assertThat(result).isEqualTo(expected);
  }

  public void toMapDuplicateKeys() {
    assertThrowsIllegalArg(() -> MapStream.of(map).mapKeys(k -> "key").toMap());
  }

  public void toMapWithMerge() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, Integer> expected = ImmutableMap.of("a", 3, "b", 30, "c", 1);
    Map<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1)).toMap((v1, v2) -> v1 + v2);
    assertThat(result).isEqualTo(expected);
  }

  public void coverage() {
    MapStream.empty()
        .filter(e -> false)
        .distinct()
        .sorted()
        .sorted((e1, e2) -> 0)
        .peek(e -> e.toString())
        .limit(0)
        .skip(0)
        .sequential()
        .parallel()
        .unordered()
        .onClose(() -> System.out.println())
        .close();
    MapStream.empty().anyMatch(e -> true);
    MapStream.empty().allMatch(e -> true);
    MapStream.empty().noneMatch(e -> true);
    MapStream.empty().count();
    MapStream.empty().findAny();
    MapStream.empty().findFirst();
    MapStream.empty().max((e1, e2) -> 0);
    MapStream.empty().min((e1, e2) -> 0);
    MapStream.empty().iterator();
    MapStream.empty().spliterator();
    MapStream.empty().isParallel();
    MapStream.empty().map(e -> e);
    MapStream.empty().mapToInt(e -> 0);
    MapStream.empty().mapToLong(e -> 0);
    MapStream.empty().mapToDouble(e -> 0);
    MapStream.empty().flatMap(e -> Stream.empty());
    MapStream.empty().flatMapToDouble(e -> DoubleStream.empty());
    MapStream.empty().flatMapToInt(e -> IntStream.empty());
    MapStream.empty().flatMapToLong(e -> LongStream.empty());
    MapStream.empty().collect(toList());
    MapStream.empty().collect(() -> null, (o, e) -> System.out.println(), (o1, o2) -> System.out.println());
    MapStream.empty().toArray();
    MapStream.empty().toArray(i -> new Object[0]);
    MapStream.empty().forEach(e -> System.out.println());
    MapStream.empty().forEachOrdered(e -> System.out.println());
    MapStream.empty().reduce(new AbstractMap.SimpleEntry<>(null, null), (o1, o2) -> null);
    MapStream.empty().reduce((o1, o2) -> null);
    MapStream.empty().reduce(null, (o, e) -> null, (o1, o2) -> null);
  }
}
