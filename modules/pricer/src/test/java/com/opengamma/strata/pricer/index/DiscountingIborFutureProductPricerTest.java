/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.index.IborFuture;

/**
 * Test {@link DiscountingIborFutureTradePricer}.
 */
@Test
public class DiscountingIborFutureProductPricerTest {

  private static final DiscountingIborFutureProductPricer PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final IborFuture FUTURE = IborFutureDummyData.IBOR_FUTURE;

  private static final double RATE = 0.045;
  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;

  //------------------------------------------------------------------------- 
  public void test_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(FUTURE.getFixingDate())).thenReturn(RATE);

    assertEquals(PRICER.price(FUTURE, prov), 1.0 - RATE, TOLERANCE_PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    PointSensitivities sensiExpected =
        PointSensitivities.of(IborRateSensitivity.of(FUTURE.getIndex(), FUTURE.getFixingDate(), -1.0d));
    PointSensitivities sensiComputed = PRICER.priceSensitivity(FUTURE, prov);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA));
  }

}
