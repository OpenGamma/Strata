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
 * Test {@link ObjDoubleToDoubleFunction}.
 */
public class ObjDoubleToDoubleFunctionTest {

  @Test
  public void test_andThen() {
    ObjDoubleToDoubleFunction<String> fn1 = (a, b) -> Double.parseDouble(a) + b;
    ObjDoubleToDoubleFunction<String> fn2 = fn1.andThen(val -> val + 4);
    assertThat(fn1.apply("2", 3.2d)).isEqualTo(5.2d);
    assertThat(fn2.apply("2", 3.2d)).isEqualTo(9.2d);
  }

  @Test
  public void test_andThen_null() {
    ObjDoubleToDoubleFunction<String> fn1 = (a, b) -> 6d;
    assertThatNullPointerException().isThrownBy(() -> fn1.andThen(null));
  }

}
