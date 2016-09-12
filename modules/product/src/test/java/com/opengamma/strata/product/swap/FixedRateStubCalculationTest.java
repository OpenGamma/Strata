/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.rate.FixedRateComputation;

/**
 * Test {@link FixedRateStubCalculation}.
 */
@Test
public class FixedRateStubCalculationTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);

  //-------------------------------------------------------------------------
  public void test_ofFixedRate() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    assertEquals(test.getFixedRate(), OptionalDouble.of(0.025d));
    assertEquals(test.getKnownAmount(), Optional.empty());
    assertEquals(test.isFixedRate(), true);
    assertEquals(test.isKnownAmount(), false);
  }

  public void test_ofKnownAmount() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertEquals(test.getFixedRate(), OptionalDouble.empty());
    assertEquals(test.getKnownAmount(), Optional.of(GBP_P1000));
    assertEquals(test.isFixedRate(), false);
    assertEquals(test.isKnownAmount(), true);
  }

  //-------------------------------------------------------------------------
  public void test_builder_invalid_fixedAndKnown() {
    assertThrowsIllegalArg(() -> FixedRateStubCalculation.meta().builder()
        .set(FixedRateStubCalculation.meta().fixedRate(), 0.025d)
        .set(FixedRateStubCalculation.meta().knownAmount(), GBP_P1000)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_createRateComputation_NONE() {
    FixedRateStubCalculation test = FixedRateStubCalculation.NONE;
    assertEquals(test.createRateComputation(3d), FixedRateComputation.of(3d));
  }

  public void test_createRateComputation_fixedRate() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    assertEquals(test.createRateComputation(3d), FixedRateComputation.of(0.025d));
  }

  public void test_createRateComputation_knownAmount() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofKnownAmount(GBP_P1000);
    assertEquals(test.createRateComputation(3d), KnownAmountRateComputation.of(GBP_P1000));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    coverImmutableBean(test);
    FixedRateStubCalculation test2 = FixedRateStubCalculation.ofKnownAmount(GBP_P1000);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FixedRateStubCalculation test = FixedRateStubCalculation.ofFixedRate(0.025d);
    assertSerialization(test);
  }

}
