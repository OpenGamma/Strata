/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.IborFutureOptionSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Pricer implementation for Ibor future option.
 * <p>
 * The Ibor future option is priced based on normal model.
 */
public final class NormalIborFutureOptionMarginedTradePricer extends IborFutureOptionMarginedTradePricer {

  /**
   * Default implementation.
   */
  public static final NormalIborFutureOptionMarginedTradePricer DEFAULT =
      new NormalIborFutureOptionMarginedTradePricer(NormalIborFutureOptionMarginedProductPricer.DEFAULT);

  /**
   * Underlying option pricer.
   */
  private final NormalIborFutureOptionMarginedProductPricer futureOptionPricer;

  /**
   * Creates an instance.
   * 
   * @param futureOptionPricer  the pricer for {@link IborFutureOption}
   */
  public NormalIborFutureOptionMarginedTradePricer(
      NormalIborFutureOptionMarginedProductPricer futureOptionPricer) {
    this.futureOptionPricer = ArgChecker.notNull(futureOptionPricer, "futureOptionPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  public NormalIborFutureOptionMarginedProductPricer getProductPricer() {
    return futureOptionPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor future option trade from the underlying future price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @param futurePrice  the price of the underlying future
   * @param lastClosingPrice  the last closing price
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider,
      double futurePrice,
      double lastClosingPrice) {

    double optionPrice = getProductPricer().price(trade.getProduct(), ratesProvider, volatilityProvider, futurePrice);
    return presentValue(trade, ratesProvider.getValuationDate(), optionPrice, lastClosingPrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the normal volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * The volatility is associated with the expiry/delay/strike/future price key combination.
   * <p>
   * This calculates the underlying future price using the future pricer.
   * 
   * @param futureOptionTrade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @return the price sensitivity
   */
  public IborFutureOptionSensitivity presentValueSensitivityNormalVolatility(
      ResolvedIborFutureOptionTrade futureOptionTrade,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider) {

    ResolvedIborFuture future = futureOptionTrade.getProduct().getUnderlyingFuture();
    double futurePrice = futureOptionPricer.getFuturePricer().price(future, ratesProvider);
    return presentValueSensitivityNormalVolatility(futureOptionTrade, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Computes the present value sensitivity to the normal volatility used in the pricing
   * based on the price of the underlying future.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * The volatility is associated with the expiry/delay/strike/future price key combination.
   * 
   * @param futureOptionTrade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @param futurePrice  the price of the underlying future
   * @return the price sensitivity
   */
  public IborFutureOptionSensitivity presentValueSensitivityNormalVolatility(
      ResolvedIborFutureOptionTrade futureOptionTrade,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider,
      double futurePrice) {

    ResolvedIborFutureOption product = futureOptionTrade.getProduct();
    IborFutureOptionSensitivity priceSensitivity =
        futureOptionPricer.priceSensitivityNormalVolatility(product, ratesProvider, volatilityProvider, futurePrice);
    double factor = futureOptionPricer.marginIndex(product, 1) * futureOptionTrade.getQuantity();
    return priceSensitivity.withSensitivity(priceSensitivity.getSensitivity() * factor);
  }

}
