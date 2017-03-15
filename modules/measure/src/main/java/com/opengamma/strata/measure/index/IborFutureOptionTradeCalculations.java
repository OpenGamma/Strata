/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.NormalIborFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Calculates pricing and risk measures for trades in an option contract based on an Ibor index future.
 * <p>
 * This provides a high-level entry point for option pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedIborFutureOptionTrade}, whereas application code will
 * typically work with {@link IborFutureOptionTrade}. Call
 * {@link IborFutureOptionTrade#resolve(com.opengamma.strata.basics.ReferenceData) IborFutureOptionTrade::resolve(ReferenceData)}
 * to convert {@code IborFutureOptionTrade} to {@code ResolvedIborFutureOptionTrade}.
 * 
 * <h4>Price</h4>
 * The price of an Ibor future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
 */
public class IborFutureOptionTradeCalculations {

  /**
   * Default implementation.
   */
  public static final IborFutureOptionTradeCalculations DEFAULT = new IborFutureOptionTradeCalculations(
      NormalIborFutureOptionMarginedTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedIborFutureOptionTrade}.
   */
  private final IborFutureOptionMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedIborFutureOptionTrade}
   */
  public IborFutureOptionTradeCalculations(
      NormalIborFutureOptionMarginedTradePricer tradePricer) {
    this.calc = new IborFutureOptionMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the rates market data
   * @param optionLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public CurrencyScenarioArray presentValue(
      ResolvedIborFutureOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborFutureOptionMarketDataLookup optionLookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(trade, ratesLookup.marketDataView(marketData), optionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return calc.presentValue(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the rates market data
   * @param optionLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedIborFutureOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborFutureOptionMarketDataLookup optionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, ratesLookup.marketDataView(marketData), optionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return calc.pv01CalibratedSum(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the rates market data
   * @param optionLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborFutureOptionMarketDataLookup optionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, ratesLookup.marketDataView(marketData), optionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return calc.pv01CalibratedBucketed(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the rates market data
   * @param optionLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01MarketQuoteSum(
      ResolvedIborFutureOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborFutureOptionMarketDataLookup optionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteSum(trade, ratesLookup.marketDataView(marketData), optionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01MarketQuoteSum(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return calc.pv01MarketQuoteSum(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the rates market data
   * @param optionLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01MarketQuoteBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborFutureOptionMarketDataLookup optionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01MarketQuoteBucketed(trade, ratesLookup.marketDataView(marketData), optionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedIborFutureOptionTrade, RatesMarketDataLookup, IborFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the market quotes used to calibrate the curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01MarketQuoteBucketed(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return calc.pv01MarketQuoteBucketed(trade, ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates unit price across one or more scenarios.
   * <p>
   * This is the price of a single unit of the security.
   * <p>
   * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
   * The decimal price is based on the decimal rate equivalent to the percentage.
   * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the rates market data
   * @param optionLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public DoubleScenarioArray unitPrice(
      ResolvedIborFutureOptionTrade trade,
      RatesMarketDataLookup ratesLookup,
      IborFutureOptionMarketDataLookup optionLookup,
      ScenarioMarketData marketData) {

    return calc.unitPrice(trade, ratesLookup.marketDataView(marketData), optionLookup.marketDataView(marketData));
  }

  /**
   * Calculates unit price for a single set of market data.
   * <p>
   * This is the price of a single unit of the security.
   * <p>
   * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
   * The decimal price is based on the decimal rate equivalent to the percentage.
   * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value
   */
  public double unitPrice(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities) {

    return calc.unitPrice(trade, ratesProvider, volatilities);
  }

}
