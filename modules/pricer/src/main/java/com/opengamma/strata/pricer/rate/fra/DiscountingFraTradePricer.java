/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.fra.FraProduct;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for for forward rate agreement (FRA) trades.
 * <p>
 * This function provides the ability to price a {@link FraTrade}.
 * The trade is priced by pricing the underlying product using a forward curve for the index.
 */
public class DiscountingFraTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFraTradePricer DEFAULT = new DiscountingFraTradePricer(
      DiscountingFraProductPricer.DEFAULT);

  /**
   * Pricer for {@link FraProduct}.
   */
  private final DiscountingFraProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link FraProduct}
   */
  public DiscountingFraTradePricer(
      DiscountingFraProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FRA trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted future value.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the present value of the trade
   */
  public CurrencyAmount presentValue(RatesProvider provider, FraTrade trade) {
    return productPricer.presentValue(provider, trade.getProduct());
  }

  /**
   * Calculates the present value sensitivity of the FRA trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(RatesProvider provider, FraTrade trade) {
    return productPricer.presentValueSensitivity(provider, trade.getProduct());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future value of the FRA trade.
   * <p>
   * The future value of the trade is the value on the valuation date without present value discounting.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the future value of the trade
   */
  public CurrencyAmount futureValue(RatesProvider provider, FraTrade trade) {
    return productPricer.futureValue(provider, trade.getProduct());
  }

  /**
   * Calculates the future value sensitivity of the FRA trade.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the point sensitivity of the future value
   */
  public PointSensitivities futureValueSensitivity(RatesProvider provider, FraTrade trade) {
    return productPricer.futureValueSensitivity(provider, trade.getProduct());
  }

}
