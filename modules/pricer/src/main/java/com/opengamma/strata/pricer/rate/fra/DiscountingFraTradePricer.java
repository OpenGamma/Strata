/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.fra.FraProduct;
import com.opengamma.strata.product.rate.fra.FraTrade;

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
   * This is the discounted forecast value.
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
   * Calculates the forecast value of the FRA trade.
   * <p>
   * The forecast value of the trade is the value on the valuation date without present value discounting.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the forecast value of the trade
   */
  public CurrencyAmount forecastValue(FraTrade trade, RatesProvider provider) {
    return productPricer.forecastValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the forecast value sensitivity of the FRA trade.
   * <p>
   * The forecast value sensitivity of the product is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the point sensitivity of the forecast value
   */
  public PointSensitivities forecastValueSensitivity(FraTrade trade, RatesProvider provider) {
    return productPricer.forecastValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flow of the FRA trade.
   * <p>
   * There is only one cash flow on the payment date for the FRA trade.
   * The expected currency amount of the cash flow is the same as {@link #forecastValue(FraTrade, RatesProvider)}.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(FraTrade trade, RatesProvider provider) {
    return productPricer.cashFlows(trade.getProduct(), provider);
  }

}
