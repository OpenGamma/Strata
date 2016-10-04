/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.bond.BlackBondFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.bond.BondFutureVolatilities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.ResolvedBondFutureOptionTrade;

/**
 * Calculates pricing and risk measures for trades in an option contract based on an bond future.
 * <p>
 * This provides a high-level entry point for option pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedBondFutureOptionTrade}, whereas application code will
 * typically work with {@link BondFutureOptionTrade}. Call
 * {@link BondFutureOptionTrade#resolve(com.opengamma.strata.basics.ReferenceData) BondFutureOptionTrade::resolve(ReferenceData)}
 * to convert {@code BondFutureOptionTrade} to {@code ResolvedBondFutureOptionTrade}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link BondFuture}.
 */
public class BondFutureOptionTradeCalculations {

  /**
   * Default implementation.
   */
  public static final BondFutureOptionTradeCalculations DEFAULT = new BondFutureOptionTradeCalculations(
      BlackBondFutureOptionMarginedTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedBondFutureOptionTrade}.
   */
  private final BondFutureOptionMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBondFutureOptionTrade}
   */
  public BondFutureOptionTradeCalculations(
      BlackBondFutureOptionMarginedTradePricer tradePricer) {
    this.calc = new BondFutureOptionMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param legalEntityLookup  the lookup used to query the rates market data
   * @param volsLookup  the lookup used to query the volatility market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public CurrencyScenarioArray presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      BondFutureOptionMarketDataLookup volsLookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(trade, legalEntityLookup.marketDataView(marketData), volsLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @param volatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    return calc.presentValue(trade, discountingProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureOptionTrade, LegalEntityDiscountingMarketDataLookup, BondFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param legalEntityLookup  the lookup used to query the rates market data
   * @param volsLookup  the lookup used to query the volatility market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      BondFutureOptionMarketDataLookup volsLookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, legalEntityLookup.marketDataView(marketData), volsLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureOptionTrade, LegalEntityDiscountingMarketDataLookup, BondFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    return calc.pv01CalibratedSum(trade, discountingProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureOptionTrade, LegalEntityDiscountingMarketDataLookup, BondFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param legalEntityLookup  the lookup used to query the rates market data
   * @param volsLookup  the lookup used to query the volatility market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      BondFutureOptionMarketDataLookup volsLookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, legalEntityLookup.marketDataView(marketData),
        volsLookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureOptionTrade, LegalEntityDiscountingMarketDataLookup, BondFutureOptionMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    return calc.pv01CalibratedBucketed(trade, discountingProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates unit price across one or more scenarios.
   * <p>
   * This is the price of a single unit of the security.
   * 
   * <h4>Price</h4>
   * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
   * This is coherent with the pricing of {@link BondFuture}.
   * 
   * @param trade  the trade
   * @param legalEntityLookup  the lookup used to query the rates market data
   * @param volsLookup  the lookup used to query the volatility market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public DoubleScenarioArray unitPrice(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      BondFutureOptionMarketDataLookup volsLookup,
      ScenarioMarketData marketData) {

    return calc.unitPrice(trade, legalEntityLookup.marketDataView(marketData), volsLookup.marketDataView(marketData));
  }

  /**
   * Calculates unit price for a single set of market data.
   * <p>
   * This is the price of a single unit of the security.
   * 
   * <h4>Price</h4>
   * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
   * This is coherent with the pricing of {@link BondFuture}.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @param volatilities  the volatilities
   * @return the present value
   */
  public double unitPrice(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    return calc.unitPrice(trade, discountingProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param legalEntityLookup  the lookup used to query the rates market data
   * @param volsLookup  the lookup used to query the volatility market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingMarketDataLookup legalEntityLookup,
      BondFutureOptionMarketDataLookup volsLookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(trade, legalEntityLookup.marketDataView(marketData), volsLookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @param volatilities  the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      BondFutureVolatilities volatilities) {

    return calc.currencyExposure(trade, discountingProvider, volatilities);
  }

}
