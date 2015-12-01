/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.market.value.CompoundedRateType.CONTINUOUS;
import static com.opengamma.strata.market.value.CompoundedRateType.PERIODIC;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.datasets.LegalEntityDiscountingProviderDataSets;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureTrade;

/**
 * Test {@link DiscountingBondFutureTradePricer}.
 */
@Test
public class DiscountingBondFutureTradetPricerTest {

  // product and trade
  private static final BondFuture FUTURE_PRODUCT = BondDataSets.FUTURE_PRODUCT_USD;
  private static final BondFutureTrade FUTURE_TRADE = BondDataSets.FUTURE_TRADE_USD;
  private static final double REFERENCE_PRICE = BondDataSets.REFERENCE_PRICE_USD;
  private static final double NOTIONAL = BondDataSets.NOTIONAL_USD;
  private static final long QUANTITY = BondDataSets.QUANTITY_USD;
  // curves
  private static final LegalEntityDiscountingProvider PROVIDER = LegalEntityDiscountingProviderDataSets.ISSUER_REPO_ZERO;
  private static final CurveMetadata METADATA_ISSUER = LegalEntityDiscountingProviderDataSets.META_ZERO_ISSUER_USD;
  private static final CurveMetadata METADATA_REPO = LegalEntityDiscountingProviderDataSets.META_ZERO_REPO_USD;
  // parameters
  private static final double Z_SPREAD = 0.0075;
  private static final int PERIOD_PER_YEAR = 4;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-6;
  // pricers
  private static final DiscountingBondFutureTradePricer TRADE_PRICER = DiscountingBondFutureTradePricer.DEFAULT;
  private static final DiscountingBondFutureProductPricer PRODUCT_PRICER = DiscountingBondFutureProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL = new RatesFiniteDifferenceSensitivityCalculator(EPS);

  public void test_price() {
    double computed = TRADE_PRICER.price(FUTURE_TRADE, PROVIDER);
    double expected = PRODUCT_PRICER.price(FUTURE_PRODUCT, PROVIDER);
    assertEquals(computed, expected, TOL);
  }

  public void test_priceWithZSpread_continuous() {
    double computed = TRADE_PRICER.priceWithZSpread(FUTURE_TRADE, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    double expected = PRODUCT_PRICER.priceWithZSpread(FUTURE_PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    assertEquals(computed, expected, TOL);
  }

  public void test_priceWithZSpread_periodic() {
    double computed = TRADE_PRICER.priceWithZSpread(FUTURE_TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double expected = PRODUCT_PRICER.priceWithZSpread(FUTURE_PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(computed, expected, TOL);
  }

  public void test_presentValue() {
    CurrencyAmount computed = TRADE_PRICER.presentValue(FUTURE_TRADE, PROVIDER, REFERENCE_PRICE);
    double expected = (PRODUCT_PRICER.price(FUTURE_PRODUCT, PROVIDER) - REFERENCE_PRICE) * NOTIONAL * QUANTITY;
    assertEquals(computed.getCurrency(), USD);
    assertEquals(computed.getAmount(), expected, TOL * NOTIONAL * QUANTITY);
  }

  public void test_presentValueWithZSpread_continuous() {
    CurrencyAmount computed = TRADE_PRICER.presentValueWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, CONTINUOUS, 0);
    double expected = (PRODUCT_PRICER.priceWithZSpread(FUTURE_PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS, 0) - REFERENCE_PRICE)
        * NOTIONAL * QUANTITY;
    assertEquals(computed.getCurrency(), USD);
    assertEquals(computed.getAmount(), expected, TOL * NOTIONAL * QUANTITY);
  }

  public void test_presentValueWithZSpread_periodic() {
    CurrencyAmount computed = TRADE_PRICER.presentValueWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double expected = NOTIONAL * QUANTITY *
        (PRODUCT_PRICER.priceWithZSpread(FUTURE_PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR) - REFERENCE_PRICE);
    assertEquals(computed.getCurrency(), USD);
    assertEquals(computed.getAmount(), expected, TOL * NOTIONAL * QUANTITY);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = TRADE_PRICER.presentValueSensitivity(FUTURE_TRADE, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected =
        FD_CAL.sensitivity(PROVIDER, (p) -> TRADE_PRICER.presentValue(FUTURE_TRADE, (p), REFERENCE_PRICE));
    assertTrue(computed.equalWithTolerance(expected, 10.0 * EPS * NOTIONAL * QUANTITY));
  }

  public void test_presentValueSensitivityWithZSpread_continuous() {
    PointSensitivities point = TRADE_PRICER.presentValueSensitivityWithZSpread(
        FUTURE_TRADE, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER,
        (p) -> TRADE_PRICER.presentValueWithZSpread(FUTURE_TRADE, (p), REFERENCE_PRICE, Z_SPREAD, CONTINUOUS, 0));
    assertTrue(computed.equalWithTolerance(expected, 10.0 * EPS * NOTIONAL * QUANTITY));
  }

  public void test_presentValueSensitivityWithZSpread_periodic() {
    PointSensitivities point = TRADE_PRICER.presentValueSensitivityWithZSpread(
        FUTURE_TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) ->
        TRADE_PRICER.presentValueWithZSpread(FUTURE_TRADE, (p), REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR));
    assertTrue(computed.equalWithTolerance(expected, 10.0 * EPS * NOTIONAL * QUANTITY));
  }

  //-------------------------------------------------------------------------
  public void test_parSpread() {
    double computed = TRADE_PRICER.parSpread(FUTURE_TRADE, PROVIDER, REFERENCE_PRICE);
    double expected = PRODUCT_PRICER.price(FUTURE_PRODUCT, PROVIDER) - REFERENCE_PRICE;
    assertEquals(computed, expected, TOL);
  }

  public void test_parSpreadWithZSpread_continuous() {
    double computed = TRADE_PRICER.parSpreadWithZSpread(FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, CONTINUOUS, 0);
    double expected = PRODUCT_PRICER.priceWithZSpread(FUTURE_PRODUCT, PROVIDER, Z_SPREAD, CONTINUOUS, 0) - REFERENCE_PRICE;
    assertEquals(computed, expected, TOL);
  }

  public void test_parSpreadWithZSpread_periodic() {
    double computed = TRADE_PRICER.parSpreadWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    double expected =
        PRODUCT_PRICER.priceWithZSpread(FUTURE_PRODUCT, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR) - REFERENCE_PRICE;
    assertEquals(computed, expected, TOL);
  }

  public void test_parSpreadSensitivity() {
    PointSensitivities point = TRADE_PRICER.parSpreadSensitivity(FUTURE_TRADE, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        PROVIDER, (p) -> CurrencyAmount.of(USD, TRADE_PRICER.parSpread(FUTURE_TRADE, (p), REFERENCE_PRICE)));
    assertTrue(computed.equalWithTolerance(expected, 10.0 * EPS * NOTIONAL * QUANTITY));
  }

  public void test_parSpreadSensitivityWithZSpread_continuous() {
    PointSensitivities point = TRADE_PRICER.parSpreadSensitivityWithZSpread(
        FUTURE_TRADE, PROVIDER, Z_SPREAD, CONTINUOUS, 0);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) -> CurrencyAmount.of(
        USD, TRADE_PRICER.parSpreadWithZSpread(FUTURE_TRADE, (p), REFERENCE_PRICE, Z_SPREAD, CONTINUOUS, 0)));
    assertTrue(computed.equalWithTolerance(expected, 10.0 * EPS * NOTIONAL * QUANTITY));
  }

  public void test_parSpreadSensitivityWithZSpread_periodic() {
    PointSensitivities point = TRADE_PRICER.parSpreadSensitivityWithZSpread(
        FUTURE_TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(PROVIDER, (p) -> CurrencyAmount.of(USD,
        TRADE_PRICER.parSpreadWithZSpread(FUTURE_TRADE, (p), REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR)));
    assertTrue(computed.equalWithTolerance(expected, 10.0 * EPS * NOTIONAL * QUANTITY));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount ceComputed = TRADE_PRICER.currencyExposure(FUTURE_TRADE, PROVIDER, REFERENCE_PRICE);
    CurrencyAmount pv = TRADE_PRICER.presentValue(FUTURE_TRADE, PROVIDER, REFERENCE_PRICE);
    assertEquals(ceComputed, MultiCurrencyAmount.of(pv));
  }

  public void test_currencyExposureWithZSpread() {
    MultiCurrencyAmount ceComputed = TRADE_PRICER.currencyExposureWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurrencyAmount pv = TRADE_PRICER.presentValueWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(ceComputed, MultiCurrencyAmount.of(pv));
  }

  //-------------------------------------------------------------------------
  // regression to 2.x
  public void regression() {
    CurrencyAmount pv = TRADE_PRICER.presentValue(FUTURE_TRADE, PROVIDER, REFERENCE_PRICE);
    assertEquals(pv.getAmount(), -2937800.66334416, EPS * NOTIONAL * QUANTITY);
    PointSensitivities pvPoint = TRADE_PRICER.presentValueSensitivity(FUTURE_TRADE, PROVIDER);
    CurveCurrencyParameterSensitivities pvSensi = PROVIDER.curveParameterSensitivity(pvPoint);
    DoubleArray sensiIssuer = DoubleArray.of(
        -48626.82968419264, -513532.4556150143, -1768520.182827613, -1.262340715772077E8, -5.208162480624767E8, 0.0);
    DoubleArray sensiRepo = DoubleArray.of(1.8204636592806276E7, 2.5799948548745323E7, 0.0, 0.0, 0.0, 0.0);
    CurveCurrencyParameterSensitivities pvSensiOld = CurveCurrencyParameterSensitivities
        .of(CurveCurrencyParameterSensitivity.of(METADATA_ISSUER, USD, sensiIssuer));
    pvSensiOld = pvSensiOld.combinedWith(CurveCurrencyParameterSensitivity.of(METADATA_REPO, USD, sensiRepo));
    assertTrue(pvSensi.equalWithTolerance(pvSensiOld, TOL * NOTIONAL * QUANTITY));
  }

  public void regression_withZSpread_continuous() {
    CurrencyAmount pv = TRADE_PRICER.presentValueWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, CONTINUOUS, 0);
    assertEquals(pv.getAmount(), -7728642.649169521, EPS * NOTIONAL * QUANTITY);
  }

  public void regression_withZSpread_periodic() {
    CurrencyAmount pv = TRADE_PRICER.presentValueWithZSpread(
        FUTURE_TRADE, PROVIDER, REFERENCE_PRICE, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    assertEquals(pv.getAmount(), -7710148.864076961, EPS * NOTIONAL * QUANTITY);
    PointSensitivities pvPoint = TRADE_PRICER.presentValueSensitivityWithZSpread(
        FUTURE_TRADE, PROVIDER, Z_SPREAD, PERIODIC, PERIOD_PER_YEAR);
    CurveCurrencyParameterSensitivities pvSensi = PROVIDER.curveParameterSensitivity(pvPoint);
    DoubleArray sensiIssuer = DoubleArray.of(-48374.31671055041, -510470.43789512076, -1748988.1122760356,
        -1.2199872917663E8, -5.0289585725762194E8, 0.0);
    DoubleArray sensiRepo = DoubleArray.of(1.7625865116887797E7, 2.497970288088735E7, 0.0, 0.0, 0.0, 0.0);
    CurveCurrencyParameterSensitivities pvSensiOld = CurveCurrencyParameterSensitivities
        .of(CurveCurrencyParameterSensitivity.of(METADATA_ISSUER, USD, sensiIssuer));
    pvSensiOld = pvSensiOld.combinedWith(CurveCurrencyParameterSensitivity.of(METADATA_REPO, USD, sensiRepo));
    assertTrue(pvSensi.equalWithTolerance(pvSensiOld, TOL * NOTIONAL * QUANTITY));
  }

}
