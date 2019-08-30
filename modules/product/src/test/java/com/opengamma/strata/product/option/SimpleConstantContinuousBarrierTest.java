/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SimpleConstantContinuousBarrier}.
 */
public class SimpleConstantContinuousBarrierTest {

  @Test
  public void test_of() {
    double level = 1.5;
    SimpleConstantContinuousBarrier test =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, level);
    assertThat(test.getBarrierLevel()).isEqualTo(level);
    assertThat(test.getBarrierLevel(LocalDate.of(2015, 1, 21))).isEqualTo(level);
    assertThat(test.getBarrierType()).isEqualTo(BarrierType.DOWN);
    assertThat(test.getKnockType()).isEqualTo(KnockType.KNOCK_IN);
  }

  @Test
  public void test_inverseKnockType() {
    double level = 1.5;
    SimpleConstantContinuousBarrier base =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, level);
    SimpleConstantContinuousBarrier test = base.inverseKnockType();
    SimpleConstantContinuousBarrier expected =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, level);
    assertThat(test).isEqualTo(expected);
    assertThat(test.inverseKnockType()).isEqualTo(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleConstantContinuousBarrier test1 =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.5);
    SimpleConstantContinuousBarrier test2 =
        SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, 2.1);
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SimpleConstantContinuousBarrier test =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.5);
    assertSerialization(test);
  }

}
