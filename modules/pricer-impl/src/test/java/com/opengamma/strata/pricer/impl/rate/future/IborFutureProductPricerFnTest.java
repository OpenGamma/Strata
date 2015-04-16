/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.future;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.future.IborFutureProductPricerFn;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Tests {@link IborFutureProductPricerFn}.
 */
public class IborFutureProductPricerFnTest {

  private static final IborFutureProductPricerFn PRICER = DefaultIborFutureProductPricerFn.DEFAULT;
  private static final IborFuture FUTURE = IborFutureDummyData.IBOR_FUTURE;
  private static final double TOLERANCE_DELTA = 1.0E-5;

  //------------------------------------------------------------------------- 
  @Test
  public void test_marginIndex() {
    double notional = FUTURE.getNotional();
    double accrualFactor = FUTURE.getAccrualFactor();
    double price = 0.99;
    double marginIndexExpected = price * notional * accrualFactor;
    double marginIndexComputed = PRICER.marginIndex(FUTURE, price);
    assertEquals(marginIndexComputed, marginIndexExpected);
  }

  @Test
  public void test_marginIndexSensitivity() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    double notional = FUTURE.getNotional();
    double accrualFactor = FUTURE.getAccrualFactor();
    PointSensitivities sensiExpected = PointSensitivities.of(
        IborRateSensitivity.of(FUTURE.getIndex(), FUTURE.getFixingDate(), -notional * accrualFactor));
    PointSensitivities sensiComputed =
        PRICER.marginIndexSensitivity(FUTURE, PRICER.priceSensitivity(mockEnv, FUTURE)).normalized();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_DELTA));
  }

}
