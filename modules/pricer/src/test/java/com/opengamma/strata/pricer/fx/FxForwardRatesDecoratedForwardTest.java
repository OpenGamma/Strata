/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;

/**
 * Tests {@link FxForwardRatesDecoratedForward}.
 */
@Test
public class FxForwardRatesDecoratedForwardTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_FWD = LocalDate.of(2015, 6, 5);
  
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final CurveMetadata METADATA1 = Curves.zeroRates("TestCurve", ACT_365F);
  private static final CurveMetadata METADATA2 = Curves.zeroRates("TestCurveUSD", ACT_365F);
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(METADATA1, DoubleArray.of(0, 0.25, 0.50, 1.0, 2.0, 10.0),
          DoubleArray.of(0.01, 0.02, 0.01, 0.02, 0.01, 0.02), 
          INTERPOLATOR, EXTRAPOLATOR_FLAT, EXTRAPOLATOR_FLAT);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA2, DoubleArray.of(0, 0.35, 0.60, 1.1, 2.1, 10.1),
          DoubleArray.of(0.012, 0.022, 0.012, 0.022, 0.012, 0.022),  
          INTERPOLATOR, EXTRAPOLATOR_FLAT, EXTRAPOLATOR_FLAT);
  private static final ZeroRateDiscountFactors DFCURVE_GBP = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE1);
  private static final ZeroRateDiscountFactors DFCURVE_USD = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE2);
  
  private static final DiscountFxForwardRates FX_FWD =
      DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
  private static final FxForwardRatesDecoratedForward FX_FWD_FWD =
      FxForwardRatesDecoratedForward.of(FX_FWD, DATE_FWD);

  private static final double TOLERANCE = 1.0E-8;

  public void ccy_param() {
    assertEquals(FX_FWD_FWD.getParameterCount(), FX_FWD.getParameterCount());
    assertEquals(FX_FWD_FWD.getParameter(2), FX_FWD.getParameter(2));
    assertEquals(FX_FWD_FWD.getParameterMetadata(2), FX_FWD.getParameterMetadata(2));
    assertEquals(FX_FWD_FWD.getCurrencyPair(), FX_FWD.getCurrencyPair());
  }

  public void fx_fwd_rate() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      double dfComputed = FX_FWD_FWD.rate(USD, testDate);
      double dfExpected = FX_FWD.rate(USD, testDate);
      assertEquals(dfComputed, dfExpected, TOLERANCE);
    }
  }

  public void with_param() {
    double newParam = 0.12345;
    int paramIndex = 2;
    FxForwardRates dfWithParam = FX_FWD_FWD.withParameter(paramIndex, newParam);
    assertEquals(dfWithParam.getParameter(paramIndex), newParam, TOLERANCE);
  }

}
