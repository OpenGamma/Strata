/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.product.index.IborFuture;

/**
 * Tests {@link AbstractIborFutureTradePricer}.
 */
@Test
public class AbstractIborFutureProductPricerTest {

  private static final AbstractIborFutureProductPricer PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final DiscountingIborFutureProductPricer PRICER_PRODUCT = DiscountingIborFutureProductPricer.DEFAULT;
  private static final IborFuture FUTURE = IborFutureDummyData.IBOR_FUTURE;
  private static final double TOLERANCE_DELTA = 1.0E-5;

  //------------------------------------------------------------------------- 
  public void test_marginIndex() {
    double notional = FUTURE.getNotional();
    double accrualFactor = FUTURE.getAccrualFactor();
    double price = 0.99;
    double marginIndexExpected = price * notional * accrualFactor;
    double marginIndexComputed = PRICER.marginIndex(FUTURE, price);
    assertEquals(marginIndexComputed, marginIndexExpected);
  }

  //-------------------------------------------------------------------------
  public void test_marginIndexSensitivity() {
    double notional = FUTURE.getNotional();
    double accrualFactor = FUTURE.getAccrualFactor();
    PointSensitivities sensiExpected = PointSensitivities.of(
        IborRateSensitivity.of(FUTURE.getIndex(), FUTURE.getFixingDate(), -notional * accrualFactor));
    PointSensitivities priceSensitivity = PRICER_PRODUCT.priceSensitivity(FUTURE, new MockRatesProvider());
    PointSensitivities sensiComputed = PRICER.marginIndexSensitivity(FUTURE, priceSensitivity).normalized();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_DELTA));
  }

}
