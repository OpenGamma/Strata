/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;

/**
 * Test {@link ZeroRateDiscountFactors}.
 */
@Test
public class ZeroRateDiscountFactorsTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final YieldCurve CURVE = YieldCurve.from(
      InterpolatedDoublesCurve.fromSorted(
          new double[] {0, 10},
          new double[] {1, 2},
          Interpolator1DFactory.LINEAR_INSTANCE,
          NAME.toString()));

  //-------------------------------------------------------------------------
  public void test_of() {
    ZeroRateDiscountFactors test = ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getCurve(), CURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_discountFactor() {
    ZeroRateDiscountFactors test = ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE);
    double relativeTime = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double expected = CURVE.getDiscountFactor(relativeTime);
    assertEquals(test.discountFactor(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_pointSensitivity() {
    ZeroRateDiscountFactors test = ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE);
    double relativeTime = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double df = CURVE.getDiscountFactor(relativeTime);
    ZeroRateSensitivity expected = ZeroRateSensitivity.of(GBP, DATE_AFTER, -df * relativeTime);
    assertEquals(test.pointSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity() {
    ZeroRateDiscountFactors test = ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE);
    double relativeTime = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double[] expected = CURVE.getInterestRateParameterSensitivity(relativeTime);
    assertEquals(test.parameterSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ZeroRateDiscountFactors test = ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE);
    coverImmutableBean(test);
    ZeroRateDiscountFactors test2 = ZeroRateDiscountFactors.of(USD, DATE_VAL.plusDays(1), ACT_360, CURVE);
    coverBeanEquals(test, test2);
  }

}
