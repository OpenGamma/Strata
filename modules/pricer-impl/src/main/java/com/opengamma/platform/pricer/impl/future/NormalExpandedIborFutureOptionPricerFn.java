/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.future;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.analytics.financial.provider.description.interestrate.NormalSTIRFuturesProviderInterface;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.ExpandedIborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureOptionProductPricerFn;
import com.opengamma.platform.pricer.future.IborFutureProductPricerFn;

/**
 * Pricer implementation for Ibor future option.
 * <p>
 * The Ibor future option is priced based on normal model.
 */
public class NormalExpandedIborFutureOptionPricerFn
    implements IborFutureOptionProductPricerFn<ExpandedIborFutureOption> {

  /**
   * Default implementation.
   */
  public static final NormalExpandedIborFutureOptionPricerFn DEFAULT = new NormalExpandedIborFutureOptionPricerFn(
      DefaultExpandedIborFuturePricerFn.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final IborFutureProductPricerFn<ExpandedIborFuture> futurePricerFn;
  /**
   * Normal price function.
   */
  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  
  /**
   * Creates an instance.
   * 
   * @param futurePricerFn  the pricer for {@link ExpandedIborFuture}
   */
  public NormalExpandedIborFutureOptionPricerFn(
      IborFutureProductPricerFn<ExpandedIborFuture> futurePricerFn) {
    this.futurePricerFn = ArgChecker.notNull(futurePricerFn, "futurePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(
      PricingEnvironment env,
      ExpandedIborFutureOption iborFutureOption,
      Object surface) {

    EuropeanVanillaOption option = createOption(env, iborFutureOption);
    NormalFunctionData normalPoint = createData(env, iborFutureOption, surface);
    return NORMAL_FUNCTION.getPriceFunction(option).evaluate(normalPoint);
  }

  @Override
  public CurrencyAmount presentValue(
      PricingEnvironment env,
      ExpandedIborFutureOption iborFutureOption,
      IborFutureOptionSecurityTrade trade,
      double lastClosingPrice,
      Object surface) {

    double optionPrice = price(env, iborFutureOption, surface);
    double priceChange = (optionPrice - lastClosingPrice);
    double notional = iborFutureOption.getIborFuture().getNotional();
    double accrualFactor = iborFutureOption.getIborFuture().getAccrualFactor();
    double multiplier = trade.getMultiplier();
    double pv = priceChange * notional * accrualFactor * multiplier;
    Currency currency = iborFutureOption.getIborFuture().getCurrency();
    return CurrencyAmount.of(currency, pv);
  }

  //-------------------------------------------------------------------------
  // create analytic option object
  private EuropeanVanillaOption createOption(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct) {
    double strike = iborFutureOptionProduct.getStrikePrice();
    double timeToExpiry = env.relativeTime(iborFutureOptionProduct.getExpirationDate());
    boolean isCall = iborFutureOptionProduct.getPutCall().isCall();
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

  // TODO add normal sensitivity
  // public SurfaceValue priceNormalSensitivity(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct)
  // MultipleCurrencyMulticurveSensitivity presentValueCurveSensitivity

  // create the normal data object
  private NormalFunctionData createData(
      PricingEnvironment env,
      ExpandedIborFutureOption iborFutureOption,
      Object surface) {

    ExpandedIborFuture underlyingFuture = iborFutureOption.getIborFuture();
    double futurePrice = futurePricerFn.price(env, underlyingFuture);
    double timeToExpiry = env.relativeTime(iborFutureOption.getExpirationDate());
    // assuming last trade date is stored as fixing date
    double timeToLastTrade = env.relativeTime(iborFutureOption.getIborFuture().getRate().getFixingDate());
    double delay = timeToLastTrade - timeToExpiry;
    double strike = iborFutureOption.getStrikePrice();
    NormalSTIRFuturesProviderInterface normalSurface = (NormalSTIRFuturesProviderInterface) surface;
    double volatility = normalSurface.getVolatility(timeToExpiry, delay, strike, futurePrice);
    return new NormalFunctionData(futurePrice, 1.0, volatility);
  }

}
