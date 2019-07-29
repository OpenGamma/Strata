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
 * Test {@link TriConsumer}.
 */
public class TriConsumerTest {

  @Test
  public void test_andThen() {
    boolean[] called = new boolean[1];
    TriConsumer<Integer, Integer, Integer> consumer = (a, b, c) -> called[0] = true;
    consumer.accept(2, 3, 4);
    assertThat(called[0]).isTrue();
    assertThatNullPointerException().isThrownBy(() -> consumer.andThen(null));
  }

}
