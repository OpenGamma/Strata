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
 * Test {@link Pair}.
 */
public class PairTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public static Object[][] data_factory() {
    return new Object[][] {
        {"A", "B"},
        {"A", 200.2d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_of_getters(Object first, Object second) {
    Pair<Object, Object> test = Pair.of(first, second);
    assertThat(test.getFirst()).isEqualTo(first);
    assertThat(test.getSecond()).isEqualTo(second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_sizeElements(Object first, Object second) {
    Pair<Object, Object> test = Pair.of(first, second);
    assertThat(test.elements()).containsExactly(first, second);
  }

  @ParameterizedTest
  @MethodSource("data_factory")
  public void test_toString(Object first, Object second) {
    Pair<Object, Object> test = Pair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertThat(test).hasToString(str);
  }

  public static Object[][] data_factoryNull() {
    return new Object[][] {
        {null, null},
        {null, "B"},
        {"A", null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factoryNull")
  public void test_of_null(Object first, Object second) {
    assertThatIllegalArgumentException().isThrownBy(() -> Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combining() {
    Pair<Integer, Integer> summed = Stream.of(Pair.of(10, 11), Pair.of(10, 11))
        .reduce(Pair.of(0, 0), Pair.combining(Integer::sum, Integer::sum));
    assertThat(summed).isEqualTo(Pair.of(20, 22));
  }

  @Test
  public void test_combinedWith() {
    Pair<String, String> combined = Pair.of("1", "2").combinedWith(Pair.of("A", "B"), String::concat, String::concat);
    assertThat(combined).isEqualTo(Pair.of("1A", "2B"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    Pair<String, String> ab = Pair.of("A", "B");
    Pair<String, String> ad = Pair.of("A", "D");
    Pair<String, String> ba = Pair.of("B", "A");

    List<Pair<String, String>> list = new ArrayList<>();
    list.add(ab);
    list.add(ad);
    list.add(ba);
    list.sort(Comparator.naturalOrder());
    assertThat(list).containsExactly(ab, ad, ba);
    list.sort(Comparator.reverseOrder());
    assertThat(list).containsExactly(ba, ad, ab);
  }

  @Test
  public void test_compareTo_notComparable() {
    Runnable notComparable = () -> {};
    Pair<Runnable, String> test1 = Pair.of(notComparable, "A");
    Pair<Runnable, String> test2 = Pair.of(notComparable, "B");
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> test1.compareTo(test2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    Pair<Integer, String> a = Pair.of(1, "Hello");
    Pair<Integer, String> a2 = Pair.of(1, "Hello");
    Pair<Integer, String> b = Pair.of(1, "Goodbye");
    Pair<Integer, String> c = Pair.of(2, "Hello");
    Pair<Integer, String> d = Pair.of(2, "Goodbye");

    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo(c)
        .isNotEqualTo(d)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
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
  public void test_toString() {
    Pair<String, String> test = Pair.of("A", "B");
    assertThat(test).hasToString("[A, B]");
  }

  @Test
  public void coverage() {
    Pair<String, String> test = Pair.of("A", "B");
    TestHelper.coverImmutableBean(test);
  }

}
