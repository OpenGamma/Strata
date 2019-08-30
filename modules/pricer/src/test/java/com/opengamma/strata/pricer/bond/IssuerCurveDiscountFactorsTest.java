/*
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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;

/**
 * Test {@link IssuerCurveDiscountFactors}.
 */
public class IssuerCurveDiscountFactorsTest {

  private static final LocalDate DATE = date(2015, 6, 4);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, ACT_365F);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final DiscountFactors DSC_FACTORS = ZeroRateDiscountFactors.of(GBP, DATE, CURVE);
  private static final LegalEntityGroup GROUP = LegalEntityGroup.of("ISSUER1");

  @Test
  public void test_of() {
    IssuerCurveDiscountFactors test = IssuerCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    assertThat(test.getLegalEntityGroup()).isEqualTo(GROUP);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValuationDate()).isEqualTo(DATE);
    assertThat(test.discountFactor(DATE_AFTER)).isEqualTo(DSC_FACTORS.discountFactor(DATE_AFTER));
  }

  @Test
  public void test_zeroRatePointSensitivity() {
    IssuerCurveDiscountFactors base = IssuerCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    IssuerCurveZeroRateSensitivity expected =
        IssuerCurveZeroRateSensitivity.of(DSC_FACTORS.zeroRatePointSensitivity(DATE_AFTER), GROUP);
    IssuerCurveZeroRateSensitivity computed = base.zeroRatePointSensitivity(DATE_AFTER);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_zeroRatePointSensitivity_USD() {
    IssuerCurveDiscountFactors base = IssuerCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    IssuerCurveZeroRateSensitivity expected =
        IssuerCurveZeroRateSensitivity.of(DSC_FACTORS.zeroRatePointSensitivity(DATE_AFTER, USD), GROUP);
    IssuerCurveZeroRateSensitivity computed = base.zeroRatePointSensitivity(DATE_AFTER, USD);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_parameterSensitivity() {
    IssuerCurveDiscountFactors base = IssuerCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    IssuerCurveZeroRateSensitivity sensi = base.zeroRatePointSensitivity(DATE_AFTER, USD);
    CurrencyParameterSensitivities computed = base.parameterSensitivity(sensi);
    CurrencyParameterSensitivities expected =
        DSC_FACTORS.parameterSensitivity(DSC_FACTORS.zeroRatePointSensitivity(DATE_AFTER, USD));
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IssuerCurveDiscountFactors test1 = IssuerCurveDiscountFactors.of(DSC_FACTORS, GROUP);
    coverImmutableBean(test1);
    IssuerCurveDiscountFactors test2 =
        IssuerCurveDiscountFactors.of(ZeroRateDiscountFactors.of(USD, DATE, CURVE), LegalEntityGroup.of("ISSUER2"));
    coverBeanEquals(test1, test2);
  }

}
