/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link KnownAmountRateComputation}.
 */
@Test
public class KnownAmountRateComputationTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);

  //-------------------------------------------------------------------------
  public void test_of() {
    KnownAmountRateComputation test = KnownAmountRateComputation.of(GBP_P1000);
    assertEquals(test.getAmount(), GBP_P1000);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices_simple() {
    KnownAmountRateComputation test = KnownAmountRateComputation.of(GBP_P1000);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    KnownAmountRateComputation test = KnownAmountRateComputation.of(GBP_P1000);
    coverImmutableBean(test);
    KnownAmountRateComputation test2 = KnownAmountRateComputation.of(GBP_P1000.plus(100));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    KnownAmountRateComputation test = KnownAmountRateComputation.of(GBP_P1000);
    assertSerialization(test);
  }

}
