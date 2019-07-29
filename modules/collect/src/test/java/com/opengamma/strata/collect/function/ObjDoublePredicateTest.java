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
 * Test ObjDoublePredicate.
 */
public class ObjDoublePredicateTest {

  @Test
  public void test_and() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    ObjDoublePredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjDoublePredicate<String> and = fn1.and(fn2);
    assertThat(fn1.test("a", 2.3d)).isFalse();
    assertThat(fn1.test("a", 3.2d)).isTrue();
    assertThat(fn2.test("a", 3.2d)).isFalse();
    assertThat(fn2.test("abcd", 3.2d)).isTrue();
    assertThat(and.test("a", 2.3d)).isFalse();
    assertThat(and.test("a", 3.2d)).isFalse();
    assertThat(and.test("abcd", 2.3d)).isFalse();
    assertThat(and.test("abcd", 3.2d)).isTrue();
  }

  @Test
  public void test_and_null() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    assertThatNullPointerException().isThrownBy(() -> fn1.and(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_or() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    ObjDoublePredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjDoublePredicate<String> or = fn1.or(fn2);
    assertThat(fn1.test("a", 2.3d)).isFalse();
    assertThat(fn1.test("a", 3.2d)).isTrue();
    assertThat(fn2.test("a", 3.2d)).isFalse();
    assertThat(fn2.test("abcd", 3.2d)).isTrue();
    assertThat(or.test("a", 2.3d)).isFalse();
    assertThat(or.test("a", 3.2d)).isTrue();
    assertThat(or.test("abcd", 2.3d)).isTrue();
    assertThat(or.test("abcd", 3.2d)).isTrue();
  }

  @Test
  public void test_or_null() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    assertThatNullPointerException().isThrownBy(() -> fn1.or(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negate() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    ObjDoublePredicate<String> negate = fn1.negate();
    assertThat(fn1.test("a", 2.3d)).isFalse();
    assertThat(fn1.test("a", 3.2d)).isTrue();
    assertThat(negate.test("a", 2.3d)).isTrue();
    assertThat(negate.test("a", 3.2d)).isFalse();
  }

}
