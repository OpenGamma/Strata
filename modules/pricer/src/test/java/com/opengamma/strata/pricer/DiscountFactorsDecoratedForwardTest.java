/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
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
  private static final InterpolatedNodalCurve CURVE_ZERO = InterpolatedNodalCurve.of(
      Curves.zeroRates(NAME, ACT_365F), DoubleArray.of(0, 0.25, 0.50, 1.0, 2.0, 10),
      DoubleArray.of(0.01, 0.02, 0.01, 0.02, 0.01, 0.02),
      INTERPOLATOR, EXTRAPOLATOR_FLAT, EXTRAPOLATOR_FLAT);
  private static final DiscountFactors DF_START = DiscountFactors.of(GBP, DATE_VAL, CURVE_ZERO);
  private static final DiscountFactorsDecoratedForward DF_FWD = DiscountFactorsDecoratedForward.of(DF_START, DATE_FWD);

  private static final double TOLERANCE_DF = 1.0E-8;

  public void date_ccy() {
    DiscountFactorsDecoratedForward test = DiscountFactorsDecoratedForward.of(DF_START, DATE_FWD);
    assertEquals(test.getValuationDate(), DATE_FWD);
    assertEquals(test.getCurrency(), GBP);
  }

  public void discount_factor() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    double dfFwd = DF_START.discountFactor(DATE_FWD);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      double dfComputed = DF_FWD.discountFactor(testDate);
      double dfExpected = DF_START.discountFactor(testDate) / dfFwd;
      assertEquals(dfComputed, dfExpected, TOLERANCE_DF);
      double yf = DF_FWD.relativeYearFraction(testDate);
      double dfComputedYf = DF_FWD.discountFactor(yf);
      assertEquals(dfComputedYf, dfExpected, TOLERANCE_DF);
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
      assertEquals(zrComputed, zrExpected, TOLERANCE_DF);
      double zrComputedYf = DF_FWD.zeroRate(yf);
      assertEquals(zrComputedYf, zrExpected, TOLERANCE_DF);
    }
  }

}
