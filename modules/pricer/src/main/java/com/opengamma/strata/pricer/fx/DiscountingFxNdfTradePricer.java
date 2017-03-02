/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxNdf;
import com.opengamma.strata.product.fx.ResolvedFxNdfTrade;

/**
 * Pricer for FX non-deliverable forward (NDF) trades.
 * <p>
 * This provides the ability to price an {@link ResolvedFxNdfTrade}.
 * The product is priced using forward curves for the currency pair.
 */
public class DiscountingFxNdfTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxNdfTradePricer DEFAULT = new DiscountingFxNdfTradePricer(
      DiscountingFxNdfProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxNdf}.
   */
  private final DiscountingFxNdfProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFxNdf}
   */
  public DiscountingFxNdfTradePricer(
      DiscountingFxNdfProductPricer productPricer) {
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
  public CurrencyAmount presentValue(ResolvedFxNdfTrade trade, RatesProvider provider) {
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
  public PointSensitivities presentValueSensitivity(ResolvedFxNdfTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure by discounting each payment in its own currency.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFxNdfTrade trade, RatesProvider provider) {
    return productPricer.currencyExposure(trade.getProduct(), provider);
  }

  /**
   * Calculates the current cash of the trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the current cash of the trade in the settlement currency
   */
  public CurrencyAmount currentCash(ResolvedFxNdfTrade trade, RatesProvider provider) {
    return productPricer.currentCash(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward exchange rate.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxNdfTrade trade, RatesProvider provider) {
    return productPricer.forwardFxRate(trade.getProduct(), provider);
  }

}
