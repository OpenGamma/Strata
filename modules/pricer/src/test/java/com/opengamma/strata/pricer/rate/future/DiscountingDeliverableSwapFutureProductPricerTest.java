/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingDeliverableSwapFutureProductPricer}.
 */
@Test
public class DiscountingDeliverableSwapFutureProductPricerTest {
  private static final ImmutableRatesProvider PROVIDER = DeliverableSwapFuturePricerTestDataSets.RATES_PROVIDER;
  private static final Swap SWAP = DeliverableSwapFuturePricerTestDataSets.SWAP;
  private static final DeliverableSwapFuture FUTURE = DeliverableSwapFuturePricerTestDataSets.SWAP_FUTURE;
  private static final double TOL = 1.0e-13;
  private static final double EPS = 1.0e-6;
  private static final DiscountingDeliverableSwapFutureProductPricer PRICER =
      DiscountingDeliverableSwapFutureProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  //-------------------------------------------------------------------------
  public void test_price() {
    double computed = PRICER.price(FUTURE, PROVIDER);
    double pvSwap = PRICER.getSwapPricer().presentValue(SWAP, PROVIDER).getAmount(USD).getAmount();
    double yc = ACT_ACT_ISDA.relativeYearFraction(
        DeliverableSwapFuturePricerTestDataSets.VALUATION,
        DeliverableSwapFuturePricerTestDataSets.DELIVERY);
    double df = Math.exp(-DeliverableSwapFuturePricerTestDataSets.USD_DSC.yValue(yc) * yc);
    double expected = 1d + pvSwap / df;
    assertEquals(computed, expected, TOL);
  }

  public void test_priceSensitivity() {
    PointSensitivities point = PRICER.priceSensitivity(FUTURE, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected =
        FD_CAL.sensitivity(PROVIDER, (p) -> CurrencyAmount.of(USD, PRICER.price(FUTURE, (p))));
    assertTrue(computed.equalWithTolerance(expected, 10d * EPS));
  }

  //-------------------------------------------------------------------------
  public void regression() {
    double price = PRICER.price(FUTURE, PROVIDER);
    assertEquals(price, 1.022245377054993, TOL); // 2.x
  }

}
