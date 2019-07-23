/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link TriPredicate}.
 */
public class TriPredicateTest {

  @Test
  public void test_and() {
    TriPredicate<Integer, Integer, Integer> lessThan8 = (a, b, c) -> (a + b + c) < 8;
    TriPredicate<Integer, Integer, Integer> greaterThan4 = (a, b, c) -> (a + b + c) > 4;
    assertThat(lessThan8.and(greaterThan4).test(5, 0, 0)).isTrue();
    assertThat(lessThan8.and(greaterThan4).test(4, 0, 0)).isFalse();
    assertThat(lessThan8.and(greaterThan4).test(8, 0, 0)).isFalse();
    assertThatNullPointerException().isThrownBy(() -> lessThan8.and(null));
  }

  @Test
  public void test_or() {
    TriPredicate<Integer, Integer, Integer> greaterThan8 = (a, b, c) -> (a + b + c) > 8;
    TriPredicate<Integer, Integer, Integer> lessThan4 = (a, b, c) -> (a + b + c) < 4;
    assertThat(greaterThan8.or(lessThan4).test(3, 0, 0)).isTrue();
    assertThat(greaterThan8.or(lessThan4).test(9, 0, 0)).isTrue();
    assertThat(greaterThan8.or(lessThan4).test(4, 0, 0)).isFalse();
    assertThat(greaterThan8.or(lessThan4).test(8, 0, 0)).isFalse();
    assertThatNullPointerException().isThrownBy(() -> greaterThan8.or(null));
  }

  @Test
  public void test_not() {
    TriPredicate<Integer, Integer, Integer> greaterThan8 = (a, b, c) -> (a + b + c) > 8;
    TriPredicate<Integer, Integer, Integer> notGreaterThan8 = greaterThan8.negate();
    assertThat(notGreaterThan8.test(7, 0, 0)).isTrue();
  }

}
