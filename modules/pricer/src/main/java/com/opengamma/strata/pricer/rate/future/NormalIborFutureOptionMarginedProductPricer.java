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
public class NormalIborFutureOptionMarginedProductPricer extends IborFutureOptionMarginedProductPricer{

  /**
   * Default implementation.
   */
  public static final NormalIborFutureOptionMarginedProductPricer DEFAULT =
      new NormalIborFutureOptionMarginedProductPricer(DiscountingIborFutureProductPricer.DEFAULT);

  /**
   * Normal or Bachelier price function.
   */
  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  
  /** Underlying future pricer. The pricer take only the curves as inputs, no model parameters. */
  private final DiscountingIborFutureProductPricer futurePricer;

  /**
   * Constructor.
   * @param futurePricer  the underlying future pricer function
   */
  public NormalIborFutureOptionMarginedProductPricer(DiscountingIborFutureProductPricer futurePricer) {
    this.futurePricer = ArgChecker.notNull(futurePricer, "future pricer");
  }
  
  /**
   * Returns the underlying future pricer function.
   * @return the future pricer
   */
  public DiscountingIborFutureProductPricer getFuturePricerFn() {
    return futurePricer;
  }

  @Override
  public double price(IborFutureOption futureOption, RatesProvider provider,
      IborFutureParameters parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, provider);
    ArgChecker.isTrue(parameters instanceof NormalVolatilityIborFutureProvider, 
        "parameters should be of type NormalIborFutureVolatilityParameters");
    return price(futureOption, provider, (NormalVolatilityIborFutureProvider) parameters, futurePrice);
  }

  /**
   * Calculates the price of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The price of the option is the price on the valuation date.
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * @return the price of the product, in decimal form
   */
  public double price(IborFutureOption futureOption, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters, double futurePrice) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlying().getProduct().getIndex().equals(parameters.getFutureIndex()), 
        "future index should be the same as data index");
    EuropeanVanillaOption option = createOption(futureOption, parameters);
    double strike = futureOption.getStrikePrice();
    IborFuture future = futureOption.getUnderlying().getProduct();
    double volatility = parameters.getVolatility(futureOption.getExpirationDate(), 
        future.getLastTradeDate(), strike, futurePrice);
    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, volatility);
    return NORMAL_FUNCTION.getPriceFunction(option).evaluate(normalPoint);
  }
  
  /**
   * Calculates the delta of the Ibor future option product.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters  the model parameters
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(IborFutureOption futureOption, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, provider);
    return deltaStickyStrike(futureOption, provider, parameters, futurePrice);
  }

  /**
   * Calculates the delta of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(IborFutureOption futureOption, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters, double futurePrice) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    EuropeanVanillaOption option = createOption(futureOption, parameters);
    double strike = futureOption.getStrikePrice();
    IborFuture future = futureOption.getUnderlying().getProduct();
    double volatility = parameters.getVolatility(futureOption.getExpirationDate(), 
        future.getLastTradeDate(), strike, futurePrice);
    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, volatility);
    return  NORMAL_FUNCTION.getDelta(option, normalPoint);
  }

  /**
   * Calculates the price sensitivity of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name. 
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(IborFutureOption futureOption, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters, double futurePrice) {
    double delta = deltaStickyStrike(futureOption, provider, parameters, futurePrice);
    PointSensitivities futurePriceSensitivity = 
        futurePricer.priceSensitivity(futureOption.getUnderlying().getProduct(), provider);
    return futurePriceSensitivity.multipliedBy(delta);
  }

  /**
   * Calculates the price sensitivity of the Ibor future option product based on curves.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name. 
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters  the model parameters
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(IborFutureOption futureOption, RatesProvider provider, 
      NormalVolatilityIborFutureProvider parameters) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, provider);
    return priceSensitivityStickyStrike(futureOption, provider, (NormalVolatilityIborFutureProvider) parameters, futurePrice);
  }

  @Override
  public PointSensitivities priceSensitivity(IborFutureOption futureOption, RatesProvider provider, 
      IborFutureParameters parameters) {
    ArgChecker.isTrue(parameters instanceof NormalVolatilityIborFutureProvider, 
        "parameters should be of type NormalIborFutureVolatilityParameters");
    return priceSensitivityStickyStrike(futureOption, provider, (NormalVolatilityIborFutureProvider) parameters);
  }
  
  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Ibor future option.
   * <p>
   * This sensitivity is also called the price normal vega.
   * 
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters the normal volatility parameters
   * @return the sensitivity
   */
  public IborFutureOptionSensitivity priceSensitivityNormalVolatility(IborFutureOption futureOption, 
      RatesProvider provider, NormalVolatilityIborFutureProvider parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricer.price(future, provider);
    return priceSensitivityNormalVolatility(futureOption, provider, parameters, futurePrice);
  }
  
  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Ibor future option.
   * <p>
   * This sensitivity is also called the price normal vega.
   * 
   * @param futureOption  the option product to price
   * @param provider  the rates provider
   * @param parameters the normal volatility parameters
   * @param futurePrice  the underlying future price
   * @return the sensitivity
   */
  public IborFutureOptionSensitivity priceSensitivityNormalVolatility(IborFutureOption futureOption, 
      RatesProvider provider, NormalVolatilityIborFutureProvider parameters, double futurePrice) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    EuropeanVanillaOption option = createOption(futureOption, parameters);
    double strike = futureOption.getStrikePrice();
    IborFuture future = futureOption.getUnderlying().getProduct();
    double volatility = parameters.getVolatility(futureOption.getExpirationDate(), 
        future.getLastTradeDate(), strike, futurePrice);
    NormalFunctionData normalPoint = new NormalFunctionData(futurePrice, 1.0, volatility);
    return IborFutureOptionSensitivity.of(future.getIndex(), futureOption.getExpirationDate(), 
        future.getLastTradeDate(), strike, futurePrice, NORMAL_FUNCTION.getVega(option, normalPoint));
  }

  //-------------------------------------------------------------------------
  // create analytic option object
  private EuropeanVanillaOption createOption(IborFutureOption futureOption, 
      NormalVolatilityIborFutureProvider parameters) {
    double strike = futureOption.getStrikePrice();
    double timeToExpiry = parameters.relativeTime(futureOption.getExpirationDate(), futureOption.getExpirationTime(), 
        futureOption.getExpirationZone());
    boolean isCall = futureOption.getPutCall().isCall();
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

}
