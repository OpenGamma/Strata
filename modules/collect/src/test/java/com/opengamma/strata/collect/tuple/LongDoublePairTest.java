/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.collect.TestHelper;

/**
 * Test {@link LongDoublePair}.
 */
public class LongDoublePairTest {

  private static final Offset<Double> TOLERANCE = within(0.00001d);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {1L, 2.5d},
        {-100L, 200.2d},
        {-1L, -2.5d},
        {0L, 0d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_ofPair(long first, double second) {
    Pair<Long, Double> pair = Pair.of(first, second);
    LongDoublePair test = LongDoublePair.ofPair(pair);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    assertThat(test.elements()).containsExactly(first, second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertThat(test).hasToString(str);
    assertThat(LongDoublePair.parse(str)).isEqualTo(test);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toPair(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    assertThat(test.toPair()).isEqualTo(Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_parseGood() {
    return new Object[][] {
        {"[1, 2.5]", 1L, 2.5d},
        {"[1,2.5]", 1L, 2.5d},
        {"[ 1, 2.5 ]", 1L, 2.5d},
        {"[-1, -2.5]", -1L, -2.5d},
        {"[0,4]", 0L, 4d},
        {"[1,201d]", 1L, 201d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_good(String text, long first, double second) {
    LongDoublePair test = LongDoublePair.parse(text);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  public static Object[][] data_parseBad() {
    return new Object[][] {
        {null},
        {""},
        {"[]"},
        {"[10]"},
        {"[10,20"},
        {"10,20]"},
        {"[10 20]"},
        {"[10,20,30]"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_bad(String text) {
    assertThatIllegalArgumentException().isThrownBy(() -> LongDoublePair.parse(text));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    LongDoublePair p12 = LongDoublePair.of(1L, 2d);
    LongDoublePair p13 = LongDoublePair.of(1L, 3d);
    LongDoublePair p21 = LongDoublePair.of(2L, 1d);

    List<LongDoublePair> list = new ArrayList<>();
    list.add(p12);
    list.add(p13);
    list.add(p21);
    list.sort(Comparator.naturalOrder());
    assertThat(list).containsExactly(p12, p13, p21);
    list.sort(Comparator.reverseOrder());
    assertThat(list).containsExactly(p21, p13, p12);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    LongDoublePair a = LongDoublePair.of(1L, 2.0d);
    LongDoublePair a2 = LongDoublePair.of(1L, 2.0d);
    LongDoublePair b = LongDoublePair.of(1L, 3.0d);
    LongDoublePair c = LongDoublePair.of(2L, 2.0d);
    LongDoublePair d = LongDoublePair.of(2L, 3.0d);

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
        .isNotEqualTo(Pair.of(Long.valueOf(1L), Double.valueOf(1.7d)))
        .hasSameHashCodeAs(a2);

    assertThat(b)
        .isNotEqualTo(a)
        .isEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d);

    assertThat(c)
        .isNotEqualTo(a)
        .isEqualTo(c)
        .isNotEqualTo(b)
        .isNotEqualTo(d);

    assertThat(d)
        .isNotEqualTo(a)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isEqualTo(d);
  }

  @Test
  public void coverage() {
    LongDoublePair test = LongDoublePair.of(1L, 1.7d);
    TestHelper.coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    assertSerialization(LongDoublePair.of(1L, 1.7d));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(LongDoublePair.class, LongDoublePair.of(1L, 1.7d));
  }

}
