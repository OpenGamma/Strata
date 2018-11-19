/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.TestHelper;

/**
 * Test {@link FixedOvernightCompoundedAnnualRateComputation}.
 */
@Test
public class FixedOvernightCompoundedAnnualRateComputationTest {

  public void test_of() {
    FixedOvernightCompoundedAnnualRateComputation test = sut();
    assertEquals(test.getRate(), 0.05);
    assertEquals(test.getAccrualFactor(), 0.1);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    FixedOvernightCompoundedAnnualRateComputation test = sut();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void test_getSimpleRate() {
    FixedOvernightCompoundedAnnualRateComputation test = sut();
    assertEquals(
        1 + test.getAccrualFactor() * test.getSimpleRate(),
        Math.pow(1 + test.getRate(), test.getAccrualFactor()));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedOvernightCompoundedAnnualRateComputation test = sut();
    coverImmutableBean(test);
    FixedOvernightCompoundedAnnualRateComputation test2 = sut2();
    TestHelper.coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FixedOvernightCompoundedAnnualRateComputation test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  private FixedOvernightCompoundedAnnualRateComputation sut() {
    return FixedOvernightCompoundedAnnualRateComputation.of(0.05, 0.1);
  }

  private FixedOvernightCompoundedAnnualRateComputation sut2() {
    return FixedOvernightCompoundedAnnualRateComputation.of(0.15, 0.2);
  }

}
