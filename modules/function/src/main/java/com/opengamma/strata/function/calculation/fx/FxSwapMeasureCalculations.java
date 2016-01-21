/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyValuesArray;
import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * Multi-scenario measure calculations for FX swap trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxSwapMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingFxSwapProductPricer PRICER = DiscountingFxSwapProductPricer.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private FxSwapMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      FxSwapTrade trade,
      ExpandedFxSwap product,
      CalculationMarketData marketData) {

    DoubleArray array = DoubleArray.of(
        marketData.getScenarioCount(),
        index -> PRICER.parSpread(product, ratesProvider(marketData, index)));
    return ValuesArray.of(array);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static MultiCurrencyValuesArray presentValue(
      FxSwapTrade trade,
      ExpandedFxSwap product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.presentValue(product, provider))
        .collect(toMultiCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      FxSwapTrade trade,
      ExpandedFxSwap product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> calculatePv01(product, provider))
        .collect(toMultiCurrencyValuesArray());
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(
      ExpandedFxSwap product,
      RatesProvider provider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      FxSwapTrade trade,
      ExpandedFxSwap product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> calculateBucketedPv01(product, provider))
        .collect(toScenarioResult());
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ExpandedFxSwap product,
      RatesProvider provider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(product, provider);
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  static MultiCurrencyValuesArray currencyExposure(
      FxSwapTrade trade,
      ExpandedFxSwap product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.currencyExposure(product, provider))
        .collect(toMultiCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  static MultiCurrencyValuesArray currentCash(
      FxSwapTrade trade,
      ExpandedFxSwap product,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.currentCash(product, provider.getValuationDate()))
        .collect(toMultiCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // common code, creating a stream of RatesProvider from CalculationMarketData
  private static Stream<RatesProvider> ratesProviderStream(CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> ratesProvider(marketData, index));
  }

  // creates a RatesProvider
  private static RatesProvider ratesProvider(CalculationMarketData marketData, int index) {
    return new MarketDataRatesProvider(new SingleCalculationMarketData(marketData, index));
  }

}
