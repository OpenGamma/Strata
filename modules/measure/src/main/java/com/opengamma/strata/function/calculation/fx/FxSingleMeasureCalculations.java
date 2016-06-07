/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.function.calculation.RatesMarketData;
import com.opengamma.strata.function.calculation.RatesScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleTrade;

/**
 * Multi-scenario measure calculations for FX single leg trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxSingleMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingFxSingleProductPricer PRICER = DiscountingFxSingleProductPricer.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private FxSingleMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return ValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateParSpread(product, marketData.scenario(i)));
  }

  // par spread for one scenario
  private static double calculateParSpread(ResolvedFxSingle product, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    return PRICER.parSpread(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static MultiCurrencyValuesArray presentValue(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static MultiCurrencyAmount calculatePresentValue(ResolvedFxSingle product, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    return PRICER.presentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(product, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(ResolvedFxSingle product, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> bucketedPv01(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(product, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedFxSingle product,
      RatesMarketData marketData) {

    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  static MultiCurrencyValuesArray currencyExposure(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCurrencyExposure(product, marketData.scenario(i)));
  }

  // currency exposure for one scenario
  private static MultiCurrencyAmount calculateCurrencyExposure(ResolvedFxSingle product, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    return PRICER.currencyExposure(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  static MultiCurrencyValuesArray currentCash(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCurrentCash(product, marketData.scenario(i)));
  }

  // current cash for one scenario
  private static MultiCurrencyAmount calculateCurrentCash(ResolvedFxSingle product, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    return PRICER.currentCash(product, provider.getValuationDate());
  }

  //-------------------------------------------------------------------------
  // calculates forward FX rate for all scenarios
  static ScenarioArray<FxRate> forwardFxRate(
      ResolvedFxSingleTrade trade,
      RatesScenarioMarketData marketData) {

    ResolvedFxSingle product = trade.getProduct();
    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateForwardFxRate(product, marketData.scenario(i)));
  }

  // current cash for one scenario
  private static FxRate calculateForwardFxRate(ResolvedFxSingle product, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    return PRICER.forwardFxRate(product, provider);
  }

}
