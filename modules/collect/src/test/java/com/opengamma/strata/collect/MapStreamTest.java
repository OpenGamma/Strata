/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

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

  public void toMapDuplicateKeys() {
    assertThrowsIllegalArg(() -> MapStream.of(map).mapKeys(k -> "key").toMap());
  }

  public void toImmutableMapDuplicateKeys() {
    assertThrowsIllegalArg(() -> MapStream.of(map).mapKeys(k -> "key").toImmutableMap());
  }
}
