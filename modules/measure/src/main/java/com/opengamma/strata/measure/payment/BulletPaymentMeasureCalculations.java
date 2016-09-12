/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.payment;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.payment.DiscountingBulletPaymentTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Multi-scenario measure calculations for Bullet Payment trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class BulletPaymentMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final BulletPaymentMeasureCalculations DEFAULT = new BulletPaymentMeasureCalculations(
      DiscountingBulletPaymentTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedBulletPaymentTrade}.
   */
  private final DiscountingBulletPaymentTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBulletPaymentTrade}
   */
  BulletPaymentMeasureCalculations(
      DiscountingBulletPaymentTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> presentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates explain present value for all scenarios
  ScenarioArray<ExplainMap> explainPresentValue(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> explainPresentValue(trade, marketData.scenario(i).ratesProvider()));
  }

  // explain present value for one scenario
  ExplainMap explainPresentValue(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.explainPresentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    return ratesProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteSum(trade, marketData.scenario(i).ratesProvider()));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> pv01MarketQuoteBucketed(trade, marketData.scenario(i).ratesProvider()));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, ratesProvider);
    CurrencyParameterSensitivities parameterSensitivity = ratesProvider.parameterSensitivity(pointSensitivity);
    return MARKET_QUOTE_SENS.sensitivity(parameterSensitivity, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates cash flows for all scenarios
  ScenarioArray<CashFlows> cashFlows(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> cashFlows(trade, marketData.scenario(i).ratesProvider()));
  }

  // cash flows for one scenario
  CashFlows cashFlows(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.cashFlows(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return MultiCurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currencyExposure(trade, marketData.scenario(i).ratesProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return MultiCurrencyAmount.of(tradePricer.currencyExposure(trade, ratesProvider));
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    return CurrencyScenarioArray.of(
        marketData.getScenarioCount(),
        i -> currentCash(trade, marketData.scenario(i).ratesProvider()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return tradePricer.currentCash(trade, ratesProvider);
  }

}
