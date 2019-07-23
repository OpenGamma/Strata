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
 * Test ObjLongFunction.
 */
public class ObjLongFunctionTest {

  @Test
  public void test_andThen() {
    ObjLongFunction<Integer, String> fn1 = (a, b) -> a + "=" + b;
    ObjLongFunction<Integer, String> fn2 = fn1.andThen(str -> "[" + str + "]");
    assertThat(fn1.apply(2, 3L)).isEqualTo("2=3");
    assertThat(fn2.apply(2, 3L)).isEqualTo("[2=3]");
  }

  @Test
  public void test_andThen_null() {
    ObjLongFunction<Integer, String> fn1 = (a, b) -> a + "=" + b;
    assertThatNullPointerException().isThrownBy(() -> fn1.andThen(null));
  }

}
