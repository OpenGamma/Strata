/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/**
 * Test ObjLongPredicate.
 */
public class ObjLongPredicateTest {

  @Test
  public void test_and() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    ObjLongPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjLongPredicate<String> and = fn1.and(fn2);
    assertThat(fn1.test("a", 2L)).isFalse();
    assertThat(fn1.test("a", 4L)).isTrue();
    assertThat(fn2.test("a", 4L)).isFalse();
    assertThat(fn2.test("abcd", 4L)).isTrue();
    assertThat(and.test("a", 2L)).isFalse();
    assertThat(and.test("a", 4L)).isFalse();
    assertThat(and.test("abcd", 2L)).isFalse();
    assertThat(and.test("abcd", 4L)).isTrue();
  }

  @Test
  public void test_and_null() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    assertThatNullPointerException().isThrownBy(() -> fn1.and(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_or() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    ObjLongPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjLongPredicate<String> or = fn1.or(fn2);
    assertThat(fn1.test("a", 2L)).isFalse();
    assertThat(fn1.test("a", 4L)).isTrue();
    assertThat(fn2.test("a", 4L)).isFalse();
    assertThat(fn2.test("abcd", 4L)).isTrue();
    assertThat(or.test("a", 2L)).isFalse();
    assertThat(or.test("a", 4L)).isTrue();
    assertThat(or.test("abcd", 2L)).isTrue();
    assertThat(or.test("abcd", 4L)).isTrue();
  }

  @Test
  public void test_or_null() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    assertThatNullPointerException().isThrownBy(() -> fn1.or(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negate() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    ObjLongPredicate<String> negate = fn1.negate();
    assertThat(fn1.test("a", 2L)).isFalse();
    assertThat(fn1.test("a", 4L)).isTrue();
    assertThat(negate.test("a", 2L)).isTrue();
    assertThat(negate.test("a", 4L)).isFalse();
  }

}
