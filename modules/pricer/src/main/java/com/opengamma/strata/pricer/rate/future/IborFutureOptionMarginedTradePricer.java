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
import com.opengamma.strata.finance.common.FutureOptionPremiumStyle;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.finance.rate.future.IborFutureOptionTrade;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for Ibor future option trades.
 * <p>
 * This function provides the ability to price an {@link IborFutureOptionTrade} with {@link FutureOptionPremiumStyle}
 * DAILY_MARGIN.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public abstract class IborFutureOptionMarginedTradePricer {
  
  /**
   * Returns the {@link IborFutureOptionMarginedProductPricer} used for the computation related to the future option 
   * underlying the trade.
   * @return  the future option product pricer
   */
  public abstract IborFutureOptionMarginedProductPricer getFutureOptionProductPricerFn();

  /**
   * Calculates the price of the Ibor future option trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * @param trade  the trade to price
   * @param provider  the pricing environment
   * @param parameters  the model parameters
   * 
   * @return the price of the product, in decimal form
   */
  public double price(IborFutureOptionTrade trade, RatesProvider provider, IborFutureParameters parameters) {
    return getFutureOptionProductPricerFn().price(trade.getSecurity().getProduct(), provider, parameters);
  }

  /**
   * Calculates the present value of the Ibor future option trade from the current option price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * @param trade  the trade to price
   * @param currentOptionPrice  the option price on the valuation date
   * @param lastClosingPrice  the last closing price
   * 
   * @return the present value
   */
  public CurrencyAmount presentValue(IborFutureOptionTrade trade, LocalDate valuationDate, double currentOptionPrice, 
      double lastClosingPrice){
    IborFutureOption option = trade.getSecurity().getProduct();
    Optional<LocalDate> tradeDateOpt = trade.getTradeInfo().getTradeDate();
    ArgChecker.isTrue(tradeDateOpt.isPresent(), "trade date not present");
    double priceIndex = getFutureOptionProductPricerFn().marginIndex(option, currentOptionPrice);
    double marginReferencePrice = lastClosingPrice;
    LocalDate tradeDate = tradeDateOpt.get();
    if(tradeDate.equals(valuationDate)) {
      OptionalDouble tradePrice = trade.getInitialPrice();
      ArgChecker.isTrue(tradePrice.isPresent(), "trade price not present");
      marginReferencePrice = tradePrice.getAsDouble();
    }
    double referenceIndex = getFutureOptionProductPricerFn().marginIndex(option, marginReferencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(option.getUnderlying().getProduct().getCurrency(), pv);
  }

  /**
   * Calculates the present value of the Ibor future option trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * @param trade  the trade to price
   * @param provider  the pricing environment
   * @param parameters  the model parameters
   * @param lastClosingPrice  the last closing price
   * 
   * @return the present value
   */
  public CurrencyAmount presentValue(IborFutureOptionTrade trade, RatesProvider provider, 
      IborFutureParameters parameters, double lastClosingPrice) {
    double price = price(trade, provider, parameters);
    return presentValue(trade, provider.getValuationDate(), price, lastClosingPrice);
  }  
  
  /**
   * Calculates the present value sensitivity of the Ibor future option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * @param trade  the trade to price
   * @param provider  the pricing environment
   * 
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(IborFutureOptionTrade trade, RatesProvider provider, 
      IborFutureParameters parameters) {
    IborFutureOption product = trade.getSecurity().getProduct();
    PointSensitivities priceSensi = getFutureOptionProductPricerFn().priceSensitivity(product, provider, parameters);
    PointSensitivities marginIndexSensi = getFutureOptionProductPricerFn().marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }
  
}