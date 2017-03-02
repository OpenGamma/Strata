/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.dsf;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.dsf.ResolvedDsf;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;

/**
 * Pricer implementation for Deliverable Swap Futures (DSFs).
 * <p>
 * This function provides the ability to price a {@link ResolvedDsfTrade}.
 * 
 * <h4>Price</h4>
 * The price of a DSF is based on the present value (NPV) of the underlying swap on the delivery date.
 * For example, a price of 100.182 represents a present value of $100,182.00, if the notional is $100,000.
 * This price can also be viewed as a percentage present value - {@code (100 + percentPv)}, or 0.182% in this example.
 * <p>
 * Strata uses <i>decimal prices</i> for DSFs in the trade model, pricers and market data.
 * The decimal price is based on the decimal multiplier equivalent to the implied percentage.
 * Thus the market price of 100.182 is represented in Strata by 1.00182.
 */
public class DiscountingDsfTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingDsfTradePricer DEFAULT =
      new DiscountingDsfTradePricer(DiscountingDsfProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DiscountingDsfProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link Dsf}
   */
  public DiscountingDsfTradePricer(
      DiscountingDsfProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the underlying deliverable swap futures product.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the price of the trade, in decimal form
   */
  public double price(ResolvedDsfTrade trade, RatesProvider ratesProvider) {
    return productPricer.price(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the deliverable swap futures product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the price curve sensitivity of the trade
   */
  public PointSensitivities priceSensitivity(ResolvedDsfTrade trade, RatesProvider ratesProvider) {
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
  private double referencePrice(ResolvedDsfTrade trade, LocalDate valuationDate, double lastSettlementPrice) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    if (trade.getInfo().getTradeDate().isPresent()) {
      return (trade.getInfo().getTradeDate().get().equals(valuationDate) ? trade.getPrice() : lastSettlementPrice);
    }
    return lastSettlementPrice;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the deliverable swap futures trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The current price is specified, not calculated.
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
  private CurrencyAmount presentValue(
      ResolvedDsfTrade trade,
      double currentPrice,
      double referencePrice) {

    ResolvedDsf future = trade.getProduct();
    double priceIndex = productPricer.marginIndex(future, currentPrice);
    double referenceIndex = productPricer.marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the deliverable swap futures trade.
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
      ResolvedDsfTrade trade,
      RatesProvider ratesProvider,
      double lastSettlementPrice) {

    double price = price(trade, ratesProvider);
    double referencePrice = referencePrice(trade, ratesProvider.getValuationDate(), lastSettlementPrice);
    return presentValue(trade, price, referencePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the deliverable swap futures trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(ResolvedDsfTrade trade, RatesProvider ratesProvider) {
    ResolvedDsf product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, ratesProvider);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the deliverable swap futures trade.
   * <p>
   * Since the deliverable swap futures is based on a single currency, the trade is exposed to only this currency.
   * The current price is calculated using the discounting model.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedDsfTrade trade,
      RatesProvider ratesProvider,
      double lastSettlementPrice) {

    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider, lastSettlementPrice));
  }

}
