/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for for rate swap trades.
 * <p>
 * This function provides the ability to price a {@link SwapTrade}.
 * The product is priced by pricing the product.
 */
public class DiscountingSwapTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapTradePricer DEFAULT = new DiscountingSwapTradePricer(
      DiscountingSwapProductPricer.DEFAULT);

  /**
   * Pricer for {@link SwapProduct}.
   */
  private final DiscountingSwapProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link SwapProduct}
   */
  public DiscountingSwapTradePricer(DiscountingSwapProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap trade, converted to the specified currency.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param trade  the trade to price
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap trade in the specified currency
   */
  public CurrencyAmount presentValue(SwapTrade trade, Currency currency, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), currency, provider);
  }

  /**
   * Calculates the present value of the swap trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted future value.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the present value of the swap trade
   */
  public MultiCurrencyAmount presentValue(SwapTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value sensitivity of the swap trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap trade
   */
  public PointSensitivities presentValueSensitivity(SwapTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future value of the swap trade.
   * <p>
   * The future value of the trade is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the future value of the swap trade
   */
  public MultiCurrencyAmount futureValue(SwapTrade trade, RatesProvider provider) {
    return productPricer.futureValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the future value sensitivity of the swap trade.
   * <p>
   * The future value sensitivity of the trade is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the future value curve sensitivity of the swap trade
   */
  public PointSensitivities futureValueSensitivity(SwapTrade trade, RatesProvider provider) {
    return productPricer.futureValueSensitivity(trade.getProduct(), provider).build();
  }

}
