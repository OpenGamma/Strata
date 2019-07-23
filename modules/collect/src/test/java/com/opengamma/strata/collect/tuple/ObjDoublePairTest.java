/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
 * Test {@link ObjDoublePair}.
 */
public class ObjDoublePairTest {

  private static final Offset<Double> TOLERANCE = within(0.00001d);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {"A", 2.5d},
        {"B", 200.2d},
        {"C", -2.5d},
        {"D", 0d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_ofPair(String first, double second) {
    Pair<String, Double> pair = Pair.of(first, second);
    ObjDoublePair<String> test = ObjDoublePair.ofPair(pair);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second, TOLERANCE);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    assertThat(test.elements()).containsExactly(first, second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertThat(test).hasToString(str);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toPair(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    assertThat(test.toPair()).isEqualTo(Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    ObjDoublePair<String> p12 = ObjDoublePair.of("1", 2d);
    ObjDoublePair<String> p13 = ObjDoublePair.of("1", 3d);
    ObjDoublePair<String> p21 = ObjDoublePair.of("2", 1d);

    List<ObjDoublePair<String>> list = new ArrayList<>();
    list.add(p12);
    list.add(p13);
    list.add(p21);
    list.sort(Comparator.naturalOrder());
    assertThat(list).containsExactly(p12, p13, p21);
    list.sort(Comparator.reverseOrder());
    assertThat(list).containsExactly(p21, p13, p12);
  }

  @Test
  public void test_compareTo_notComparable() {
    Runnable notComparable = () -> {};
    ObjDoublePair<Runnable> test1 = ObjDoublePair.of(notComparable, 2d);
    ObjDoublePair<Runnable> test2 = ObjDoublePair.of(notComparable, 2d);
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test1.compareTo(test2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    ObjDoublePair<String> a = ObjDoublePair.of("1", 2.0d);
    ObjDoublePair<String> a2 = ObjDoublePair.of("1", 2.0d);
    ObjDoublePair<String> b = ObjDoublePair.of("1", 3.0d);
    ObjDoublePair<String> c = ObjDoublePair.of("2", 2.0d);
    ObjDoublePair<String> d = ObjDoublePair.of("2", 3.0d);

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
    ObjDoublePair<String> test = ObjDoublePair.of("1", 1.7d);
    TestHelper.coverImmutableBean(test);
  }

}
