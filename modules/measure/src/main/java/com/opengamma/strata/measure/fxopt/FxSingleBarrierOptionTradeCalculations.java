/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.fxopt.BlackFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOptionTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOptionTrade;

/**
 * Calculates pricing and risk measures for FX single barrier option trades.
 * <p>
 * This provides a high-level entry point for FX single barrier option pricing and risk measures.
 * Pricing is performed using the Black method.
 * <p>
 * Each method takes a {@link ResolvedFxSingleBarrierOptionTrade}, whereas application code will
 * typically work with {@link FxSingleBarrierOptionTrade}. Call
 * {@link FxSingleBarrierOptionTrade#resolve(com.opengamma.strata.basics.ReferenceData) FxSingleBarrierOptionTrade::resolve(ReferenceData)}
 * to convert {@code FxSingleBarrierOptionTrade} to {@code ResolvedFxSingleBarrierOptionTrade}.
 */
public class FxSingleBarrierOptionTradeCalculations {

  /**
   * Default implementation.
   */
  public static final FxSingleBarrierOptionTradeCalculations DEFAULT = new FxSingleBarrierOptionTradeCalculations(
      BlackFxSingleBarrierOptionTradePricer.DEFAULT,
      ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSingleBarrierOptionTrade}.
   */
  private final FxSingleBarrierOptionMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param blackPricer  the pricer for {@link ResolvedFxSingleBarrierOptionTrade} using Black
   * @param trinomialTreePricer  the pricer for {@link ResolvedFxSingleBarrierOptionTrade} using Trinomial-Tree
   */
  public FxSingleBarrierOptionTradeCalculations(
      BlackFxSingleBarrierOptionTradePricer blackPricer,
      ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer trinomialTreePricer) {
    this.calc = new FxSingleBarrierOptionMeasureCalculations(blackPricer, trinomialTreePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @param method  the pricing method
   * @return the present value, one entry per scenario
   */
  public MultiCurrencyScenarioArray presentValue(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.presentValue(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.presentValue(trade, ratesProvider, volatilities, method);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @param method  the pricing method
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesCalibratedSum(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesCalibratedSum(trade, ratesProvider, volatilities, method);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @param method  the pricing method
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesCalibratedBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesCalibratedBucketed(trade, ratesProvider, volatilities, method);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in
   * the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @param method  the pricing method
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesMarketQuoteSum(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in
   * the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesMarketQuoteSum(trade, ratesProvider, volatilities, method);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in
   * the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param method  the pricing method
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesMarketQuoteBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in
   * the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.pv01RatesMarketQuoteBucketed(trade, ratesProvider, volatilities, method);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @param method  the pricing method
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.currencyExposure(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.currencyExposure(trade, ratesProvider, volatilities, method);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates current cash across one or more scenarios.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @param method  the pricing method
   * @return the current cash, one entry per scenario
   */
  public CurrencyScenarioArray currentCash(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData,
      FxSingleBarrierOptionMethod method) {

    return calc.currentCash(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData),
        method);
  }

  /**
   * Calculates current cash for a single set of market data.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @param method  the pricing method
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    return calc.currentCash(trade, ratesProvider.getValuationDate(), method);
  }

}
