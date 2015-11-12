/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;
import com.opengamma.strata.product.rate.future.IborFutureOption;
import com.opengamma.strata.product.rate.future.IborFutureOptionTrade;

/**
 * Pricer for Ibor future option trades with daily margin.
 * <p>
 * This function provides the ability to price an {@link IborFutureOptionTrade}.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public abstract class IborFutureOptionMarginedTradePricer {

  /**
   * Creates an instance.
   */
  protected IborFutureOptionMarginedTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract IborFutureOptionMarginedProductPricer getProductPricer();

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future option trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the price of the product, in decimal form
   */
  public double price(
      IborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureProvider futureProvider) {

    return getProductPricer().price(trade.getSecurity().getProduct(), ratesProvider, futureProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor future option trade from the current option price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade to price
   * @param valuationDate  the valuation date; required to asses if the trade or last closing price should be used
   * @param currentOptionPrice  the option price on the valuation date
   * @param lastClosingPrice  the last closing price
   * @return the present value
   */
  public CurrencyAmount presentValue(
      IborFutureOptionTrade trade,
      LocalDate valuationDate,
      double currentOptionPrice,
      double lastClosingPrice) {

    IborFutureOption option = trade.getProduct();
    Optional<LocalDate> tradeDateOpt = trade.getTradeInfo().getTradeDate();
    ArgChecker.isTrue(tradeDateOpt.isPresent(), "trade date not present");
    double priceIndex = getProductPricer().marginIndex(option, currentOptionPrice);
    double marginReferencePrice = lastClosingPrice;
    LocalDate tradeDate = tradeDateOpt.get();
    if (tradeDate.equals(valuationDate)) {
      OptionalDouble tradePrice = trade.getInitialPrice();
      ArgChecker.isTrue(tradePrice.isPresent(), "trade price not present");
      marginReferencePrice = tradePrice.getAsDouble();
    }
    double referenceIndex = getProductPricer().marginIndex(option, marginReferencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(option.getUnderlying().getCurrency(), pv);
  }

  /**
   * Calculates the present value of the Ibor future option trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @param lastClosingPrice  the last closing price
   * @return the present value
   */
  public CurrencyAmount presentValue(
      IborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureProvider futureProvider,
      double lastClosingPrice) {

    double price = price(trade, ratesProvider, futureProvider);
    return presentValue(trade, ratesProvider.getValuationDate(), price, lastClosingPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the Ibor future option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      IborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      IborFutureProvider futureProvider) {

    IborFutureOption product = trade.getProduct();
    PointSensitivities priceSensi = getProductPricer().priceSensitivity(product, ratesProvider, futureProvider);
    PointSensitivities marginIndexSensi = getProductPricer().marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

}
