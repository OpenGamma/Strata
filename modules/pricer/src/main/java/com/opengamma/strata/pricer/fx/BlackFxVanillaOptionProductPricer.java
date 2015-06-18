/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.analytics.financial.model.volatility.BlackFormulaRepository;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.fx.FxProduct;
import com.opengamma.strata.finance.fx.FxVanillaOption;
import com.opengamma.strata.finance.fx.FxVanillaOptionProduct;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange vanilla option transaction products.
 * <p>
 * This function provides the ability to price an {@link FxVanillaOptionProduct}.
 */
public class BlackFxVanillaOptionProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackFxVanillaOptionProductPricer DEFAULT =
      new BlackFxVanillaOptionProductPricer(DiscountingFxProductPricer.DEFAULT);

  /**
   * Underlying FX pricer.
   */
  private final DiscountingFxProductPricer fxPricer;

  /**
   * Creates an instance.
   * 
   * @param fxPricer  the pricer for {@link FxProduct}
   */
  public BlackFxVanillaOptionProductPricer(
      DiscountingFxProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  public DiscountingFxProductPricer getDiscountingFxProductPricer() {
    return fxPricer;
  }

  //-------------------------------------------------------------------------
  public CurrencyAmount presentValue(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double forwardPrice = BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    double discountFactor =
        ratesProvider.discountFactor(option.getPayoffCurrency(), option.getUnderlying().getPaymentDate());
    return CurrencyAmount.of(option.getPayoffCurrency(),
        (option.getLongShort().isLong() ? 1d : -1d) * option.getAmount() * discountFactor * forwardPrice);
  }

  public double impliedVol(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    return volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
  }

  public double vega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double vega = BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), option.getExpiryDate());
    return (option.getLongShort().isLong() ? 1d : -1d) * option.getAmount() * discountFactor * vega;
  }

  // TODO requires forward sensitivity to spot to compute spot-related Greeks, i.e., spotDelta, theta, etc.
  // TODO requires forward sensitivity to curve to compute parallel and bucketed pv01
  // TODO bucketed vega
}
