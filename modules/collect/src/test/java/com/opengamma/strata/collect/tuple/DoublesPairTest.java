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
 * Test {@link DoublesPair}.
 */
public class DoublesPairTest {

  private static final Offset<Double> TOLERANCE = within(0.00001d);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {1.2d, 2.5d},
        {-100.1d, 200.2d},
        {-1.2d, -2.5d},
        {0d, 0d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    assertThat(test.getFirst()).isEqualTo(first, TOLERANCE);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_ofPair(double first, double second) {
    Pair<Double, Double> pair = Pair.of(first, second);
    DoublesPair test = DoublesPair.ofPair(pair);
    assertThat(test.getFirst()).isEqualTo(first, TOLERANCE);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    assertThat(test.elements()).containsExactly(first, second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertThat(test).hasToString(str);
    assertThat(DoublesPair.parse(str)).isEqualTo(test);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toPair(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    assertThat(test.toPair()).isEqualTo(Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_parseGood() {
    return new Object[][] {
        {"[1.2, 2.5]", 1.2d, 2.5d},
        {"[1.2,2.5]", 1.2d, 2.5d},
        {"[ 1.2, 2.5 ]", 1.2d, 2.5d},
        {"[-1.2, -2.5]", -1.2d, -2.5d},
        {"[0,4]", 0d, 4d},
        {"[1d,201d]", 1d, 201d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_good(String text, double first, double second) {
    DoublesPair test = DoublesPair.parse(text);
    assertThat(test.getFirst()).isEqualTo(first, TOLERANCE);
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
    assertThatIllegalArgumentException().isThrownBy(() -> DoublesPair.parse(text));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    DoublesPair p12 = DoublesPair.of(1d, 2d);
    DoublesPair p13 = DoublesPair.of(1d, 3d);
    DoublesPair p21 = DoublesPair.of(2d, 1d);

    List<DoublesPair> list = new ArrayList<>();
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
    DoublesPair a = DoublesPair.of(1d, 2.0d);
    DoublesPair a2 = DoublesPair.of(1d, 2.0d);
    DoublesPair b = DoublesPair.of(1d, 3.0d);
    DoublesPair c = DoublesPair.of(2d, 2.0d);
    DoublesPair d = DoublesPair.of(2d, 3.0d);

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
        .isNotEqualTo(Pair.of(Double.valueOf(1.1d), Double.valueOf(1.7d)))
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
    DoublesPair test = DoublesPair.of(1d, 2.0d);
    TestHelper.coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    assertSerialization(DoublesPair.of(1d, 1.7d));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(DoublesPair.class, DoublesPair.of(1d, 1.7d));
  }

}
