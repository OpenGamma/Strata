/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.credit.CreditCurveZeroRateSensitivity;
import com.opengamma.strata.pricer.credit.CreditDiscountFactors;
import com.opengamma.strata.pricer.credit.IsdaCompliantZeroRateDiscountFactors;
import com.opengamma.strata.pricer.credit.LegalEntitySurvivalProbabilities;

/**
 * Test {@link LegalEntitySurvivalProbabilities}.
 */
@Test
public class LegalEntitySurvivalProbabilitiesTest {
  private static final LocalDate VALUATION = LocalDate.of(2016, 5, 6);
  private static final DoubleArray TIME = DoubleArray.ofUnsafe(new double[] {
      0.09041095890410959, 0.16712328767123288, 0.2547945205479452, 0.5041095890410959, 0.7534246575342466, 1.0054794520547945,
      2.0054794520547947, 3.008219178082192, 4.013698630136987, 5.010958904109589, 6.008219178082192, 7.010958904109589,
      8.01095890410959, 9.01095890410959, 10.016438356164384, 12.013698630136986, 15.021917808219179, 20.01917808219178,
      30.024657534246575});
  private static final DoubleArray RATE = DoubleArray.ofUnsafe(new double[] {
      -0.002078655697855299, -0.001686438401304855, -0.0013445486228483379, -4.237819925898475E-4, 2.5142499469348057E-5,
      5.935063895780138E-4, -3.247081037469503E-4, 6.147182786549223E-4, 0.0019060597240545122, 0.0033125742254568815,
      0.0047766352312329455, 0.0062374324537341225, 0.007639664176639106, 0.008971003650150983, 0.010167545380711455,
      0.012196853322376243, 0.01441082634734099, 0.016236611610989507, 0.01652439910865982});
  private static final CurveName CURVE_NAME = CurveName.of("yieldUsd");
  private static final CreditDiscountFactors DFS =
      IsdaCompliantZeroRateDiscountFactors.of(USD, VALUATION, CURVE_NAME, TIME, RATE, ACT_365F);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final LocalDate DATE_AFTER = LocalDate.of(2017, 2, 24);

  public void test_of() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getParameterKeys(), TIME);
    assertEquals(test.getSurvivalProbabilities(), DFS);
    assertEquals(test.getValuationDate(), VALUATION);
  }

  //-------------------------------------------------------------------------
  public void test_survivalProbability() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    assertEquals(test.survivalProbability(DATE_AFTER), DFS.discountFactor(DATE_AFTER));
  }

  public void test_zeroRate() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    double relativeYearFraction = ACT_365F.relativeYearFraction(VALUATION, DATE_AFTER);
    double discountFactor = test.survivalProbability(DATE_AFTER);
    double zeroRate = test.zeroRate(relativeYearFraction);
    assertEquals(Math.exp(-zeroRate * relativeYearFraction), discountFactor);
  }

  //-------------------------------------------------------------------------
  public void test_zeroRatePointSensitivity() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    CreditCurveZeroRateSensitivity expected =
        CreditCurveZeroRateSensitivity.of(DFS.zeroRatePointSensitivity(DATE_AFTER), LEGAL_ENTITY);
    assertEquals(test.zeroRatePointSensitivity(DATE_AFTER), expected);
  }

  public void test_zeroRatePointSensitivity_sensitivityCurrency() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    CreditCurveZeroRateSensitivity expected =
        CreditCurveZeroRateSensitivity.of(DFS.zeroRatePointSensitivity(DATE_AFTER, GBP), LEGAL_ENTITY);
    assertEquals(test.zeroRatePointSensitivity(DATE_AFTER, GBP), expected);
  }

  public void test_zeroRatePointSensitivity_yearFraction() {
    double yearFraction = DFS.relativeYearFraction(DATE_AFTER);
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    CreditCurveZeroRateSensitivity expected =
        CreditCurveZeroRateSensitivity.of(DFS.zeroRatePointSensitivity(yearFraction), LEGAL_ENTITY);
    assertEquals(test.zeroRatePointSensitivity(yearFraction), expected);
  }

  public void test_zeroRatePointSensitivity_sensitivityCurrency_yearFraction() {
    double yearFraction = DFS.relativeYearFraction(DATE_AFTER);
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    CreditCurveZeroRateSensitivity expected =
        CreditCurveZeroRateSensitivity.of(DFS.zeroRatePointSensitivity(yearFraction, GBP), LEGAL_ENTITY);
    assertEquals(test.zeroRatePointSensitivity(yearFraction, GBP), expected);
  }

  //-------------------------------------------------------------------------
  public void test_unitParameterSensitivity() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    CreditCurveZeroRateSensitivity sens = test.zeroRatePointSensitivity(DATE_AFTER);
    CurrencyParameterSensitivities expected =
        DFS.parameterSensitivity(DFS.zeroRatePointSensitivity(DATE_AFTER));
    assertEquals(test.parameterSensitivity(sens), expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end FD tests are in pricer test
  public void test_parameterSensitivity() {
    LegalEntitySurvivalProbabilities test = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    CreditCurveZeroRateSensitivity point =
        CreditCurveZeroRateSensitivity.of(ZeroRateSensitivity.of(USD, 1d, 1d), LEGAL_ENTITY);
    assertEquals(test.parameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    LegalEntitySurvivalProbabilities test1 = LegalEntitySurvivalProbabilities.of(LEGAL_ENTITY, DFS);
    coverImmutableBean(test1);
    LegalEntitySurvivalProbabilities test2 = LegalEntitySurvivalProbabilities.of(
        StandardId.of("OG", "CCC"),
        IsdaCompliantZeroRateDiscountFactors.of(
            GBP, VALUATION, CURVE_NAME, DoubleArray.of(5.0), DoubleArray.of(0.014), ACT_365F));
    coverBeanEquals(test1, test2);
  }

}
