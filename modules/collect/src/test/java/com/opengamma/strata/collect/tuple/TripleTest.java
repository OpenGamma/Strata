/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.collect.TestHelper;

/**
 * Test {@link Triple}.
 */
public class TripleTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {"A", "B", "C"},
        {"A", 200.2d, 6L},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(Object first, Object second, Object third) {
    Triple<Object, Object, Object> test = Triple.of(first, second, third);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second);
    assertThat(test.getThird()).isEqualTo(third);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(Object first, Object second, Object third) {
    Triple<Object, Object, Object> test = Triple.of(first, second, third);
    assertThat(test.elements()).containsExactly(first, second, third);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(Object first, Object second, Object third) {
    Triple<Object, Object, Object> test = Triple.of(first, second, third);
    String str = "[" + first + ", " + second + ", " + third + "]";
    assertThat(test).hasToString(str);
  }

  public static Object[][] data_factoryNull() {
    return new Object[][] {
        {null, null, null},
        {null, "B", "C"},
        {"A", null, "C"},
        {"A", "B", null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factoryNull")
  public void test_of_null(Object first, Object second, Object third) {
    assertThatIllegalArgumentException().isThrownBy(() -> Triple.of(first, second, third));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combining() {
    Triple<Integer, Integer, String> summed = Stream.of(Triple.of(10, 11, "3"), Triple.of(10, 11, "4"))
        .reduce(Triple.of(0, 0, ""), Triple.combining(Integer::sum, Integer::sum, String::concat));
    assertThat(summed).isEqualTo(Triple.of(20, 22, "34"));
  }

  @Test
  public void test_combinedWith() {
    Triple<String, Integer, Double> combined =
        Triple.of("1", 10, 4d).combinedWith(Triple.of("A", 20, 5d), String::concat, Integer::sum, Double::sum);
    assertThat(combined).isEqualTo(Triple.of("1A", 30, 9d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    Triple<String, String, String> abc = Triple.of("A", "B", "C");
    Triple<String, String, String> adc = Triple.of("A", "D", "C");
    Triple<String, String, String> bac = Triple.of("B", "A", "C");
    Triple<String, String, String> bad = Triple.of("B", "A", "D");

    List<Triple<String, String, String>> list = new ArrayList<>();
    list.add(abc);
    list.add(adc);
    list.add(bac);
    list.add(bad);
    list.sort(Comparator.naturalOrder());
    assertThat(list).containsExactly(abc, adc, bac, bad);
    list.sort(Comparator.reverseOrder());
    assertThat(list).containsExactly(bad, bac, adc, abc);
  }

  @Test
  public void test_compareTo_notComparable() {
    Runnable notComparable = () -> {};
    Triple<Integer, Runnable, String> test1 = Triple.of(1, notComparable, "A");
    Triple<Integer, Runnable, String> test2 = Triple.of(2, notComparable, "B");
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test1.compareTo(test2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    Triple<Integer, String, String> a = Triple.of(1, "Hello", "Triple");
    Triple<Integer, String, String> a2 = Triple.of(1, "Hello", "Triple");
    Triple<Integer, String, String> b = Triple.of(1, "Goodbye", "Triple");
    Triple<Integer, String, String> c = Triple.of(2, "Hello", "Triple");
    Triple<Integer, String, String> d = Triple.of(2, "Goodbye", "Triple");
    Triple<Integer, String, String> e = Triple.of(2, "Goodbye", "Other");

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(e)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
        .isNotEqualTo(Pair.of(Integer.valueOf(1), Double.valueOf(1.7d)))
        .hasSameHashCodeAs(a2);

    assertThat(b)
        .isNotEqualTo(a)
        .isEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(e);

    assertThat(c)
        .isNotEqualTo(a)
        .isEqualTo(c)
        .isNotEqualTo(b)
        .isNotEqualTo(d)
        .isNotEqualTo(e);

    assertThat(d)
        .isNotEqualTo(a)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isEqualTo(d)
        .isNotEqualTo(e);

    assertThat(e)
        .isNotEqualTo(a)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isEqualTo(e);
  }

  @Test
  public void test_toString() {
    Triple<String, String, String> test = Triple.of("A", "B", "C");
    assertThat(test).hasToString("[A, B, C]");
  }

  @Test
  public void coverage() {
    Triple<String, String, String> test = Triple.of("A", "B", "C");
    TestHelper.coverImmutableBean(test);
  }

}
