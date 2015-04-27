/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Test {@link DiscountingIborFutureTradePricer}.
 */
@Test
public class DiscountingIborFutureProductPricerTest {

  private static final DiscountingIborFutureProductPricer PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final IborFuture FUTURE = IborFutureDummyData.IBOR_FUTURE;

  private static final double RATE = 0.045;
  private static final PricingEnvironment ENV_MOCK = mock(PricingEnvironment.class);
  static {
    when(ENV_MOCK.iborIndexRate(FUTURE.getIndex(), FUTURE.getFixingDate())).thenReturn(RATE);
  }
  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;

  //------------------------------------------------------------------------- 
  public void test_price() {
    assertEquals(PRICER.price(ENV_MOCK, FUTURE), 1.0 - RATE, TOLERANCE_PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivity() {
    PointSensitivities sensiExpected =
        PointSensitivities.of(IborRateSensitivity.of(FUTURE.getIndex(), FUTURE.getFixingDate(), -1.0d));
    PointSensitivities sensiComputed = PRICER.priceSensitivity(ENV_MOCK, FUTURE);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA));
  }

}
