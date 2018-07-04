/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.bond.DiscountingBillTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.BillTrade;
import com.opengamma.strata.product.bond.ResolvedBillTrade;

/**
 * Calculates pricing and risk measures for bill trades.
 * <p>
 * This provides a high-level entry point for bill pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedBillTrade}, whereas application code will
 * typically work with {@link BillTrade}. Call
 * {@link BillTrade#resolve(com.opengamma.strata.basics.ReferenceData) BillTrade::resolve(ReferenceData)}
 * to convert {@code BillTrade} to {@code ResolvedBillTrade}.
 * 
 * <h4>Price and yield</h4>
 * Strata uses <i>decimal</i> yields and prices for bills in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932 and a yield of 1.32% is represented by 0.0132.
 */
public class BillTradeCalculations {

  /**
   * Default implementation.
   */
  public static final BillTradeCalculations DEFAULT = new BillTradeCalculations(
      DiscountingBillTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedBillTrade}.
   */
  private final BillMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBillTrade}
   */
  public BillTradeCalculations(
      DiscountingBillTradePricer tradePricer) {
    this.calc = new BillMeasureCalculations(tradePricer);
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
  public CurrencyScenarioArray presentValue(
      ResolvedBillTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
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
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider ratesProvider) {

    return calc.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBillTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBillTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBillTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider ratesProvider) {

    return calc.pv01CalibratedSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBillTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBillTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBillTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider ratesProvider) {

    return calc.pv01CalibratedBucketed(trade, ratesProvider);
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
      ResolvedBillTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
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
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider ratesProvider) {

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
  public CurrencyScenarioArray currentCash(
      ResolvedBillTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
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
  public CurrencyAmount currentCash(
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider ratesProvider) {

    return calc.currentCash(trade, ratesProvider);
  }

}
