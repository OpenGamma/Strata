/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.payment;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.payment.DiscountingBulletPaymentTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Calculates pricing and risk measures for bullet payment trades.
 * <p>
 * This provides a high-level entry point for bullet payment pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedBulletPaymentTrade}, whereas application code will
 * typically work with {@link BulletPaymentTrade}. Call
 * {@link BulletPaymentTrade#resolve(com.opengamma.strata.basics.ReferenceData) BulletPaymentTrade::resolve(ReferenceData)}
 * to convert {@code BulletPaymentTrade} to {@code ResolvedBulletPaymentTrade}.
 */
public class BulletPaymentTradeCalculations {

  /**
   * Default implementation.
   */
  public static final BulletPaymentTradeCalculations DEFAULT = new BulletPaymentTradeCalculations(
      DiscountingBulletPaymentTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedBulletPaymentTrade}.
   */
  private final BulletPaymentMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param paymentPricer  the pricer for {@link Payment}
   * @deprecated use constructor taking {@link DiscountingBulletPaymentTradePricer}
   */
  @Deprecated
  public BulletPaymentTradeCalculations(DiscountingPaymentPricer paymentPricer) {
    this(new DiscountingBulletPaymentTradePricer(paymentPricer));
  }

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBulletPaymentTrade}
   */
  public BulletPaymentTradeCalculations(
      DiscountingBulletPaymentTradePricer tradePricer) {
    this.calc = new BulletPaymentMeasureCalculations(tradePricer);
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
      ResolvedBulletPaymentTrade trade,
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
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.presentValue(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBulletPaymentTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01CalibratedSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01CalibratedBucketed(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedBulletPaymentTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01MarketQuoteSum(trade, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBulletPaymentTrade, RatesMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.pv01MarketQuoteBucketed(trade, ratesProvider);
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
      ResolvedBulletPaymentTrade trade,
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
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.cashFlows(trade, ratesProvider);
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
      ResolvedBulletPaymentTrade trade,
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
      ResolvedBulletPaymentTrade trade,
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
  public CurrencyScenarioArray currentCash(
      ResolvedBulletPaymentTrade trade,
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
  public CurrencyAmount currentCash(
      ResolvedBulletPaymentTrade trade,
      RatesProvider ratesProvider) {

    return calc.currentCash(trade, ratesProvider);
  }

}
