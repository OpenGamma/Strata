/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Pricer implementation for Ibor future option.
 * <p>
 * This provides the ability to price an Ibor future option.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * 
 * <h4>Price</h4>
 * The price of an Ibor future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
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
   * The current price is calculated using the volatility model with a known future price.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @param lastOptionSettlementPrice  the last settlement price used for margining for the option, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedIborFutureOptionTrade trade,
      RatesProvider ratesProvider,
      NormalIborFutureOptionVolatilities volatilities,
      double futurePrice,
      double lastOptionSettlementPrice) {

    double optionPrice = getProductPricer().price(trade.getProduct(), ratesProvider, volatilities, futurePrice);
    return presentValue(trade, ratesProvider.getValuationDate(), optionPrice, lastOptionSettlementPrice);
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
   * @param volatilities  the volatilities
   * @return the price sensitivity
   */
  public IborFutureOptionSensitivity presentValueSensitivityNormalVolatility(
      ResolvedIborFutureOptionTrade futureOptionTrade,
      RatesProvider ratesProvider,
      NormalIborFutureOptionVolatilities volatilities) {

    ResolvedIborFuture future = futureOptionTrade.getProduct().getUnderlyingFuture();
    double futurePrice = futureOptionPricer.getFuturePricer().price(future, ratesProvider);
    return presentValueSensitivityNormalVolatility(futureOptionTrade, ratesProvider, volatilities, futurePrice);
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
   * @param volatilities  the volatilities
   * @param futurePrice  the price of the underlying future, in decimal form
   * @return the price sensitivity
   */
  public IborFutureOptionSensitivity presentValueSensitivityNormalVolatility(
      ResolvedIborFutureOptionTrade futureOptionTrade,
      RatesProvider ratesProvider,
      NormalIborFutureOptionVolatilities volatilities,
      double futurePrice) {

    ResolvedIborFutureOption product = futureOptionTrade.getProduct();
    IborFutureOptionSensitivity priceSensitivity =
        futureOptionPricer.priceSensitivityNormalVolatility(product, ratesProvider, volatilities, futurePrice);
    double factor = futureOptionPricer.marginIndex(product, 1) * futureOptionTrade.getQuantity();
    return priceSensitivity.multipliedBy(factor);
  }

}
