/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
public class FixedRateComputationTest {

  @Test
  public void test_of() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    assertThat(test.getRate()).isEqualTo(0.05);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    FixedRateComputation test = FixedRateComputation.of(0.05);
    assertSerialization(test);
  }

}
