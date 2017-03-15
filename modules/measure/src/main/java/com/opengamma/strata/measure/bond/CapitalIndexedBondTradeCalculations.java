/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.bond.DiscountingCapitalIndexedBondTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondTrade;

/**
 * Calculates pricing and risk measures for forward rate agreement (capital indexed bond) trades.
 * <p>
 * This provides a high-level entry point for capital indexed bond pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedCapitalIndexedBondTrade}, whereas application code will
 * typically work with {@link CapitalIndexedBondTrade}. Call
 * {@link CapitalIndexedBondTrade#resolve(com.opengamma.strata.basics.ReferenceData) CapitalIndexedBondTrade::resolve(ReferenceData)}
 * to convert {@code CapitalIndexedBondTrade} to {@code ResolvedCapitalIndexedBondTrade}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
public class CapitalIndexedBondTradeCalculations {

  /**
   * Default implementation.
   */
  public static final CapitalIndexedBondTradeCalculations DEFAULT = new CapitalIndexedBondTradeCalculations(
      DiscountingCapitalIndexedBondTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedCapitalIndexedBondTrade}.
   */
  private final CapitalIndexedBondMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedCapitalIndexedBondTrade}
   */
  public CapitalIndexedBondTradeCalculations(
      DiscountingCapitalIndexedBondTradePricer tradePricer) {
    this.calc = new CapitalIndexedBondMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param legalEntityLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public CurrencyScenarioArray presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesMarketDataLookup ratesLookup,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(trade, ratesLookup.marketDataView(marketData), legalEntityLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param legalEntityProvider  the market data
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider legalEntityProvider) {

    return calc.presentValue(trade, ratesProvider, legalEntityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedCapitalIndexedBondTrade, RatesMarketDataLookup, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param legalEntityLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedCapitalIndexedBondTrade trade,
      RatesMarketDataLookup ratesLookup,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(
        trade,
        ratesLookup.marketDataView(marketData),
        legalEntityLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedCapitalIndexedBondTrade, RatesMarketDataLookup, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param legalEntityProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider legalEntityProvider) {

    return calc.pv01CalibratedSum(trade, ratesProvider, legalEntityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedCapitalIndexedBondTrade, RatesMarketDataLookup, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param legalEntityLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedCapitalIndexedBondTrade trade,
      RatesMarketDataLookup ratesLookup,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        legalEntityLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedCapitalIndexedBondTrade, RatesMarketDataLookup, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param legalEntityProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider legalEntityProvider) {

    return calc.pv01CalibratedBucketed(trade, ratesProvider, legalEntityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param legalEntityLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesMarketDataLookup ratesLookup,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(
        trade,
        ratesLookup.marketDataView(marketData),
        legalEntityLookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param legalEntityProvider  the market data
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider legalEntityProvider) {

    return calc.currencyExposure(trade, ratesProvider, legalEntityProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates current cash across one or more scenarios.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param legalEntityLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the current cash, one entry per scenario
   */
  public CurrencyScenarioArray currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesMarketDataLookup ratesLookup,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      ScenarioMarketData marketData) {

    return calc.currentCash(
        trade,
        ratesLookup.marketDataView(marketData),
        legalEntityLookup.marketDataView(marketData));
  }

  /**
   * Calculates current cash for a single set of market data.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param legalEntityProvider  the market data
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider legalEntityProvider) {

    return calc.currentCash(trade, ratesProvider);
  }

}
