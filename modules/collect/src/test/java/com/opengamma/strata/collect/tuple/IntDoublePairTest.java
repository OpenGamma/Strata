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
 * Test {@link IntDoublePair}.
 */
public class IntDoublePairTest {

  private static final Offset<Double> TOLERANCE = within(0.00001d);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {1, 2.5d},
        {-100, 200.2d},
        {-1, -2.5d},
        {0, 0d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_ofPair(int first, double second) {
    Pair<Integer, Double> pair = Pair.of(first, second);
    IntDoublePair test = IntDoublePair.ofPair(pair);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    assertThat(test.elements()).containsExactly(first, second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertThat(test).hasToString(str);
    assertThat(IntDoublePair.parse(str)).isEqualTo(test);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toPair(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    assertThat(test.toPair()).isEqualTo(Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_parseGood() {
    return new Object[][] {
        {"[1, 2.5]", 1, 2.5d},
        {"[1,2.5]", 1, 2.5d},
        {"[ 1, 2.5 ]", 1, 2.5d},
        {"[-1, -2.5]", -1, -2.5d},
        {"[0,4]", 0, 4d},
        {"[1,201d]", 1, 201d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_good(String text, int first, double second) {
    IntDoublePair test = IntDoublePair.parse(text);
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
    assertThatIllegalArgumentException().isThrownBy(() -> IntDoublePair.parse(text));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    IntDoublePair p12 = IntDoublePair.of(1, 2d);
    IntDoublePair p13 = IntDoublePair.of(1, 3d);
    IntDoublePair p21 = IntDoublePair.of(2, 1d);

    List<IntDoublePair> list = new ArrayList<>();
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
    IntDoublePair a = IntDoublePair.of(1, 2.0d);
    IntDoublePair a2 = IntDoublePair.of(1, 2.0d);
    IntDoublePair b = IntDoublePair.of(1, 3.0d);
    IntDoublePair c = IntDoublePair.of(2, 2.0d);
    IntDoublePair d = IntDoublePair.of(2, 3.0d);

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
        .isNotEqualTo(Pair.of(Integer.valueOf(1), Double.valueOf(1.7d)))
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
    IntDoublePair test = IntDoublePair.of(1, 1.7d);
    TestHelper.coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    assertSerialization(IntDoublePair.of(1, 1.7d));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(IntDoublePair.class, IntDoublePair.of(1, 1.7d));
  }

}
