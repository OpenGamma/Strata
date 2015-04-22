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
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the present value of the trade
   */
  public CurrencyAmount presentValue(FraTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value sensitivity of the FRA trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(FraTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future value of the FRA trade.
   * <p>
   * The future value of the trade is the value on the valuation date without present value discounting.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the future value of the trade
   */
  public CurrencyAmount futureValue(FraTrade trade, RatesProvider provider) {
    return productPricer.futureValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the future value sensitivity of the FRA trade.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the point sensitivity of the future value
   */
  public PointSensitivities futureValueSensitivity(FraTrade trade, RatesProvider provider) {
    return productPricer.futureValueSensitivity(trade.getProduct(), provider);
  }

}
