/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.ResolvedOvernightFuture;
import com.opengamma.strata.product.index.ResolvedOvernightFutureTrade;

/**
 * Pricer implementation for Overnight rate future trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedOvernightFutureTrade}.
 * 
 * <h4>Price</h4>
 * The price of an Overnight rate future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Overnight rate futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
public class DiscountingOvernightFutureTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingOvernightFutureTradePricer DEFAULT =
      new DiscountingOvernightFutureTradePricer(DiscountingOvernightFutureProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DiscountingOvernightFutureProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedOvernightFuture}
   */
  public DiscountingOvernightFutureTradePricer(
      DiscountingOvernightFutureProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Overnight rate future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * The price is calculated using the discounting model.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the price of the trade, in decimal form
   */
  public double price(ResolvedOvernightFutureTrade trade, RatesProvider ratesProvider) {
    return productPricer.price(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the Overnight rate future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(ResolvedOvernightFutureTrade trade, RatesProvider ratesProvider) {
    return productPricer.priceSensitivity(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the reference price for the trade.
   * <p>
   * If the valuation date equals the trade date, then the reference price is the trade price.
   * Otherwise, the reference price is the last settlement price used for margining.
   * 
   * @param trade  the trade
   * @param valuationDate  the date for which the reference price should be calculated
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the reference price, in decimal form
   */
  double referencePrice(ResolvedOvernightFutureTrade trade, LocalDate valuationDate, double lastSettlementPrice) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    return trade.getTradedPrice()
        .filter(tp -> tp.getTradeDate().equals(valuationDate))
        .map(tp -> tp.getPrice())
        .orElse(lastSettlementPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Overnight rate future trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The calculation is performed against a reference price. The reference price
   * must be the last settlement price used for margining, except on the trade date,
   * when it must be the trade price.
   * 
   * @param trade  the trade
   * @param currentPrice  the current price, in decimal form
   * @param referencePrice  the reference price to margin against, typically the last settlement price, in decimal form
   * @return the present value
   */
  CurrencyAmount presentValue(ResolvedOvernightFutureTrade trade, double currentPrice, double referencePrice) {
    ResolvedOvernightFuture future = trade.getProduct();
    double priceIndex = productPricer.marginIndex(future, currentPrice);
    double referenceIndex = productPricer.marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Overnight rate future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is calculated using the discounting model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedOvernightFutureTrade trade,
      RatesProvider ratesProvider,
      double lastSettlementPrice) {

    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), lastSettlementPrice);
    double price = price(trade, ratesProvider);
    return presentValue(trade, price, referencePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the Overnight rate future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(ResolvedOvernightFutureTrade trade, RatesProvider ratesProvider) {
    ResolvedOvernightFuture product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, ratesProvider);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the Overnight rate future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * The current price is calculated using the discounting model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the par spread.
   */
  public double parSpread(ResolvedOvernightFutureTrade trade, RatesProvider ratesProvider, double lastSettlementPrice) {
    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), lastSettlementPrice);
    return price(trade, ratesProvider) - referencePrice;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread sensitivity of the Overnight rate future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivity(ResolvedOvernightFutureTrade trade, RatesProvider ratesProvider) {
    return productPricer.priceSensitivity(trade.getProduct(), ratesProvider);
  }

}
