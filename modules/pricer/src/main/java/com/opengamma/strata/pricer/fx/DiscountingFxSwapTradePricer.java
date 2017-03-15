/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSwap;
import com.opengamma.strata.product.fx.ResolvedFxSwapTrade;

/**
 * Pricer for foreign exchange swap transaction trades.
 * <p>
 * This provides the ability to price an {@link ResolvedFxSwapTrade}.
 */
public class DiscountingFxSwapTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxSwapTradePricer DEFAULT = new DiscountingFxSwapTradePricer(
      DiscountingFxSwapProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSwap}.
   */
  private final DiscountingFxSwapProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFxSwap}
   */
  public DiscountingFxSwapTradePricer(
      DiscountingFxSwapProductPricer productPricer) {
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
  public MultiCurrencyAmount presentValue(ResolvedFxSwapTrade trade, RatesProvider provider) {
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
  public PointSensitivities presentValueSensitivity(ResolvedFxSwapTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread.
   * <p>
   * The par spread is the spread that should be added to the FX forward points to have a zero value.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(ResolvedFxSwapTrade trade, RatesProvider provider) {
    return productPricer.parSpread(trade.getProduct(), provider);
  }

  /**
   * Calculates the par spread sensitivity to the curves.
   * <p>
   * The sensitivity is reported in the counter currency of the product, but is actually dimensionless.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedFxSwapTrade trade, RatesProvider provider) {
    return productPricer.parSpreadSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure by discounting each payment in its own currency.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFxSwapTrade trade, RatesProvider provider) {
    return productPricer.currencyExposure(trade.getProduct(), provider);
  }

  /**
   * Calculates the current cash of the trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the current cash of the trade in the settlement currency
   */
  public MultiCurrencyAmount currentCash(ResolvedFxSwapTrade trade, RatesProvider provider) {
    return productPricer.currentCash(trade.getProduct(), provider.getValuationDate());
  }

}
