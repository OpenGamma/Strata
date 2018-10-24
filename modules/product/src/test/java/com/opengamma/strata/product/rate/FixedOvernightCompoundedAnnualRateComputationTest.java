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

/**
 * Test.
 */
@Test
public class FixedOvernightCompoundedAnnualRateComputationTest {
  
  public void test_of() {
    FixedOvernightCompoundedAnnualRateComputation test = FixedOvernightCompoundedAnnualRateComputation.of(0.05, 0.1);
    assertEquals(test.getRate(), 0.05);
    assertEquals(test.getAccrualFactor(), 0.1);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    FixedOvernightCompoundedAnnualRateComputation test = FixedOvernightCompoundedAnnualRateComputation.of(0.05, 0.1);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of());
  }
  
  //-------------------------------------------------------------------------
  public void test_simpleRate() {
    FixedOvernightCompoundedAnnualRateComputation test = FixedOvernightCompoundedAnnualRateComputation.of(0.05, 0.1);
    assertEquals(1 + test.getAccrualFactor() * test.simpleRate(), Math.pow(1 + test.getRate(), test.getAccrualFactor()));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedOvernightCompoundedAnnualRateComputation test = FixedOvernightCompoundedAnnualRateComputation.of(0.05, 0.1);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    FixedOvernightCompoundedAnnualRateComputation test = FixedOvernightCompoundedAnnualRateComputation.of(0.05, 0.1);
    assertSerialization(test);
  }
  
}
