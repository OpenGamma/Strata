/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.Guavate.entriesToImmutableMap;
import static com.opengamma.strata.collect.Guavate.pairsToImmutableMap;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    List<String> list = Arrays.asList("a", "b", "b", "b", "c", "a");
    Map<String, Integer> result = list.stream()
        .collect(Guavate.toImmutableMap(s -> s, s -> 1, (s1, s2) -> s1 + s2));
    Map<String, Integer> expected = ImmutableMap.of("a", 2, "b", 3, "c", 1);
    assertEquals(result, expected);
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
  public void test_validUtilityClass() {
    assertUtilityClass(Guavate.class);
  }

}
