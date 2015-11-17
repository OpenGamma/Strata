/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.SwapProduct;
import com.opengamma.strata.product.swap.SwapTrade;

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
   * This is the discounted forecast value.
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
   * This is the discounted forecast value.
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
   * Calculates the forecast value of the swap trade.
   * <p>
   * The forecast value of the trade is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the forecast value of the swap trade
   */
  public MultiCurrencyAmount forecastValue(SwapTrade trade, RatesProvider provider) {
    return productPricer.forecastValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the forecast value sensitivity of the swap trade.
   * <p>
   * The forecast value sensitivity of the trade is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the swap trade
   */
  public PointSensitivities forecastValueSensitivity(SwapTrade trade, RatesProvider provider) {
    return productPricer.forecastValueSensitivity(trade.getProduct(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flows of the swap trade.
   * <p>
   * Each expected cash flow is added to the result.
   * This is based on {@link #forecastValue(SwapTrade, RatesProvider)}.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(SwapTrade trade, RatesProvider provider) {
    return productPricer.cashFlows(trade.getProduct(), provider);
  }

}
