/*
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
import com.opengamma.strata.pricer.fxopt.DiscountingFxCollarTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxCollarTrade;

/**
 * Calculates pricing and risk measures for FX collar trades.
 * <p>
 * This provides a high-level entry point for FX collar pricing and risk measures.
 * Pricing is performed using the Black method.
 * <p>
 * Each method takes a {@link ResolvedFxCollarTrade}, whereas application code will
 * typically work with {@link com.opengamma.strata.product.fxopt.FxCollarTrade}
 * . Call
 * {@link com.opengamma.strata.product.fxopt.FxCollarTrade
 * #resolve(com.opengamma.strata.basics.ReferenceData) FxVanillaOptionTrade::resolve(ReferenceData)}
 * to convert {@code FxVanillaOptionTrade} to {@code ResolvedFxVanillaOptionTrade}.
 */
public class FxCollarTradeCalculations {

  /**
   * Default implementation.
   */
  public static final FxCollarTradeCalculations DEFAULT = new FxCollarTradeCalculations(
      DiscountingFxCollarTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxCollarTrade}.
   */
  private final FxCollarMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   *
   * @param tradePricer  the pricer for {@link ResolvedFxCollarTrade}
   */
  public FxCollarTradeCalculations(
      DiscountingFxCollarTradePricer tradePricer) {
    this.calc = new FxCollarMeasureCalculations(tradePricer);
  }

  /**
   * Calculates present value across one or more scenarios.
   *
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public MultiCurrencyScenarioArray presentValue(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   *
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.presentValue(trade, ratesProvider, volatilities);
  }

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
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesCalibratedSum(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
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
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.pv01RatesCalibratedSum(trade, ratesProvider, volatilities);
  }

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
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesCalibratedBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
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
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.pv01RatesCalibratedBucketed(trade, ratesProvider, volatilities);
  }

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
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesMarketQuoteSum(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
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
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.pv01RatesMarketQuoteSum(trade, ratesProvider, volatilities);
  }

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
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesMarketQuoteBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
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
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.pv01RatesMarketQuoteBucketed(trade, ratesProvider, volatilities);
  }

  /**
   * Calculates present value vega sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of present value to the implied volatilities
   * used to calibrate the curves.
   *
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @param fxLookup  the lookup used to query the option market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> vegaMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.vegaMarketQuoteBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value vega sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value of the implied volatilities
   * used to calibrate the curves.
   *
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities vegaMarketQuoteBucketed(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.vegaMarketQuoteBucketed(trade, ratesProvider, volatilities);
  }

  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   *
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param fxLookup  the lookup used to query the option market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   *
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the option volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities) {

    return calc.currencyExposure(trade, ratesProvider, volatilities);
  }

  /**
   * Calculates current cash across one or more scenarios.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   *
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param marketData  the market data
   * @param fxLookup  the market data lookup
   * @return the current cash, one entry per scenario
   */
  public CurrencyScenarioArray currentCash(
      ResolvedFxCollarTrade trade,
      RatesMarketDataLookup ratesLookup,
      FxOptionMarketDataLookup fxLookup,
      ScenarioMarketData marketData) {

    return calc.currentCash(
        trade,
        ratesLookup.marketDataView(marketData),
        fxLookup.marketDataView(marketData));
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
      ResolvedFxCollarTrade trade,
      RatesProvider ratesProvider) {

    return calc.currentCash(trade, ratesProvider.getValuationDate());
  }
}
