/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.Guavate.entry;
import static com.opengamma.strata.collect.Guavate.in;
import static com.opengamma.strata.collect.Guavate.pairsToImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.list;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link MapStream}.
 */
public class MapStreamTest {

  private final Map<String, Integer> map = ImmutableMap.of("one", 1, "two", 2, "three", 3, "four", 4);
  private final List<String> list = ImmutableList.of("one", "two", "three", "four");

  //-------------------------------------------------------------------------
  @Test
  public void keys() {
    List<String> result = MapStream.of(map).keys().collect(toImmutableList());
    assertThat(result).isEqualTo(ImmutableList.of("one", "two", "three", "four"));
  }

  @Test
  public void values() {
    List<Integer> result = MapStream.of(map).values().collect(toImmutableList());
    assertThat(result).isEqualTo(ImmutableList.of(1, 2, 3, 4));
  }

  @Test
  public void keys_fromList() {
    List<String> result = MapStream.of(list).keys().collect(toImmutableList());
    assertThat(result).isEqualTo(list);
  }

  @Test
  public void values_fromList() {
    List<String> result = MapStream.of(list).values().collect(toImmutableList());
    assertThat(result).isEqualTo(list);
  }

  @Test
  public void keys_fromStream() {
    List<String> result = MapStream.of(list.stream()).keys().collect(toImmutableList());
    assertThat(result).isEqualTo(list);
  }

  @Test
  public void values_fromStream() {
    List<String> result = MapStream.of(list.stream()).values().collect(toImmutableList());
    assertThat(result).isEqualTo(list);
  }

  @Test
  public void inverse() {
    Map<Integer, String> expected = ImmutableMap.of(1, "one", 2, "two", 3, "three", 4, "four");
    Map<Integer, String> result = MapStream.of(map).inverse().toMap();
    assertThat(result).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void filter() {
    Map<String, Integer> expected = ImmutableMap.of("one", 1, "two", 2);
    Map<String, Integer> result = MapStream.of(map).filter((k, v) -> k.equals("one") || v == 2).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void filterKeys() {
    Map<String, Integer> expected = ImmutableMap.of("one", 1, "two", 2);
    Map<String, Integer> result = MapStream.of(map).filterKeys(k -> k.length() == 3).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void filterKeys_byClass() {
    Map<Number, Number> map = ImmutableMap.of(1, 11, 2d, 22d, 3, 33d);
    Map<Integer, Number> result = MapStream.of(map).filterKeys(Integer.class).toMap();
    assertThat(result).isEqualTo(ImmutableMap.of(1, 11, 3, 33d));
  }

  @Test
  public void filterValues() {
    Map<String, Integer> expected = ImmutableMap.of("one", 1, "two", 2);
    Map<String, Integer> result = MapStream.of(map).filterValues(v -> v < 3).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void filterValues_byClass() {
    Map<Number, Number> map = ImmutableMap.of(1, 11, 2d, 22, 3, 33d);
    Map<Number, Integer> result = MapStream.of(map).filterValues(Integer.class).toMap();
    assertThat(result).isEqualTo(ImmutableMap.of(1, 11, 2d, 22));
  }

  @Test
  public void mapKeysToKeys() {
    Map<String, Integer> expected = ImmutableMap.of("ONE", 1, "TWO", 2, "THREE", 3, "FOUR", 4);
    Map<String, Integer> result = MapStream.of(map).mapKeys(k -> k.toUpperCase(Locale.ENGLISH)).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void mapKeysAndValuesToKeys() {
    Map<String, Integer> expected = ImmutableMap.of("one1", 1, "two2", 2, "three3", 3, "four4", 4);
    Map<String, Integer> result = MapStream.of(map).mapKeys((k, v) -> k + v).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void mapValuesToValues() {
    Map<String, Integer> expected = ImmutableMap.of("one", 2, "two", 4, "three", 6, "four", 8);
    Map<String, Integer> result = MapStream.of(map).mapValues(v -> v * 2).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void mapKeysAndValuesToValues() {
    Map<String, String> expected = ImmutableMap.of("one", "one1", "two", "two2", "three", "three3", "four", "four4");
    Map<String, String> result = MapStream.of(map).mapValues((k, v) -> k + v).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void mapBoth() {
    Map<String, Short> expected =
        ImmutableMap.of("one - 1", (short) 0, "two - 1", (short) 1, "three - 1", (short) 2, "four - 1", (short) 3);
    Map<String, Short> result = MapStream.of(map).mapBoth((k, v) -> entry(k + " - 1", (short) (v - 1))).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMapBoth() {
    Map<String, ImmutableMap<String, Short>> initialMap = ImmutableMap.of(
        "one - 1", ImmutableMap.of("sub-one", (short) 0),
        "two - 1", ImmutableMap.of("sub-two", (short) 1),
        "three - 1", ImmutableMap.of("sub-three", (short) 2),
        "four - 1", ImmutableMap.of("sub-four", (short) 3, "sub-five", (short) 4));
    Map<String, Short> expectedResult = ImmutableMap.of(
        "sub-one", (short) 0,
        "sub-two", (short) 1,
        "sub-three", (short) 2,
        "sub-four", (short) 3,
        "sub-five", (short) 4);

    Map<String, Short> result = MapStream.of(initialMap)
        .flatMapBoth((k, v) -> v.entrySet().stream())
        .toMap();
    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  public void map() {
    List<String> expected = ImmutableList.of("one1", "two2", "three3", "four4");
    List<String> result = MapStream.of(map).map((k, v) -> k + v).collect(toList());
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void mapToDouble() {
    double[] expected = new double[] {1d, 2d, 3d, 4d};
    double[] result = MapStream.of(map)
        .mapToDouble((k, v) -> (double) v)
        .toArray();
    assertThat(expected).isEqualTo(result);
  }

  @Test
  public void mapToInt() {
    int[] expected = new int[] {1, 2, 3, 4};
    int[] result = MapStream.of(map)
        .mapToInt((k, v) -> v)
        .toArray();
    assertThat(expected).isEqualTo(result);
  }

  @Test
  public void flatMapKeysToKeys() {
    Map<String, Integer> expected = ImmutableMap.<String, Integer>builder()
        .put("one", 1)
        .put("ONE", 1)
        .put("two", 2)
        .put("TWO", 2)
        .put("three", 3)
        .put("THREE", 3)
        .put("four", 4)
        .put("FOUR", 4)
        .build();

    ImmutableMap<String, Integer> result = MapStream.of(map)
        .flatMapKeys(key -> Stream.of(key.toLowerCase(Locale.ENGLISH), key.toUpperCase(Locale.ENGLISH)))
        .toMap();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMapKeysAndValuesToKeys() {
    Map<String, Integer> expected = ImmutableMap.<String, Integer>builder()
        .put("one", 1)
        .put("1", 1)
        .put("two", 2)
        .put("2", 2)
        .put("three", 3)
        .put("3", 3)
        .put("four", 4)
        .put("4", 4)
        .build();

    ImmutableMap<String, Integer> result = MapStream.of(map)
        .flatMapKeys((key, value) -> Stream.of(key, Integer.toString(value)))
        .toMap();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMapValuesToValues() {
    List<Pair<String, Integer>> expected = ImmutableList.of(
        Pair.of("one", 1),
        Pair.of("one", 1),
        Pair.of("two", 2),
        Pair.of("two", 4),
        Pair.of("three", 3),
        Pair.of("three", 9),
        Pair.of("four", 4),
        Pair.of("four", 16));

    List<Pair<String, Integer>> result = MapStream.of(map)
        .flatMapValues(value -> Stream.of(value, value * value))
        .map((k, v) -> Pair.of(k, v))
        .collect(toList());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMapKeysAndValuesToValues() {
    List<Pair<String, String>> expected = ImmutableList.of(
        Pair.of("one", "one"),
        Pair.of("one", "1"),
        Pair.of("two", "two"),
        Pair.of("two", "2"),
        Pair.of("three", "three"),
        Pair.of("three", "3"),
        Pair.of("four", "four"),
        Pair.of("four", "4"));

    List<Pair<String, String>> result = MapStream.of(map)
        .flatMapValues((key, value) -> Stream.of(key, Integer.toString(value)))
        .map((k, v) -> Pair.of(k, v))
        .collect(toList());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMap() {
    Map<String, String> expected = ImmutableMap.<String, String>builder()
        .put("one", "1")
        .put("1", "one")
        .put("two", "2")
        .put("2", "two")
        .put("three", "3")
        .put("3", "three")
        .put("four", "4")
        .put("4", "four")
        .build();

    Map<String, String> result = MapStream.of(map)
        .flatMap((k, v) -> Stream.of(Pair.of(k, Integer.toString(v)), Pair.of(Integer.toString(v), k)))
        .collect(pairsToImmutableMap());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMapToDouble() {
    double[] expected = new double[] {1d, 1d, 2d, 4d, 3d, 9d, 4d, 16d};

    double[] result = MapStream.of(map)
        .flatMapToDouble((k, v) -> DoubleStream.of(v, v * v))
        .toArray();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void flatMapToInt() {
    int[] expected = new int[] {1, 1, 2, 4, 3, 9, 4, 16};

    int[] result = MapStream.of(map)
        .flatMapToInt((k, v) -> IntStream.of(v, v * v))
        .toArray();

    assertThat(result).isEqualTo(expected);
  }

  //-----------------------------------------------------------------------
  @Test
  public void sortedKeys() {
    List<Map.Entry<String, Integer>> expected =
        ImmutableList.of(entry("four", 4), entry("one", 1), entry("three", 3), entry("two", 2));

    List<Map.Entry<String, Integer>> result = MapStream.of(map)
        .sortedKeys()
        .collect(toList());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void sortedKeys_comparator() {
    List<Map.Entry<String, Integer>> expected =
        ImmutableList.of(entry("two", 2), entry("three", 3), entry("one", 1), entry("four", 4));

    List<Map.Entry<String, Integer>> result = MapStream.of(map)
        .sortedKeys(Comparator.reverseOrder())
        .collect(toList());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void sortedValues() {
    ImmutableMap<String, Integer> invertedValuesMap = ImmutableMap.of(
        "one", 4,
        "two", 3,
        "three", 2,
        "four", 1);

    List<Map.Entry<String, Integer>> expected =
        ImmutableList.of(entry("four", 1), entry("three", 2), entry("two", 3), entry("one", 4));

    List<Map.Entry<String, Integer>> result = MapStream.of(invertedValuesMap).sortedValues().collect(toList());

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void sortedValues_comparator() {
    List<Map.Entry<String, Integer>> expected =
        ImmutableList.of(entry("four", 4), entry("three", 3), entry("two", 2), entry("one", 1));

    List<Map.Entry<String, Integer>> result = MapStream.of(map)
        .sortedValues(Comparator.reverseOrder())
        .collect(toList());

    assertThat(result).isEqualTo(expected);
  }

  //-----------------------------------------------------------------------
  @Test
  public void minKeys() {
    Map.Entry<String, Integer> result = MapStream.of(map).minKeys(Comparator.naturalOrder()).get();
    assertThat(result).isEqualTo(entry("four", 4));
  }

  @Test
  public void minValues() {
    Map.Entry<String, Integer> result = MapStream.of(map).minValues(Comparator.naturalOrder()).get();
    assertThat(result).isEqualTo(entry("one", 1));
  }

  @Test
  public void maxKeys() {
    Map.Entry<String, Integer> result = MapStream.of(map).maxKeys(Comparator.naturalOrder()).get();
    assertThat(result).isEqualTo(entry("two", 2));
  }

  @Test
  public void maxValues() {
    Map.Entry<String, Integer> result = MapStream.of(map).maxValues(Comparator.naturalOrder()).get();
    assertThat(result).isEqualTo(entry("four", 4));
  }

  //-----------------------------------------------------------------------
  @Test
  public void anyMatch() {
    assertThat(MapStream.of(map).anyMatch((key, value) -> key.length() + value < 10)).isTrue();
    assertThat(MapStream.of(map).anyMatch((key, value) -> key.length() + value < 8)).isTrue();
    assertThat(MapStream.of(map).anyMatch((key, value) -> key.length() + value < 4)).isFalse();
  }

  @Test
  public void allMatch() {
    assertThat(MapStream.of(map).allMatch((key, value) -> key.length() + value < 10)).isTrue();
    assertThat(MapStream.of(map).allMatch((key, value) -> key.length() + value < 8)).isFalse();
    assertThat(MapStream.of(map).allMatch((key, value) -> key.length() + value < 4)).isFalse();
  }

  @Test
  public void noneMatch() {
    assertThat(MapStream.of(map).noneMatch((key, value) -> key.length() + value < 10)).isFalse();
    assertThat(MapStream.of(map).noneMatch((key, value) -> key.length() + value < 8)).isFalse();
    assertThat(MapStream.of(map).noneMatch((key, value) -> key.length() + value < 4)).isTrue();
  }

  //-----------------------------------------------------------------------
  @Test
  public void forEach() {
    HashMap<Object, Object> mutableMap = new HashMap<>();
    MapStream.of(map).forEach((k, v) -> mutableMap.put(k, v));
    assertThat(mutableMap).isEqualTo(map);
  }

  //-------------------------------------------------------------------------
  @Test
  public void ofMultimap() {
    ImmutableMultimap<String, Integer> input = ImmutableMultimap.of("one", 1, "two", 2, "one", 3);
    assertThat(MapStream.of(input)).containsExactlyInAnyOrder(entry("one", 1), entry("two", 2), entry("one", 3));
    assertThat(MapStream.of(input).toMap(Integer::sum)).containsOnly(entry("one", 4), entry("two", 2));
  }

  @Test
  public void ofCollection() {
    List<String> letters = ImmutableList.of("a", "b", "c");
    Map<String, String> expected = ImmutableMap.of("A", "a", "B", "b", "C", "c");
    Map<String, String> result = MapStream.of(letters, letter -> letter.toUpperCase(Locale.ENGLISH)).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void ofCollection_2arg() {
    List<String> letters = ImmutableList.of("a", "b", "c");
    Map<String, String> expected = ImmutableMap.of("A", "aa", "B", "bb", "C", "cc");
    Map<String, String> result =
        MapStream.of(letters, letter -> letter.toUpperCase(Locale.ENGLISH), letter -> letter + letter).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void ofStream() {
    Stream<String> letters = Stream.of("a", "b", "c");
    Map<String, String> expected = ImmutableMap.of("A", "a", "B", "b", "C", "c");
    Map<String, String> result = MapStream.of(letters, letter -> letter.toUpperCase(Locale.ENGLISH)).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void ofStream_2arg() {
    Stream<String> letters = Stream.of("a", "b", "c");
    Map<String, String> expected = ImmutableMap.of("A", "aa", "B", "bb", "C", "cc");
    Map<String, String> result =
        MapStream.of(letters, letter -> letter.toUpperCase(Locale.ENGLISH), letter -> letter + letter).toMap();
    assertThat(result).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void zip() {
    Stream<Integer> numbers = Stream.of(0, 1, 2);
    Stream<String> letters = Stream.of("a", "b", "c");
    Map<Integer, String> expected = ImmutableMap.of(0, "a", 1, "b", 2, "c");
    Map<Integer, String> result = MapStream.zip(numbers, letters).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void zip_longerFirst() {
    Stream<Integer> numbers = Stream.of(0, 1, 2, 3);
    Stream<String> letters = Stream.of("a", "b", "c");
    Map<Integer, String> expected = ImmutableMap.of(0, "a", 1, "b", 2, "c");
    Map<Integer, String> result = MapStream.zip(numbers, letters).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void zip_longerSecond() {
    Stream<Integer> numbers = Stream.of(0, 1, 2);
    Stream<String> letters = Stream.of("a", "b", "c", "d");
    Map<Integer, String> expected = ImmutableMap.of(0, "a", 1, "b", 2, "c");
    Map<Integer, String> result = MapStream.zip(numbers, letters).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void zipWithIndex() {
    Stream<String> letters = Stream.of("a", "b", "c");
    Map<Integer, String> expected = ImmutableMap.of(0, "a", 1, "b", 2, "c");
    Map<Integer, String> result = MapStream.zipWithIndex(letters).toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void concat() {
    ImmutableMap<String, Integer> map1 = ImmutableMap.of("one", 1, "two", 2, "three", 3);
    ImmutableMap<String, Integer> map2 = ImmutableMap.of("three", 7, "four", 4);
    ImmutableMap<String, Integer> result = MapStream.concat(MapStream.of(map1), MapStream.of(map2)).toMap((a, b) -> a);
    assertThat(result).isEqualTo(map);
  }

  @Test
  public void concatGeneric() {
    ImmutableMap<String, Object> map1 = ImmutableMap.of("one", 1, "two", 2, "three", 3);
    ImmutableMap<Object, Integer> map2 = ImmutableMap.of("three", 7, "four", 4);
    ImmutableMap<Object, Object> result = MapStream.concat(MapStream.of(map1), MapStream.of(map2)).toMap((a, b) -> a);
    assertThat(result).isEqualTo(map);
  }

  @Test
  public void concatNumberValues() {
    ImmutableMap<String, Double> map1 = ImmutableMap.of("one", 1D, "two", 2D, "three", 3D);
    ImmutableMap<Object, Integer> map2 = ImmutableMap.of("three", 7, "four", 4);
    ImmutableMap<Object, ? extends Number> result =
        MapStream.concat(MapStream.of(map1), MapStream.of(map2)).toMap((a, b) -> a);
    assertThat(result).isEqualTo(ImmutableMap.of("one", 1D, "two", 2D, "three", 3D, "four", 4));
  }

  //-------------------------------------------------------------------------
  @Test
  public void toMapDuplicateKeys() {
    assertThatIllegalArgumentException().isThrownBy(() -> MapStream.of(map).mapKeys(k -> "key").toMap());
  }

  @Test
  public void toMapWithMerge() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, Integer> expected = ImmutableMap.of("a", 3, "b", 30, "c", 1);
    Map<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1)).toMap((v1, v2) -> v1 + v2);
    assertThat(result).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void toMapGrouping() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, List<Integer>> expected = ImmutableMap.of("a", list(1, 2), "b", list(10, 20), "c", list(1));
    Map<String, List<Integer>> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1)).toMapGrouping();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void groupingAndThen() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, ImmutableList<Integer>> expected = ImmutableMap.of("a", list(1, 2), "b", list(10, 20), "c", list(1));
    Map<String, List<Integer>> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1)).groupingAndThen().toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void toMapGroupingWithCollector() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, Integer> expected = ImmutableMap.of("a", 3, "b", 30, "c", 1);
    Map<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1))
        .toMapGrouping(reducing(0, Integer::sum));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void groupingAndThenWithCollector() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, Integer> expected = ImmutableMap.of("a", 3, "b", 30, "c", 1);
    Map<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1))
        .groupingAndThen(reducing(0, Integer::sum))
        .toMap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void toMapGroupingRetainsOrder() {
    Map<String, Integer> map = ImmutableMap.of("d", 1, "dd", 2, "b", 10, "bb", 20, "c", 1);
    Map<String, Integer> expected = ImmutableMap.of("d", 3, "b", 30, "c", 1);
    Map<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1))
        .toMapGrouping(reducing(0, Integer::sum));
    assertThat(result).isEqualTo(expected);
    assertThat(result).containsExactlyEntriesOf(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void toListMultimap() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "aa", 2, "b", 10, "bb", 20, "c", 1);
    ListMultimap<String, Integer> expected = ImmutableListMultimap.of("a", 1, "a", 2, "b", 10, "b", 20, "c", 1);
    ListMultimap<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1)).toListMultimap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void toSetMultimap() {
    Map<String, Integer> map = ImmutableMap.<String, Integer>builder()
        .put("a", 1)
        .put("aa", 2)
        .put("aaa", 1)
        .put("b", 10)
        .put("bb", 20)
        .put("c", 1)
        .build();
    SetMultimap<String, Integer> expected = ImmutableSetMultimap.of("a", 1, "a", 2, "b", 10, "b", 20, "c", 1);
    SetMultimap<String, Integer> result = MapStream.of(map).mapKeys(s -> s.substring(0, 1)).toSetMultimap();
    assertThat(result).isEqualTo(expected);
  }

  @Test
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
