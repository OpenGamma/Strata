/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;

/**
 * Test {@link RepoCurveDiscountFactors}.
 */
@Test
public class RepoCurveDiscountFactorsTest {

  private static final LocalDate DATE = date(2015, 6, 4);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, ACT_365F);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS = ZeroRateDiscountFactors.of(GBP, DATE, CURVE);
  private static final BondGroup GROUP = BondGroup.of("ISSUER1 BND 5Y");

  public void test_of() {
    RepoCurveDiscountFactors test = RepoCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    assertEquals(test.getBondGroup(), GROUP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getValuationDate(), DATE);
    assertEquals(test.discountFactor(DATE_AFTER), DSC_FACTORS.discountFactor(DATE_AFTER));
  }

  public void test_zeroRatePointSensitivity() {
    RepoCurveDiscountFactors base = RepoCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    RepoCurveZeroRateSensitivity expected =
        RepoCurveZeroRateSensitivity.of(DSC_FACTORS.zeroRatePointSensitivity(DATE_AFTER), GROUP);
    RepoCurveZeroRateSensitivity computed = base.zeroRatePointSensitivity(DATE_AFTER);
    assertEquals(computed, expected);
  }

  public void test_zeroRatePointSensitivity_USD() {
    RepoCurveDiscountFactors base = RepoCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    RepoCurveZeroRateSensitivity expected =
        RepoCurveZeroRateSensitivity.of(DSC_FACTORS.zeroRatePointSensitivity(DATE_AFTER, USD), GROUP);
    RepoCurveZeroRateSensitivity computed = base.zeroRatePointSensitivity(DATE_AFTER, USD);
    assertEquals(computed, expected);
  }

  public void test_parameterSensitivity() {
    RepoCurveDiscountFactors base = RepoCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    RepoCurveZeroRateSensitivity sensi = base.zeroRatePointSensitivity(DATE_AFTER, USD);
    CurrencyParameterSensitivities computed = base.parameterSensitivity(sensi);
    CurrencyParameterSensitivities expected =
        DSC_FACTORS.parameterSensitivity(DSC_FACTORS.zeroRatePointSensitivity(DATE_AFTER, USD));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RepoCurveDiscountFactors test1 = RepoCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    coverImmutableBean(test1);
    RepoCurveDiscountFactors test2 =
        RepoCurveDiscountFactors.of(ZeroRateDiscountFactors.of(USD, DATE, CURVE), BondGroup.of("ISSUER2"));
    coverBeanEquals(test1, test2);
  }

}
