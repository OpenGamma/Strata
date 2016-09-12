/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.fx.DiscountingFxNdfTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxNdfTrade;

/**
 * Multi-scenario measure calculations for FX NDF trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxNdfMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final FxNdfMeasureCalculations DEFAULT = new FxNdfMeasureCalculations(
      DiscountingFxNdfTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFxNdfTrade}.
   */
  private final DiscountingFxNdfTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedFxNdfTrade}
   */
  FxNdfMeasureCalculations(
      DiscountingFxNdfTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currencyExposure(trade, marketData.scenario(i).ratesProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.currencyExposure(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currentCash(trade, marketData.scenario(i).ratesProvider()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.currentCash(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates forward FX rate for all scenarios
  ScenarioArray<FxRate> forwardFxRate(
      ResolvedFxNdfTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> forwardFxRate(trade, marketData.scenario(i).ratesProvider()));
  }

  // current cash for one scenario
  FxRate forwardFxRate(
      ResolvedFxNdfTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.forwardFxRate(trade, ratesProvider);
  }

}
