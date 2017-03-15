/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
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
public class FixedRateComputationTest {

  public void test_of() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    assertEquals(test.getRate(), 0.05);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    assertSerialization(test);
  }

}
