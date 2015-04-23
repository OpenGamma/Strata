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
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.option.IborFutureOptionSensitivityKey;
import com.opengamma.strata.pricer.sensitivity.option.OptionPointSensitivity;

public class NormalIborFutureOptionMarginedProductPricer extends IborFutureOptionMarginedProductPricer{

  /**
   * Normal or Bachelier price function.
   */
  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  
  /** Underlying future pricer. The pricer take only the curves as inputs, no model parameters. */
  private final DiscountingIborFutureProductPricer futurePricerFn;

  /**
   * Constructor.
   * @param futurePricerFn  the underlying future pricer function
   */
  public NormalIborFutureOptionMarginedProductPricer(DiscountingIborFutureProductPricer futurePricerFn) {
    this.futurePricerFn = ArgChecker.notNull(futurePricerFn, "futurePricerFn");
  }
  
  /**
   * Returns the underlying future pricer function.
   * @return the future pricer
   */
  public DiscountingIborFutureProductPricer getFuturePricerFn() {
    return futurePricerFn;
  }

  @Override
  public double price(IborFutureOption futureOption, RatesProvider prov,
      IborFutureParameters parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricerFn.price(future, prov);
    ArgChecker.isTrue(parameters instanceof NormalVolatilityIborFutureParameters, 
        "parameters should be of type NormalIborFutureVolatilityParameters");
    return price(futureOption, prov, (NormalVolatilityIborFutureParameters) parameters, futurePrice);
  }

  /**
   * Calculates the price of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The price of the option is the price on the valuation date.
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * 
   * @return the price of the product, in decimal form
   */
  public double price(IborFutureOption futureOption, RatesProvider prov, 
      NormalVolatilityIborFutureParameters parameters, double futurePrice) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    ArgChecker.isTrue(futureOption.getUnderlying().getProduct().getIndex().equals(parameters.getFutureIndex()), 
        "future index should be the same as data index");
    EuropeanVanillaOption option = createOption(futureOption, parameters);
    NormalFunctionData normalPoint = createData(createKey(futureOption, futurePrice, parameters), parameters);
    return NORMAL_FUNCTION.getPriceFunction(option).evaluate(normalPoint);
  }
  
  /**
   * Calculates the delta of the Ibor future option product.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters  the model parameters
   * 
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(IborFutureOption futureOption, RatesProvider prov, 
      NormalVolatilityIborFutureParameters parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricerFn.price(future, prov);
    return deltaStickyStrike(futureOption, prov, parameters, futurePrice);
  }

  /**
   * Calculates the delta of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The delta of the product is the sensitivity of the option price to the future price.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name.
   * 
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * 
   * @return the price curve sensitivity of the product
   */
  public double deltaStickyStrike(IborFutureOption futureOption, RatesProvider prov, 
      NormalVolatilityIborFutureParameters parameters, double futurePrice) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    // Forward sweep
    EuropeanVanillaOption option = createOption(futureOption, parameters);
    NormalFunctionData normalPoint = createData(createKey(futureOption, futurePrice, parameters), parameters);
    // Backward sweep
    double[] priceAdjoint = new double[3];
    NORMAL_FUNCTION.getPriceAdjoint(option, normalPoint, priceAdjoint);
    return priceAdjoint[0];
  }

  /**
   * Calculates the price sensitivity of the Ibor future option product based on the price of the underlying future.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name. 
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters  the model parameters
   * @param futurePrice  the price of the underlying future
   * 
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(IborFutureOption futureOption, RatesProvider prov, 
      NormalVolatilityIborFutureParameters parameters, double futurePrice) {
    double delta = deltaStickyStrike(futureOption, prov, parameters, futurePrice);
    PointSensitivities futurePriceSensitivity = 
        futurePricerFn.priceSensitivity(futureOption.getUnderlying().getProduct(), prov);
    return futurePriceSensitivity.multipliedBy(delta);
  }

  /**
   * Calculates the price sensitivity of the Ibor future option product based on curves.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * The volatility is unchanged for a fixed strike in the sensitivity computation, hence the "StickyStrike" name. 
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters  the model parameters
   * 
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityStickyStrike(IborFutureOption futureOption, RatesProvider prov, 
      NormalVolatilityIborFutureParameters parameters) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricerFn.price(future, prov);
    return priceSensitivityStickyStrike(futureOption, prov, (NormalVolatilityIborFutureParameters) parameters, futurePrice);
  }

  @Override
  public PointSensitivities priceSensitivity(IborFutureOption futureOption, RatesProvider prov, 
      IborFutureParameters parameters) {
    ArgChecker.isTrue(parameters instanceof NormalVolatilityIborFutureParameters, 
        "parameters should be of type NormalIborFutureVolatilityParameters");
    return priceSensitivityStickyStrike(futureOption, prov, (NormalVolatilityIborFutureParameters) parameters);
  }
  
  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Ibor future option.
   * <p>
   * This sensitivity is also called the price normal vega.
   * 
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters the normal volatility parameters
   * 
   * @return the sensitivity
   */
  public OptionPointSensitivity priceSensitivityNormalVolatility(IborFutureOption futureOption, 
      RatesProvider prov, NormalVolatilityIborFutureParameters parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double futurePrice = futurePricerFn.price(future, prov);
    return priceSensitivityNormalVolatility(futureOption, prov, parameters, futurePrice);
  }
  
  /**
   * Calculates the price sensitivity to the normal volatility used for the pricing of the Ibor future option.
   * <p>
   * This sensitivity is also called the price normal vega.
   * 
   * @param futureOption  the option product to price
   * @param prov  the pricing environment
   * @param parameters the normal volatility parameters
   * @param futurePrice  the underlying future price
   * 
   * @return the sensitivity
   */
  public OptionPointSensitivity priceSensitivityNormalVolatility(IborFutureOption futureOption, 
      RatesProvider prov, NormalVolatilityIborFutureParameters parameters, double futurePrice) {
    ArgChecker.isTrue(futureOption.getPremiumStyle().equals(FutureOptionPremiumStyle.DAILY_MARGIN), 
        "premium style should be DAILY_MARGIN");
    // Forward sweep
    NormalVolatilityIborFutureParameters normalParameters = (NormalVolatilityIborFutureParameters) parameters;
    EuropeanVanillaOption option = createOption(futureOption, parameters);
    IborFutureOptionSensitivityKey key = createKey(futureOption, futurePrice, parameters);
    NormalFunctionData normalPoint = createData(key, normalParameters);
    // Backward sweep
    double[] priceAdjoint = new double[3];
    NORMAL_FUNCTION.getPriceAdjoint(option, normalPoint, priceAdjoint);
    return new OptionPointSensitivity(key, priceAdjoint[1], futureOption.getUnderlying().getProduct().getCurrency());
  }

  //-------------------------------------------------------------------------
  // create analytic option object
  private EuropeanVanillaOption createOption(IborFutureOption futureOption, 
      NormalVolatilityIborFutureParameters parameters) {
    double strike = futureOption.getStrikePrice();
    double timeToExpiry = parameters.relativeTime(futureOption.getExpirationDate(), futureOption.getExpirationTime(), 
        futureOption.getExpirationZone());
    boolean isCall = futureOption.getPutCall().isCall();
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }
  
  // create the normal data object from future price
  private NormalFunctionData createData(IborFutureOptionSensitivityKey key,
      NormalVolatilityIborFutureParameters parameters) {
    double volatility = parameters.getVolatility(key);
    return new NormalFunctionData(key.getFuturePrice(), 1.0, volatility);    
  }
  
  // create the normal volatility key
  private IborFutureOptionSensitivityKey createKey(IborFutureOption futureOption, 
      double futurePrice, NormalVolatilityIborFutureParameters parameters) {
    IborFuture future = futureOption.getUnderlying().getProduct();
    double strike = futureOption.getStrikePrice();
    return new IborFutureOptionSensitivityKey(future.getIndex(), futureOption.getExpirationDate(), 
        future.getLastTradeDate(), strike, futurePrice);    
  }

}
