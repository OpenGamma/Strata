/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;

/**
 * Tests {@link DiscountFactorsDecoratedForward}.
 */
@Test
public class DiscountFactorsDecoratedForwardTest {

  private static final LocalDate DATE_VAL = LocalDate.of(2015, 6, 4);
  private static final LocalDate DATE_FWD = LocalDate.of(2015, 6, 5);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final DoubleArray TIMES = DoubleArray.of(0, 0.25, 0.50, 1.0, 2.0, 10);
  private static final DoubleArray ZC = DoubleArray.of(0.01, 0.02, 0.01, 0.02, 0.01, 0.02);
  private static final InterpolatedNodalCurve CURVE_ZERO = InterpolatedNodalCurve.of(
      Curves.zeroRates(NAME, ACT_365F), TIMES, ZC,
      INTERPOLATOR, EXTRAPOLATOR_FLAT, EXTRAPOLATOR_FLAT);
  private static final DiscountFactors DF_START = DiscountFactors.of(GBP, DATE_VAL, CURVE_ZERO);
  private static final DiscountFactorsDecoratedForward DF_FWD = DiscountFactorsDecoratedForward.of(DF_START, DATE_FWD);

  private static final double TOLERANCE = 1.0E-8;
  private static final double TOLERANCE_DELTA = 1.0E-6;

  public void date_ccy_param() {
    assertEquals(DF_FWD.getValuationDate(), DATE_FWD);
    assertEquals(DF_FWD.getCurrency(), GBP);
    assertEquals(DF_FWD.getParameterCount(), DF_START.getParameterCount());
    assertEquals(DF_FWD.getParameter(2), DF_START.getParameter(2));
    assertEquals(DF_FWD.getParameterMetadata(2), DF_START.getParameterMetadata(2));
  }

  public void discount_factor() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    double dfFwd = DF_START.discountFactor(DATE_FWD);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      double dfComputed = DF_FWD.discountFactor(testDate);
      double dfExpected = DF_START.discountFactor(testDate) / dfFwd;
      assertEquals(dfComputed, dfExpected, TOLERANCE);
      double yf = DF_FWD.relativeYearFraction(testDate);
      double dfComputedYf = DF_FWD.discountFactor(yf);
      assertEquals(dfComputedYf, dfExpected, TOLERANCE);
      assertEquals(yf, DF_START.relativeYearFraction(testDate) - DF_START.relativeYearFraction(DATE_FWD), TOLERANCE);
    }
  }

  public void zero_rate() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      double zrComputed = DF_FWD.zeroRate(testDate);
      double yf = DF_FWD.relativeYearFraction(testDate);
      double yfEffective = Math.max(1.0E-10, yf);
      double zrExpected = -1.0 / yfEffective * Math.log(DF_FWD.discountFactor(yfEffective));
      assertEquals(zrComputed, zrExpected, TOLERANCE);
      double zrComputedYf = DF_FWD.zeroRate(yf);
      assertEquals(zrComputedYf, zrExpected, TOLERANCE);
    }
  }  

  public void with_param() {
    double newParam = 0.12345;
    int paramIndex = 2;
    DiscountFactors dfWithParam = DF_FWD.withParameter(paramIndex, newParam);
    assertEquals(dfWithParam.getParameter(paramIndex), newParam, TOLERANCE);
  }

  public void zero_rate_sensitivity() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      ZeroRateSensitivity zrsComputed = DF_FWD.zeroRatePointSensitivity(testDate);
      double t = DF_FWD.relativeYearFraction(testDate);
      double df = DF_FWD.discountFactor(testDate);
      assertEquals(zrsComputed.getSensitivity(), -t * df, TOLERANCE);
      assertEquals(zrsComputed.getYearFraction(), t, TOLERANCE);
    }
  }

  // Compare parameter sensitivity to a finite difference computation.
  public void parameter_sensitivity() {
    double sensiFactor = 123.456;
    final double shift = 1.0E-4;
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      ZeroRateSensitivity zrsComputed = DF_FWD.zeroRatePointSensitivity(testDate, USD).multipliedBy(sensiFactor);
      CurrencyParameterSensitivities psComputed = DF_FWD.parameterSensitivity(zrsComputed);
      if (i == 0) {
        assertEquals(psComputed.size(), 0); // No sensitivity at valuation date
      } else {
        DoubleArray psComputedArray = psComputed.getSensitivities().get(0).getSensitivity();
        for (int loopparam = 0; loopparam < ZC.size(); loopparam++) {
          double[] dfMP = new double[2];
          for (int j = 0; j < 2; j++) {
            final int loopparam2 = loopparam;
            final int j2 = j;
            DiscountFactors dfShifted = DF_FWD
                .withPerturbation((idx, value, meta) -> (idx == loopparam2) ? value + (-1 + 2 * j2) * shift : value);
            DiscountFactorsDecoratedForward dfFwd = DiscountFactorsDecoratedForward.of(dfShifted, DATE_FWD);
            dfMP[j] = dfFwd.discountFactor(testDate);
          }
          double sensiExpected = sensiFactor * (dfMP[1] - dfMP[0]) / (2 * shift);
          assertEquals(psComputedArray.get(loopparam), sensiExpected, Math.abs(TOLERANCE_DELTA * sensiExpected));
        }
      }
    }
  }

}
