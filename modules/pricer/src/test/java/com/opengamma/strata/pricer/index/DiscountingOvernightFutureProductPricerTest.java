/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.rate.DispatchingRateComputationFn;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.ResolvedOvernightFuture;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link DiscountingOvernightFutureProductPricer}.
 */
@Test
public class DiscountingOvernightFutureProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION = LocalDate.of(2018, 7, 12);
  private static final double NOTIONAL = 5_000_000d;
  private static final double ACCRUAL_FACTOR = TENOR_1M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2018, 9, 28);
  private static final LocalDate START_DATE = date(2018, 9, 1);
  private static final LocalDate END_DATE = date(2018, 9, 30);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(5);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "OnFuture");
  private static final ResolvedOvernightFuture FUTURE = OvernightFuture.builder()
      .securityId(SECURITY_ID)
      .currency(USD)
      .notional(NOTIONAL)
      .accrualFactor(ACCRUAL_FACTOR)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(USD_FED_FUND)
      .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
      .rounding(ROUNDING)
      .build()
      .resolve(REF_DATA);

  private static final DoubleArray TIME = DoubleArray.of(0.02, 0.08, 0.25, 0.5);
  private static final DoubleArray RATE = DoubleArray.of(0.01, 0.015, 0.008, 0.005);
  private static final Curve CURVE = InterpolatedNodalCurve.of(
      Curves.zeroRates("FED-FUND", DayCounts.ACT_365F), TIME, RATE, CurveInterpolators.NATURAL_SPLINE);
  private static final RatesProvider RATES_PROVIDER = ImmutableRatesProvider.builder(VALUATION)
      .indexCurve(USD_FED_FUND, CURVE)
      .build();

  private static final double TOL = 1.0e-14;
  private static final double EPS = 1.0e-6;
  private static final DiscountingOvernightFutureProductPricer PRICER = DiscountingOvernightFutureProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALC = new RatesFiniteDifferenceSensitivityCalculator(EPS);

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
    PointSensitivities priceSensitivity = PRICER.priceSensitivity(FUTURE, RATES_PROVIDER);
    PointSensitivities sensiComputed = PRICER.marginIndexSensitivity(FUTURE, priceSensitivity);
    assertTrue(sensiComputed.equalWithTolerance(priceSensitivity.multipliedBy(accrualFactor * notional), TOL * notional));
  }

  //------------------------------------------------------------------------- 
  public void test_price() {
    double computed = PRICER.price(FUTURE, RATES_PROVIDER);
    double rate = DispatchingRateComputationFn.DEFAULT.rate(FUTURE.getOvernightRate(), START_DATE, END_DATE, RATES_PROVIDER);
    double expected = 1d - rate;
    assertEquals(computed, expected, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_priceSensitivity() {
    PointSensitivities points = PRICER.priceSensitivity(FUTURE, RATES_PROVIDER);
    CurrencyParameterSensitivities computed = RATES_PROVIDER.parameterSensitivity(points);
    CurrencyParameterSensitivities expected = FD_CALC.sensitivity(
        RATES_PROVIDER, r -> CurrencyAmount.of(USD, PRICER.price(FUTURE, r)));
    assertTrue(computed.equalWithTolerance(expected, EPS));
  }

}
