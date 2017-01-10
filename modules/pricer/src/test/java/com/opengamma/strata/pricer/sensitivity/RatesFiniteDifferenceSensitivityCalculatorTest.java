/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Map.Entry;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.RepoGroup;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.credit.CreditDiscountFactors;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.credit.ImmutableCreditRatesProvider;
import com.opengamma.strata.pricer.credit.IsdaCreditDiscountFactors;
import com.opengamma.strata.pricer.credit.LegalEntitySurvivalProbabilities;
import com.opengamma.strata.pricer.datasets.CreditRatesProviderDataSets;
import com.opengamma.strata.pricer.datasets.LegalEntityDiscountingProviderDataSets;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Tests {@link RatesFiniteDifferenceSensitivityCalculator}.
 */
public class RatesFiniteDifferenceSensitivityCalculatorTest {

  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      RatesFiniteDifferenceSensitivityCalculator.DEFAULT;

  private static final double TOLERANCE_DELTA = 1.0E-8;

  @Test
  public void sensitivity_single_curve() {
    CurrencyParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(RatesProviderDataSets.SINGLE_USD, this::fn);
    DoubleArray times = RatesProviderDataSets.TIMES_1;
    assertEquals(sensiComputed.size(), 1);
    DoubleArray s = sensiComputed.getSensitivities().get(0).getSensitivity();
    assertEquals(s.size(), times.size());
    for (int i = 0; i < times.size(); i++) {
      assertEquals(s.get(i), times.get(i) * 4.0d, TOLERANCE_DELTA);
    }
  }

  @Test
  public void sensitivity_multi_curve() {
    CurrencyParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(RatesProviderDataSets.MULTI_CPI_USD, this::fn);
    DoubleArray times1 = RatesProviderDataSets.TIMES_1;
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    DoubleArray times4 = RatesProviderDataSets.TIMES_4;
    assertEquals(sensiComputed.size(), 4);
    DoubleArray s1 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    assertEquals(s1.size(), times1.size());
    for (int i = 0; i < times1.size(); i++) {
      assertEquals(times1.get(i) * 2.0d, s1.get(i), TOLERANCE_DELTA);
    }
    DoubleArray s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertEquals(s2.size(), times2.size());
    for (int i = 0; i < times2.size(); i++) {
      assertEquals(times2.get(i), s2.get(i), TOLERANCE_DELTA);
    }
    DoubleArray s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertEquals(s3.size(), times3.size());
    for (int i = 0; i < times3.size(); i++) {
      assertEquals(times3.get(i), s3.get(i), TOLERANCE_DELTA);
    }
    DoubleArray s4 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    assertEquals(s4.size(), times4.size());
    for (int i = 0; i < times4.size(); i++) {
      assertEquals(times4.get(i), s4.get(i), TOLERANCE_DELTA);
    }
  }

  // private function for testing. Returns the sum of rates multiplied by time
  private CurrencyAmount fn(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumProduct(curveInt);
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumProduct(curveInt);
    }
    return CurrencyAmount.of(USD, result);
  }

  // compute the sum of the product of times and rates
  private double sumProduct(NodalCurve curveInt) {
    double result = 0.0;
    DoubleArray x = curveInt.getXValues();
    DoubleArray y = curveInt.getYValues();
    int nbNodePoint = x.size();
    for (int i = 0; i < nbNodePoint; i++) {
      result += x.get(i) * y.get(i);
    }
    return result;
  }

  // check that the curve is InterpolatedNodalCurve
  private InterpolatedNodalCurve checkInterpolated(Curve curve) {
    ArgChecker.isTrue(curve instanceof InterpolatedNodalCurve, "Curve should be a InterpolatedNodalCurve");
    return (InterpolatedNodalCurve) curve;
  }

  //-------------------------------------------------------------------------
  @Test
  public void sensitivity_legalEntity_Zero() {
    CurrencyParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(
        LegalEntityDiscountingProviderDataSets.ISSUER_REPO_ZERO, this::fn);
    DoubleArray timeIssuer = LegalEntityDiscountingProviderDataSets.ISSUER_TIME_USD;
    DoubleArray timesRepo = LegalEntityDiscountingProviderDataSets.REPO_TIME_USD;
    assertEquals(sensiComputed.size(), 2);
    DoubleArray sensiIssuer = sensiComputed.getSensitivity(
        LegalEntityDiscountingProviderDataSets.META_ZERO_ISSUER_USD.getCurveName(), USD).getSensitivity();
    assertEquals(sensiIssuer.size(), timeIssuer.size());
    for (int i = 0; i < timeIssuer.size(); i++) {
      assertEquals(timeIssuer.get(i), sensiIssuer.get(i), TOLERANCE_DELTA);
    }
    DoubleArray sensiRepo = sensiComputed.getSensitivity(
        LegalEntityDiscountingProviderDataSets.META_ZERO_REPO_USD.getCurveName(), USD).getSensitivity();
    assertEquals(sensiRepo.size(), timesRepo.size());
    for (int i = 0; i < timesRepo.size(); i++) {
      assertEquals(timesRepo.get(i), sensiRepo.get(i), TOLERANCE_DELTA);
    }
  }

  @Test
  public void sensitivity_legalEntity_Simple() {
    CurrencyParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(
        LegalEntityDiscountingProviderDataSets.ISSUER_REPO_SIMPLE, this::fn);
    DoubleArray timeIssuer = LegalEntityDiscountingProviderDataSets.ISSUER_TIME_USD;
    DoubleArray timesRepo = LegalEntityDiscountingProviderDataSets.REPO_TIME_USD;
    assertEquals(sensiComputed.size(), 2);
    DoubleArray sensiIssuer = sensiComputed.getSensitivity(
        LegalEntityDiscountingProviderDataSets.META_SIMPLE_ISSUER_USD.getCurveName(), USD).getSensitivity();
    assertEquals(sensiIssuer.size(), timeIssuer.size());
    for (int i = 0; i < timeIssuer.size(); i++) {
      assertEquals(timeIssuer.get(i), sensiIssuer.get(i), TOLERANCE_DELTA);
    }
    DoubleArray sensiRepo = sensiComputed.getSensitivity(
        LegalEntityDiscountingProviderDataSets.META_SIMPLE_REPO_USD.getCurveName(), USD).getSensitivity();
    assertEquals(sensiRepo.size(), timesRepo.size());
    for (int i = 0; i < timesRepo.size(); i++) {
      assertEquals(timesRepo.get(i), sensiRepo.get(i), TOLERANCE_DELTA);
    }
  }

  // private function for testing. Returns the sum of rates multiplied by time
  private CurrencyAmount fn(ImmutableLegalEntityDiscountingProvider provider) {
    double result = 0.0;
    // issuer curve
    ImmutableMap<Pair<LegalEntityGroup, Currency>, DiscountFactors> mapLegal = provider.metaBean().issuerCurves()
        .get(provider);
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : mapLegal.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(checkDiscountFactors(entry.getValue()));
      result += sumProduct(curveInt);
    }
    // repo curve
    ImmutableMap<Pair<RepoGroup, Currency>, DiscountFactors> mapRepo = provider.metaBean().repoCurves().get(provider);
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : mapRepo.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(checkDiscountFactors(entry.getValue()));
      result += sumProduct(curveInt);
    }
    return CurrencyAmount.of(USD, result);
  }

  private Curve checkDiscountFactors(DiscountFactors discountFactors) {
    if (discountFactors instanceof ZeroRateDiscountFactors) {
      return ((ZeroRateDiscountFactors) discountFactors).getCurve();
    } else if (discountFactors instanceof SimpleDiscountFactors) {
      return ((SimpleDiscountFactors) discountFactors).getCurve();
    }
    throw new IllegalArgumentException("Not supported");
  }

  //-------------------------------------------------------------------------
  @Test
  public void sensitivity_credit_isda() {
    LocalDate valuationDate = LocalDate.of(2014, 1, 3);
    CreditRatesProvider rates = CreditRatesProviderDataSets.createCreditRatesProvider(valuationDate);
    CurrencyParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(
        rates, this::creditFunction);
    List<IsdaCreditDiscountFactors> curves = CreditRatesProviderDataSets.getAllDiscountFactors(valuationDate);
    assertEquals(sensiComputed.size(), curves.size());
    for (IsdaCreditDiscountFactors curve : curves) {
      DoubleArray time = curve.getParameterKeys();
      DoubleArray sensiValueComputed = sensiComputed.getSensitivity(curve.getCurve().getName(), USD).getSensitivity();
      assertEquals(sensiValueComputed.size(), time.size());
      for (int i = 0; i < time.size(); i++) {
        assertEquals(time.get(i), sensiValueComputed.get(i), TOLERANCE_DELTA);
      }
    }
  }

  // private function for testing. Returns the sum of rates multiplied by time
  private CurrencyAmount creditFunction(ImmutableCreditRatesProvider provider) {
    double result = 0.0;
    // credit curve
    ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> mapCredit =
        provider.metaBean().creditCurves().get(provider);
    for (Entry<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> entry : mapCredit.entrySet()) {
      InterpolatedNodalCurve curveInt =
          checkInterpolated(checkDiscountFactors(entry.getValue().getSurvivalProbabilities().toDiscountFactors()));
      result += sumProduct(curveInt);
    }
    // repo curve
    ImmutableMap<Currency, CreditDiscountFactors> mapDiscount = provider.metaBean().discountCurves().get(provider);
    for (Entry<Currency, CreditDiscountFactors> entry : mapDiscount.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(checkDiscountFactors(entry.getValue().toDiscountFactors()));
      result += sumProduct(curveInt);
    }
    return CurrencyAmount.of(USD, result);
  }

}
