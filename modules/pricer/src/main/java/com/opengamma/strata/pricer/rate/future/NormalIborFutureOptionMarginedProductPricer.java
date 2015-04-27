/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.common.FutureOptionPremiumStyle;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.IborFutureOptionSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer of options on Ibor future with a normal model on the underlying future price.
 */
public class NormalIborFutureOptionMarginedProductPricer extends IborFutureOptionMarginedProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalIborFutureOptionMarginedProductPricer DEFAULT =
      new NormalIborFutureOptionMarginedProductPricer(DiscountingIborFutureProductPricer.DEFAULT);

  /**
   * Normal or Bachelier price function.
   */
  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();

  /**
   * The underlying future pricer.
   * The pricer take only the curves as inputs, no model parameters.
   */
  private final DiscountingIborFutureProductPricer futurePricer;

  /**
   * Creates an instance.
   * 
   * @param futurePricer  the pricer for {@link IborFutureOption}
   */
  public NormalIborFutureOptionMarginedProductPricer(
      DiscountingIborFutureProductPricer futurePricer) {
    this.futurePricer = ArgChecker.notNull(futurePricer, "futurePricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the underlying future pricer function.
   * 
   * @return the future pricer
   */
  DiscountingIborFutureProductPricer getFuturePricer() {
    return futurePricer;
  }

  @Override
  public double price(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      IborFutureProvider volatilityProvider) {

    ArgChecker.isTrue(volatilityProvider instanceof NormalVolatilityIborFutureProvider,
        "Provider must be of type NormalVolatilityIborFutureProvider");

    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, ratesProvider);
    return price(futureOption, ratesProvider, (NormalVolatilityIborFutureProvider) volatilityProvider, futurePrice);
  }

  /**
   * Calculates the price of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The price of the option is the price on the valuation date.
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @param futurePrice  the price of the underlying future
   * @return the price of the product, in decimal form
   */
  public double price(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlying().getProduct().getIndex().equals(volatilityProvider.getFutureIndex()),
        "Future index should be the same as data index");

    EuropeanVanillaOption option = createOption(futureOption, volatilityProvider);
    double strike = futureOption.getStrikePrice();
    IborFuture future = futureOption.getUnderlying().getProduct();
    double volatility = volatilityProvider.getVolatility(
        futureOption.getExpirationDate(), future.getLastTradeDate(), strike, futurePrice);

    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, volatility);
    return NORMAL_FUNCTION.getPriceFunction(option).evaluate(normalPoint);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the delta of the Ibor future option product.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider) {

    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, ratesProvider);
    return deltaStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the delta of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");

    EuropeanVanillaOption option = createOption(futureOption, volatilityProvider);
    double strike = futureOption.getStrikePrice();
    IborFuture future = futureOption.getUnderlying().getProduct();
    double volatility = volatilityProvider.getVolatility(futureOption.getExpirationDate(),
        future.getLastTradeDate(), strike, futurePrice);

    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, volatility);
    return NORMAL_FUNCTION.getDelta(option, normalPoint);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name. 
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider,
      double futurePrice) {

    double delta = deltaStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
    PointSensitivities futurePriceSensitivity =
        futurePricer.priceSensitivity(futureOption.getUnderlying().getProduct(), ratesProvider);
    return futurePriceSensitivity.multipliedBy(delta);
  }

  /**
   * Calculates the price sensitivity of the Ibor future option product based on curves.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");

    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, ratesProvider);
    return priceSensitivityStickyStrike(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  @Override
  public PointSensitivities priceSensitivity(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      IborFutureProvider volatilityProvider) {

    ArgChecker.isTrue(volatilityProvider instanceof NormalVolatilityIborFutureProvider,
        "Provider must be of type NormalVolatilityIborFutureProvider");

    return priceSensitivityStickyStrike(
        futureOption, ratesProvider, (NormalVolatilityIborFutureProvider) volatilityProvider);
  }

  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Ibor future option.
   * <p>
   * This sensitivity is also called the price normal vega.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @return the sensitivity
   */
  public IborFutureOptionSensitivity priceSensitivityNormalVolatility(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider) {

    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, ratesProvider);
    return priceSensitivityNormalVolatility(futureOption, ratesProvider, volatilityProvider, futurePrice);
  }

  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Ibor future option.
   * <p>
   * This sensitivity is also called the price normal vega.
   * 
   * @param futureOption  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the provider of normal volatility
   * @param futurePrice  the underlying future price
   * @return the sensitivity
   */
  public IborFutureOptionSensitivity priceSensitivityNormalVolatility(
      IborFutureOption futureOption,
      RatesProvider ratesProvider,
      NormalVolatilityIborFutureProvider volatilityProvider,
      double futurePrice) {

    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN),
        "Premium style should be DAILY_MARGIN");

    EuropeanVanillaOption option = createOption(futureOption, volatilityProvider);
    double strike = futureOption.getStrikePrice();
    IborFuture future = futureOption.getUnderlying().getProduct();
    double volatility = volatilityProvider.getVolatility(futureOption.getExpirationDate(),
        future.getLastTradeDate(), strike, futurePrice);

    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, volatility);
    return IborFutureOptionSensitivity.of(future.getIndex(), futureOption.getExpirationDate(),
        future.getLastTradeDate(), strike, futurePrice, NORMAL_FUNCTION.getVega(option, normalPoint));
  }

  //-------------------------------------------------------------------------
  // create analytic option object
  private EuropeanVanillaOption createOption(
      IborFutureOption futureOption,
      NormalVolatilityIborFutureProvider volatilityProvider) {

    double strike = futureOption.getStrikePrice();
    double timeToExpiry = volatilityProvider.relativeTime(
        futureOption.getExpirationDate(), futureOption.getExpirationTime(), futureOption.getExpirationZone());
    boolean isCall = futureOption.getPutCall().isCall();
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

}
