/*
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
import com.opengamma.strata.pricer.bond.DiscountingBondFutureTradePricer;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.BondFutureTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Calculates pricing and risk measures for trades in a futures contract based on a basket of bonds.
 * <p>
 * This provides a high-level entry point for future pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedBondFutureTrade}, whereas application code will
 * typically work with {@link BondFutureTrade}. Call
 * {@link BondFutureTrade#resolve(com.opengamma.strata.basics.ReferenceData) BondFutureTrade::resolve(ReferenceData)}
 * to convert {@code BondFutureTrade} to {@code ResolvedBondFutureTrade}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
 * for an amount computed from the bond future price, a conversion factor and the accrued interest.
 */
public class BondFutureTradeCalculations {

  /**
   * Default implementation.
   */
  public static final BondFutureTradeCalculations DEFAULT = new BondFutureTradeCalculations(
      DiscountingBondFutureTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedBondFutureTrade}.
   */
  private final BondFutureMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedBondFutureTrade}
   */
  public BondFutureTradeCalculations(
      DiscountingBondFutureTradePricer tradePricer) {
    this.calc = new BondFutureMeasureCalculations(tradePricer);
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
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.presentValue(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.pv01CalibratedSum(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedBondFutureTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.pv01CalibratedBucketed(trade, discountingProvider);
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
  public DoubleScenarioArray parSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.parSpread(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates par spread for a single set of market data.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the par spread
   */
  public double parSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.parSpread(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates unit price across one or more scenarios.
   * <p>
   * This is the price of a single unit of the security.
   * 
   * <h4>Price</h4>
   * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
   * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
   * for an amount computed from the bond future price, a conversion factor and the accrued interest.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public DoubleScenarioArray unitPrice(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.unitPrice(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates unit price for a single set of market data.
   * <p>
   * This is the price of a single unit of the security.
   * 
   * <h4>Price</h4>
   * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
   * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
   * for an amount computed from the bond future price, a conversion factor and the accrued interest.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value
   */
  public double unitPrice(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.unitPrice(trade, discountingProvider);
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
      ResolvedBondFutureTrade trade,
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
   * @param discountingProvider  the market data
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.currencyExposure(trade, discountingProvider);
  }

}
