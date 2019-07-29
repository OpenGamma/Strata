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
 * Test {@link TriFunction}.
 */
public class TriFunctionTest {

  @Test
  public void test_andThen() {
    TriFunction<Integer, String, Integer, String> fn1 = (a, b, c) -> a + "=" + b + "=" + c;
    TriFunction<Integer, String, Integer, String> fn2 = fn1.andThen(str -> "[" + str + "]");
    assertThat(fn1.apply(2, "A", 4)).isEqualTo("2=A=4");
    assertThat(fn2.apply(2, "B", 4)).isEqualTo("[2=B=4]");
    assertThatNullPointerException().isThrownBy(() -> fn1.andThen(null));
  }

}
