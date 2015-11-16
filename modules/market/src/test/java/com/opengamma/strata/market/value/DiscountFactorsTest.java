/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.DiscountFactorsKey;

/**
 * Test {@link DiscountFactors}.
 */
@Test
public class DiscountFactorsTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final InterpolatedNodalCurve CURVE_DF = InterpolatedNodalCurve.of(
      Curves.discountFactors(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE_ZERO = InterpolatedNodalCurve.of(
      Curves.zeroRates(NAME, ACT_365F), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE_PRICES = InterpolatedNodalCurve.of(
      Curves.prices(NAME), DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);

  //-------------------------------------------------------------------------
  public void test_of_discountFactors() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE_DF);
    assertEquals(test instanceof SimpleDiscountFactors, true);
    assertEquals(test.getKey(), DiscountFactorsKey.of(GBP));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_of_zeroRate() {
    DiscountFactors test = DiscountFactors.of(GBP, DATE_VAL, CURVE_ZERO);
    assertEquals(test instanceof ZeroRateDiscountFactors, true);
    assertEquals(test.getKey(), DiscountFactorsKey.of(GBP));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_of_prices() {
    assertThrowsIllegalArg(() -> DiscountFactors.of(GBP, DATE_VAL, CURVE_PRICES));
  }

}
