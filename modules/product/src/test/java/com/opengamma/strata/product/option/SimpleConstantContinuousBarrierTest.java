/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link SimpleConstantContinuousBarrier}.
 */
@Test
public class SimpleConstantContinuousBarrierTest {

  public void test_of() {
    double level = 1.5;
    SimpleConstantContinuousBarrier test =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, level);
    assertEquals(test.getBarrierLevel(), level);
    assertEquals(test.getBarrierLevel(LocalDate.of(2015, 1, 21)), level);
    assertEquals(test.getBarrierType(), BarrierType.DOWN);
    assertEquals(test.getKnockType(), KnockType.KNOCK_IN);
  }

  public void test_inverseKnockType() {
    double level = 1.5;
    SimpleConstantContinuousBarrier base =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, level);
    SimpleConstantContinuousBarrier test = base.inverseKnockType();
    SimpleConstantContinuousBarrier expected =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, level);
    assertEquals(test, expected);
    assertEquals(test.inverseKnockType(), base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleConstantContinuousBarrier test1 =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.5);
    SimpleConstantContinuousBarrier test2 =
        SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, 2.1);
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SimpleConstantContinuousBarrier test =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.5);
    assertSerialization(test);
  }

}
