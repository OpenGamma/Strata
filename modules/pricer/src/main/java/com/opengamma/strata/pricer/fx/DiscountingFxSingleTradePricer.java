/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleTrade;

/**
 * Pricer for foreign exchange transaction trades.
 * <p>
 * This provides the ability to price an {@link ResolvedFxSingleTrade}.
 */
public class DiscountingFxSingleTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxSingleTradePricer DEFAULT = new DiscountingFxSingleTradePricer(
      DiscountingFxSingleProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSingle}.
   */
  private final DiscountingFxSingleProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFxSingle}
   */
  public DiscountingFxSingleTradePricer(
      DiscountingFxSingleProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The present value is returned in the settlement currency.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value of the trade in the settlement currency
   */
  public MultiCurrencyAmount presentValue(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value curve sensitivity of the trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread.
   * <p>
   * This is the spread that should be added to the FX points to have a zero value.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.parSpread(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure by discounting each payment in its own currency.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.currencyExposure(trade.getProduct(), provider);
  }

  /**
   * Calculates the current cash of the trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the current cash of the trade in the settlement currency
   */
  public MultiCurrencyAmount currentCash(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.currentCash(trade.getProduct(), provider.getValuationDate());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward exchange rate.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.forwardFxRate(trade.getProduct(), provider);
  }

  /**
   * Calculates the forward exchange rate point sensitivity.
   * <p>
   * The returned value is based on the direction of the FX product.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the point sensitivity
   */
  public PointSensitivities forwardFxRatePointSensitivity(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.forwardFxRatePointSensitivity(trade.getProduct(), provider).build();
  }

  /**
   * Calculates the sensitivity of the forward exchange rate to the spot rate.
   * <p>
   * The returned value is based on the direction of the FX product.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the sensitivity to spot
   */
  public double forwardFxRateSpotSensitivity(ResolvedFxSingleTrade trade, RatesProvider provider) {
    return productPricer.forwardFxRateSpotSensitivity(trade.getProduct(), provider);
  }

}
