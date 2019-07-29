/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.collect.TestHelper;

/**
 * Test {@link ObjIntPair}.
 */
public class ObjIntPairTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {"A", 2},
        {"B", 200},
        {"C", -2},
        {"D", 0},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_ofPair(String first, int second) {
    Pair<String, Integer> pair = Pair.of(first, second);
    ObjIntPair<String> test = ObjIntPair.ofPair(pair);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    assertThat(test.elements()).containsExactly(first, second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertThat(test).hasToString(str);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toPair(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    assertThat(test.toPair()).isEqualTo(Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    ObjIntPair<String> p12 = ObjIntPair.of("1", 2);
    ObjIntPair<String> p13 = ObjIntPair.of("1", 3);
    ObjIntPair<String> p21 = ObjIntPair.of("2", 1);

    List<ObjIntPair<String>> list = new ArrayList<>();
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
    ObjIntPair<Runnable> test1 = ObjIntPair.of(notComparable, 2);
    ObjIntPair<Runnable> test2 = ObjIntPair.of(notComparable, 2);
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test1.compareTo(test2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    ObjIntPair<String> a = ObjIntPair.of("1", 2);
    ObjIntPair<String> a2 = ObjIntPair.of("1", 2);
    ObjIntPair<String> b = ObjIntPair.of("1", 3);
    ObjIntPair<String> c = ObjIntPair.of("2", 2);
    ObjIntPair<String> d = ObjIntPair.of("2", 3);

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
        .isNotEqualTo(Pair.of(Integer.valueOf(1), Integer.valueOf(1)))
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
    ObjIntPair<String> test = ObjIntPair.of("1", 1);
    TestHelper.coverImmutableBean(test);
  }

}
