/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.finance.rate.future.IborFutureOptionTrade;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.IborFutureOptionSensitivity;

/**
 * Pricer implementation for Ibor future option.
 * <p>
 * The Ibor future option is priced based on normal model.
 */
public final class NormalIborFutureOptionMarginedTradePricer extends IborFutureOptionMarginedTradePricer{

  /**
   * Default implementation.
   */
  public static final NormalIborFutureOptionMarginedTradePricer DEFAULT =
      new NormalIborFutureOptionMarginedTradePricer(NormalIborFutureOptionMarginedProductPricer.DEFAULT);

  /**
   * Underlying option pricer.
   */
  private final NormalIborFutureOptionMarginedProductPricer futureOptionPricerFn;

  /**
   * Creates an instance.
   * 
   * @param futureOptionPricerFn  the pricer for {@link IborFutureTrade}
   */
  public NormalIborFutureOptionMarginedTradePricer(
      NormalIborFutureOptionMarginedProductPricer futureOptionPricerFn) {
    this.futureOptionPricerFn = ArgChecker.notNull(futureOptionPricerFn, "futurePricerFn");
  }

  @Override
  public NormalIborFutureOptionMarginedProductPricer getFutureOptionProductPricerFn() {
    return futureOptionPricerFn;
  }  
  
  /**
   * Calculates the present value of the Ibor future option trade from the underlying future price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * @param trade  the trade to price
   * @param provider  the pricing environment
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * @param lastClosingPrice  the last closing price
   * 
   * @return the present value
   */
  public CurrencyAmount presentValue(IborFutureOptionTrade trade, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters, double futurePrice, double lastClosingPrice) {
    double optionPrice = getFutureOptionProductPricerFn().price(trade.getSecurity().getProduct(), provider, 
        parameters, futurePrice);
    return presentValue(trade, provider.getValuationDate(), optionPrice, lastClosingPrice);
  }

  /**
   * Computes the present value sensitivity to the normal volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used. The volatility is the one associated to a 
   * expiry/delay/strike/future price key combination.
   * @param futureOptionTrade  the trade to price
   * @param provider  the pricing environment
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * 
   * @return the price sensitivity
   */
  public IborFutureOptionSensitivity presentValueSensitivityNormalVolatility(
      IborFutureOptionTrade futureOptionTrade, RatesProvider provider, NormalVolatilityIborFutureProvider parameters, 
      double futurePrice) {
    IborFutureOption product = futureOptionTrade.getSecurity().getProduct();
    IborFutureOptionSensitivity priceSensitivity =
        futureOptionPricerFn.priceSensitivityNormalVolatility(product, provider, parameters, futurePrice);
    double factor = futureOptionPricerFn.marginIndex(product, 1) * futureOptionTrade.getQuantity();
    return priceSensitivity.withSensitivity(priceSensitivity.getSensitivity() * factor);
  }


  /**
   * Computes the present value sensitivity to the normal volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used. The volatility is the one associated to a 
   * expiry/delay/strike/future price key combination.
   * The underlying future price is computed from the PricingEnvoronment using the underlying future pricer.
   * @param futureOptionTrade  the trade to price
   * @param provider  the pricing environment
   * @param parameters  the model parameters
   * 
   * @return the price sensitivity
   */
  public IborFutureOptionSensitivity presentValueSensitivityNormalVolatility(
      IborFutureOptionTrade futureOptionTrade, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters) {
    IborFuture future = futureOptionTrade.getSecurity().getProduct().getUnderlying().getProduct();
    double futurePrice = futureOptionPricerFn.getFuturePricerFn().price(future, provider);
    return presentValueSensitivityNormalVolatility(futureOptionTrade, provider, parameters, futurePrice);
  }

}