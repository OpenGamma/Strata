/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.pricer.rate.future.IborFutureDummyData.IBOR_FUTURE_TRADE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Tests {@link BaseIborFuturePricer}.
 */
@Test
public class BaseIborFutureProductPricerTest {

  private static final BaseIborFuturePricer PRICER = new BaseIborFuturePricer();
  private static final DefaultIborFutureProductPricer PRICER_PRODUCT = DefaultIborFutureProductPricer.DEFAULT;
  private static final IborFuture FUTURE = IborFutureDummyData.IBOR_FUTURE;
  private static final double TOLERANCE_DELTA = 1.0E-5;
  private static final double TOLERANCE_PV = 1.0E-4;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER.marginIndex(IBOR_FUTURE_TRADE.getSecurity().getProduct(), currentPrice);
    double referencePriceIndex = PRICER.marginIndex(IBOR_FUTURE_TRADE.getSecurity().getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * IBOR_FUTURE_TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER.presentValue(currentPrice, IBOR_FUTURE_TRADE, referencePrice);
    assertEquals(presentValueComputed.getAmount(), presentValueExpected, TOLERANCE_PV);
  }

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
    PointSensitivities priceSensitivity = PRICER_PRODUCT.priceSensitivity(new MockPricingEnvironment(), FUTURE);
    PointSensitivities sensiComputed = PRICER.marginIndexSensitivity(FUTURE, priceSensitivity).normalized();
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_DELTA));
  }

}
