/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.capfloor;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.capfloor.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.capfloor.VolatilityIborCapFloorTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorTrade;

/**
 * Calculates pricing and risk measures for cap/floor trades.
 * <p>
 * This provides a high-level entry point for cap/floor pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedIborCapFloorTrade}, whereas application code will
 * typically work with {@link IborCapFloorTrade}. Call
 * {@link IborCapFloorTrade#resolve(com.opengamma.strata.basics.ReferenceData) CapFloorTrade::resolve(ReferenceData)}
 * to convert {@code CapFloorTrade} to {@code ResolvedIborCapFloorTrade}.
 */
public class IborCapFloorTradeCalculations {

  /**
   * Default implementation.
   */
  public static final IborCapFloorTradeCalculations DEFAULT = new IborCapFloorTradeCalculations(
      VolatilityIborCapFloorTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedIborCapFloorTrade}.
   */
  private final IborCapFloorMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedIborCapFloorTrade}
   */
  public IborCapFloorTradeCalculations(
      VolatilityIborCapFloorTradePricer tradePricer) {
    this.calc = new IborCapFloorMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public MultiCurrencyScenarioArray presentValue(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the cap/floor volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.presentValue(trade, ratesProvider, volatilities);
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
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesCalibratedSum(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the cap/floor volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.pv01RatesCalibratedSum(trade, ratesProvider, volatilities);
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
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesCalibratedBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the cap/floor volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.pv01RatesCalibratedBucketed(trade, ratesProvider, volatilities);
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
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesMarketQuoteSum(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
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
   * @param volatilities  the cap/floor volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.pv01RatesMarketQuoteSum(trade, ratesProvider, volatilities);
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
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesMarketQuoteBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
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
   * @param volatilities  the cap/floor volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.pv01RatesMarketQuoteBucketed(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the cap/floor volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.currencyExposure(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates current cash across one or more scenarios.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param capFloorLookup  the lookup used to query the cap/floor market data
   * @param marketData  the market data
   * @return the current cash, one entry per scenario
   */
  public MultiCurrencyScenarioArray currentCash(
      ResolvedIborCapFloorTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborCapFloorMarketDataLookup capFloorLookup,
      ScenarioMarketData marketData) {

    return calc.currentCash(
        trade,
        ratesLookup.marketDataView(marketData),
        capFloorLookup.marketDataView(marketData));
  }

  /**
   * Calculates current cash for a single set of market data.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the cap/floor volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return calc.currentCash(trade, ratesProvider, volatilities);
  }

}
