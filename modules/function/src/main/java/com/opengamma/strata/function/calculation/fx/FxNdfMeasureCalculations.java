/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxNdfProductPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;

/**
 * Multi-scenario measure calculations for FX NDF trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxNdfMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingFxNdfProductPricer PRICER = DiscountingFxNdfProductPricer.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private FxNdfMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(product, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(ExpandedFxNdf product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.presentValue(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(product, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(ExpandedFxNdf product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(product, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ExpandedFxNdf product,
      MarketData marketData) {

    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  static MultiCurrencyValuesArray currencyExposure(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCurrencyExposure(product, marketData.scenario(i)));
  }

  // currency exposure for one scenario
  private static MultiCurrencyAmount calculateCurrencyExposure(ExpandedFxNdf product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.currencyExposure(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  static CurrencyValuesArray currentCash(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculateCurrentCash(product, marketData.scenario(i)));
  }

  // current cash for one scenario
  private static CurrencyAmount calculateCurrentCash(ExpandedFxNdf product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.currentCash(product, provider);
  }

  //-------------------------------------------------------------------------
  // calculates forward FX rate for all scenarios
  static ScenarioResult<FxRate> forwardFxRate(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateForwardFxRate(product, marketData.scenario(i)));
  }

  // current cash for one scenario
  private static FxRate calculateForwardFxRate(ExpandedFxNdf product, MarketData marketData) {
    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    return PRICER.forwardFxRate(product, provider);
  }

}
