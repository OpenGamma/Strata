/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swap;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.amount.LegAmounts;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Calculates pricing and risk measures for swap trades.
 * <p>
 * This provides a high-level entry point for swap pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedSwapTrade}, whereas application code will
 * typically work with {@link SwapTrade}. Call
 * {@link SwapTrade#resolve(com.opengamma.strata.basics.ReferenceData) SwapTrade::resolve(ReferenceData)}
 * to convert {@code SwapTrade} to {@code ResolvedSwapTrade}.
 */
public class SwapTradeCalculations {

  /**
   * Default implementation.
   */
  public static final SwapTradeCalculations DEFAULT = new SwapTradeCalculations(
      DiscountingSwapTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedSwapTrade}.
   */
  private final SwapMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedSwapTrade}
   */
  public SwapTradeCalculations(
      DiscountingSwapTradePricer tradePricer) {
    this.calc = new SwapMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public MultiCurrencyScenarioArray presentValue(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Explains the present value calculation across one or more scenarios.
   * <p>
   * This provides a breakdown of how
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * was calculated, typically used for debugging and validation.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value explanation, one entry per scenario
   */
  public ScenarioArray<ExplainMap> explainPresentValue(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.explainPresentValue(trade, lookup.marketDataView(marketData));
  }

  /**
   * Explains the present value calculation for a single set of market data.
   * <p>
   * This provides a breakdown of how
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * was calculated, typically used for debugging and validation.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value explanation
   */
  public ExplainMap explainPresentValue(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.explainPresentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01CalibratedSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01CalibratedBucketed(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01MarketQuoteSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedSwapTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01MarketQuoteBucketed(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates par rate across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the par rate, one entry per scenario
   */
  public DoubleScenarioArray parRate(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.parRate(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates par rate for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the par rate
   */
  public double parRate(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.parRate(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates par spread across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the par spread, one entry per scenario
   */
  public DoubleScenarioArray parSpread(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.parSpread(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates par spread for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the par spread
   */
  public double parSpread(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.parSpread(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates cash flows across one or more scenarios.
   * <p>
   * The cash flows provide details about the payments of the trade.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the cash flows, one entry per scenario
   */
  public ScenarioArray<CashFlows> cashFlows(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.cashFlows(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates cash flows for a single set of market data.
   * <p>
   * The cash flows provide details about the payments of the trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the cash flows
   */
  public CashFlows cashFlows(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.cashFlows(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates accrued interest across one or more scenarios.
   * <p>
   * The accrued interest since the last payment.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the accrued interest, one entry per scenario
   */
  public MultiCurrencyScenarioArray accruedInterest(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.accruedInterest(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates accrued interest for a single set of market data.
   * <p>
   * The accrued interest since the last payment.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the accrued interest
   */
  public MultiCurrencyAmount accruedInterest(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.accruedInterest(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the initial notional of each leg.
   * <p>
   * This does not require market data.
   * 
   * @param trade  the trade
   * @return the initial notional of each leg
   */
  public LegAmounts legInitialNotional(ResolvedSwapTrade trade) {
    return calc.legInitialNotional(trade);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of each leg across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value of each leg, one entry per scenario
   */
  public ScenarioArray<LegAmounts> legPresentValue(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.legPresentValue(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates the present value of each leg for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value of each leg
   */
  public LegAmounts legPresentValue(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.legPresentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.currencyExposure(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates current cash across one or more scenarios.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the current cash, one entry per scenario
   */
  public MultiCurrencyScenarioArray currentCash(
      ResolvedSwapTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.currentCash(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates current cash for a single set of market data.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedSwapTrade trade,
      RatesProvider ratesProvider) {

    return calc.currentCash(trade, ratesProvider);
  }

}
