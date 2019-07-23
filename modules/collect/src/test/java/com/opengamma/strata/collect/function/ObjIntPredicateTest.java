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
 * Test ObjIntPredicate.
 */
public class ObjIntPredicateTest {

  @Test
  public void test_and() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    ObjIntPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjIntPredicate<String> and = fn1.and(fn2);
    assertThat(fn1.test("a", 2)).isFalse();
    assertThat(fn1.test("a", 4)).isTrue();
    assertThat(fn2.test("a", 4)).isFalse();
    assertThat(fn2.test("abcd", 4)).isTrue();
    assertThat(and.test("a", 2)).isFalse();
    assertThat(and.test("a", 4)).isFalse();
    assertThat(and.test("abcd", 2)).isFalse();
    assertThat(and.test("abcd", 4)).isTrue();
  }

  @Test
  public void test_and_null() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    assertThatNullPointerException().isThrownBy(() -> fn1.and(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_or() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    ObjIntPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjIntPredicate<String> or = fn1.or(fn2);
    assertThat(fn1.test("a", 2)).isFalse();
    assertThat(fn1.test("a", 4)).isTrue();
    assertThat(fn2.test("a", 4)).isFalse();
    assertThat(fn2.test("abcd", 4)).isTrue();
    assertThat(or.test("a", 2)).isFalse();
    assertThat(or.test("a", 4)).isTrue();
    assertThat(or.test("abcd", 2)).isTrue();
    assertThat(or.test("abcd", 4)).isTrue();
  }

  @Test
  public void test_or_null() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    assertThatNullPointerException().isThrownBy(() -> fn1.or(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negate() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    ObjIntPredicate<String> negate = fn1.negate();
    assertThat(fn1.test("a", 2)).isFalse();
    assertThat(fn1.test("a", 4)).isTrue();
    assertThat(negate.test("a", 2)).isTrue();
    assertThat(negate.test("a", 4)).isFalse();
  }

}
