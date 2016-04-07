/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Multi-scenario measure calculations for Ibor Future trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class IborFutureMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingIborFutureTradePricer PRICER = DiscountingIborFutureTradePricer.DEFAULT;

  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private IborFutureMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  static ValuesArray parSpread(
      ResolvedIborFutureTrade trade,
      CalculationMarketData marketData) {

    return ValuesArray.of(
        marketData.getScenarioCount(),
        index -> calculateParSpread(trade, marketData.scenario(index)));
  }

  // par spread for one scenario
  private static double calculateParSpread(
      ResolvedIborFutureTrade trade,
      MarketData marketData) {

    QuoteKey key = QuoteKey.of(trade.getProduct().getSecurityId().getStandardId());
    double price = marketData.getValue(key) / 100;  // convert market quote to value needed
    return PRICER.parSpread(trade, MarketDataRatesProvider.of(marketData), price);
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      ResolvedIborFutureTrade trade,
      CalculationMarketData marketData) {

    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(trade, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(
      ResolvedIborFutureTrade trade,
      MarketData marketData) {

    QuoteKey key = QuoteKey.of(trade.getProduct().getSecurityId().getStandardId());
    double price = marketData.getValue(key) / 100;  // convert market quote to value needed
    return PRICER.presentValue(trade, MarketDataRatesProvider.of(marketData), price);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      ResolvedIborFutureTrade trade,
      CalculationMarketData marketData) {

    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(trade, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(
      ResolvedIborFutureTrade trade,
      MarketData marketData) {

    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, provider);
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      ResolvedIborFutureTrade trade,
      CalculationMarketData marketData) {

    return ScenarioResult.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(trade, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      ResolvedIborFutureTrade trade,
      MarketData marketData) {

    RatesProvider provider = MarketDataRatesProvider.of(marketData);
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(trade, provider);
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

}
