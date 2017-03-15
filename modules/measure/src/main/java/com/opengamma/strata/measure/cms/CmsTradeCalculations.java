/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsTrade;
import com.opengamma.strata.product.cms.ResolvedCmsTrade;

/**
 * Calculates pricing and risk measures for constant maturity swap (CMS) trades.
 * <p>
 * This provides a high-level entry point for CMS pricing and risk measures.
 * CMS pricing uses swaption volatilities with the SABR model.
 * Additional model parameters must be specified using {@link CmsSabrExtrapolationParams}.
 * <p>
 * Each method takes a {@link ResolvedCmsTrade}, whereas application code will
 * typically work with {@link CmsTrade}. Call
 * {@link CmsTrade#resolve(com.opengamma.strata.basics.ReferenceData) CmsTrade::resolve(ReferenceData)}
 * to convert {@code CmsTrade} to {@code ResolvedCmsTrade}.
 */
public class CmsTradeCalculations {

  /**
   * Pricer for {@link ResolvedCmsTrade}.
   */
  private final CmsMeasureCalculations calc;

  /**
   * Obtains an instance specifying the SABR extrapolation parameters.
   * 
   * @param cmsParams  the parameters for SABR pricing of CMS
   * @return the trade calculations
   */
  public static CmsTradeCalculations of(CmsSabrExtrapolationParams cmsParams) {
    return new CmsTradeCalculations(cmsParams);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance specifying the SABR extrapolation parameters.
   * 
   * @param cmsParams  the parameters for SABR pricing of CMS
   */
  private CmsTradeCalculations(CmsSabrExtrapolationParams cmsParams) {
    this.calc = new CmsMeasureCalculations(cmsParams);
  }

  /**
   * Creates an instance specifying the SABR pricer.
   * 
   * @param tradePricer  the pricer for {@link ResolvedCmsTrade}
   */
  public CmsTradeCalculations(
      SabrExtrapolationReplicationCmsTradePricer tradePricer) {
    this.calc = new CmsMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param ratesLookup  the lookup used to query the market data
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public MultiCurrencyScenarioArray presentValue(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the swaption volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

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
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesCalibratedSum(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

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
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesCalibratedBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of present value to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

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
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesMarketQuoteSum(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
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
   * @param volatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

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
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.pv01RatesMarketQuoteBucketed(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
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
   * @param volatilities  the swaption volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

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
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the swaption volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

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
   * @param swaptionLookup  the lookup used to query the swaption market data
   * @param marketData  the market data
   * @return the current cash, one entry per scenario
   */
  public MultiCurrencyScenarioArray currentCash(
      ResolvedCmsTrade trade,
      RatesMarketDataLookup ratesLookup,
      SwaptionMarketDataLookup swaptionLookup,
      ScenarioMarketData marketData) {

    return calc.currentCash(
        trade,
        ratesLookup.marketDataView(marketData),
        swaptionLookup.marketDataView(marketData));
  }

  /**
   * Calculates current cash for a single set of market data.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the market data
   * @param volatilities  the swaption volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedCmsTrade trade,
      RatesProvider ratesProvider,
      SwaptionVolatilities volatilities) {

    return calc.currentCash(trade, ratesProvider, volatilities);
  }

}
