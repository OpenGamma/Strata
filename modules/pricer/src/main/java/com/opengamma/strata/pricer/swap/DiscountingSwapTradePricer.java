/*
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
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Pricer for for rate swap trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedSwapTrade}.
 * The product is priced by pricing the product.
 */
public class DiscountingSwapTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapTradePricer DEFAULT = new DiscountingSwapTradePricer(
      DiscountingSwapProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedSwap}.
   */
  private final DiscountingSwapProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedSwap}
   */
  public DiscountingSwapTradePricer(DiscountingSwapProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying product pricer.
   * 
   * @return the product pricer
   */
  public DiscountingSwapProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap trade, converted to the specified currency.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is converted to the specified currency.
   * 
   * @param trade  the trade
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap trade in the specified currency
   */
  public CurrencyAmount presentValue(ResolvedSwapTrade trade, Currency currency, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), currency, provider);
  }

  /**
   * Calculates the present value of the swap trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value of the swap trade
   */
  public MultiCurrencyAmount presentValue(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Explains the present value of the swap trade.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.explainPresentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value sensitivity of the swap trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap trade
   */
  public PointSensitivities presentValueSensitivity(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forecast value of the swap trade.
   * <p>
   * The forecast value of the trade is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the forecast value of the swap trade
   */
  public MultiCurrencyAmount forecastValue(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.forecastValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the forecast value sensitivity of the swap trade.
   * <p>
   * The forecast value sensitivity of the trade is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the swap trade
   */
  public PointSensitivities forecastValueSensitivity(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.forecastValueSensitivity(trade.getProduct(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par rate of the swap trade.
   * <p>
   * The par rate is the rate for which the swap present value is 0.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.parRate(trade.getProduct(), provider);
  }

  /**
   * Calculates the par rate curve sensitivity of the swap trade.
   * <p>
   * The par rate curve sensitivity of the product is the sensitivity of the par rate to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.parRateSensitivity(trade.getProduct(), provider).build();
  }

  /**
   * Calculates the par spread of the swap trade.
   * <p>
   * This is spread to be added to the fixed rate to have a present value of 0.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.parSpread(trade.getProduct(), provider);
  }

  /**
   * Calculates the par spread curve sensitivity of the swap trade.
   * <p>
   * The par spread curve sensitivity of the product is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.parSpreadSensitivity(trade.getProduct(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flows of the swap trade.
   * <p>
   * Each expected cash flow is added to the result.
   * This is based on {@link #forecastValue(ResolvedSwapTrade, RatesProvider)}.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.cashFlows(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest since the last payment.
   * <p>
   * This determines the payment period applicable at the valuation date and calculates
   * the accrued interest since the last payment.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the accrued interest
   */
  public MultiCurrencyAmount accruedInterest(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.accruedInterest(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the swap trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the currency exposure of the swap trade
   */
  public MultiCurrencyAmount currencyExposure(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.currencyExposure(trade.getProduct(), provider);
  }

  /**
   * Calculates the current cash of the swap trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the current cash of the swap trade
   */
  public MultiCurrencyAmount currentCash(ResolvedSwapTrade trade, RatesProvider provider) {
    return productPricer.currentCash(trade.getProduct(), provider);
  }

}
