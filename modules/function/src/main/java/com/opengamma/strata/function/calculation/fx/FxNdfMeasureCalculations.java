/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toCurrencyValuesArray;
import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyValuesArray;
import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxNdfProductPricer;
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

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.presentValue(product, provider))
        .collect(toCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> calculatePv01(product, provider))
        .collect(toMultiCurrencyValuesArray());
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(
      ExpandedFxNdf product,
      RatesProvider provider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> calculateBucketedPv01(product, provider))
        .collect(toScenarioResult());
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ExpandedFxNdf product,
      RatesProvider provider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  static MultiCurrencyValuesArray currencyExposure(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.currencyExposure(product, provider))
        .collect(toMultiCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  static CurrencyValuesArray currentCash(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.currentCash(product, provider))
        .collect(toCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // calculates forward FX rate for all scenarios
  static ScenarioResult<FxRate> forwardFxRate(
      FxNdfTrade trade,
      ExpandedFxNdf product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.forwardFxRate(product, provider))
        .collect(toScenarioResult());
  }

  //-------------------------------------------------------------------------
  // common code, creating a stream of RatesProvider from CalculationMarketData
  private static Stream<RatesProvider> ratesProviderStream(CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new SingleCalculationMarketData(marketData, index))
        .map(market -> new MarketDataRatesProvider(market));
  }

}
