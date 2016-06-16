/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ValuesArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Calculates pricing and risk measures for trades in a futures contract based on an Ibor index.
 * <p>
 * This provides a high-level entry point for future pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedIborFutureTrade}, whereas application code will
 * typically work with {@link IborFutureTrade}. Call
 * {@link IborFutureTrade#resolve(com.opengamma.strata.basics.ReferenceData) IborFutureTrade::resolve(ReferenceData)}
 * to convert {@code IborFutureTrade} to {@code ResolvedIborFutureTrade}.
 */
public class IborFutureTradeCalculations {

  /**
   * Default implementation.
   */
  public static final IborFutureTradeCalculations DEFAULT = new IborFutureTradeCalculations(
      DiscountingIborFutureTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedIborFutureTrade}.
   */
  private final IborFutureMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedIborFutureTrade}
   */
  public IborFutureTradeCalculations(
      DiscountingIborFutureTradePricer tradePricer) {
    this.calc = new IborFutureMeasureCalculations(tradePricer);
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
  public CurrencyValuesArray presentValue(
      ResolvedIborFutureTrade trade,
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
  public CurrencyAmount presentValue(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider) {

    return calc.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyValuesArray pv01CalibratedSum(
      ResolvedIborFutureTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01CalibratedSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedIborFutureTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01CalibratedBucketed(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyValuesArray pv01MarketQuoteSum(
      ResolvedIborFutureTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01MarketQuoteSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedIborFutureTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01MarketQuoteBucketed(trade, ratesProvider);
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
  public ValuesArray parSpread(
      ResolvedIborFutureTrade trade,
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
      ResolvedIborFutureTrade trade,
      RatesProvider ratesProvider) {

    return calc.parSpread(trade, ratesProvider);
  }

}
