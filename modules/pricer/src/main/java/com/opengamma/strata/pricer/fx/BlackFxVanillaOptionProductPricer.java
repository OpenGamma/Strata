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
import com.opengamma.strata.finance.fx.Fx;
import com.opengamma.strata.finance.fx.FxProduct;
import com.opengamma.strata.finance.fx.FxVanillaOption;
import com.opengamma.strata.finance.fx.FxVanillaOptionProduct;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange vanilla option transaction products with a lognormal model.
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
  /**
   * Returns the pricer used to price the underlying FX product.
   * 
   * @return the pricer
   */
  public DiscountingFxProductPricer getDiscountingFxProductPricer() {
    return fxPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the foreign exchange vanilla option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    //    Fx underlying = option.getUnderlying();
    //    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    //    FxRate strike = option.getStrike();
    //    CurrencyPair strikePair = strike.getPair();
    //    double forwardRate = forward.fxRate(strikePair);
    //    double strikeRate = strike.fxRate(strikePair);
    //    boolean isCall = option.getPutCall().isCall();
    //    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    //    double timeToExpiry =
    //        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    //    double forwardPrice = BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    //    double discountFactor =
    //        ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double price = price(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), (option.getLongShort().isLong() ? 1d : -1d)
        * option.getUnderlying().getReceiveCurrencyAmount().getAmount() * price);
  }

  public double price(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
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
        ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * forwardPrice;
  }

  public double impliedVol(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    return volatilityProvider.getVolatility(
        strikePair, option.getExpiryDate(), strike.fxRate(strikePair), forward.fxRate(strikePair));
  }

  public double presentValueVega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double fwdVega = BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), option.getExpiryDate());
    
    return (option.getLongShort().isLong() ? 1d : -1d)
        * underlying.getReceiveCurrencyAmount().getAmount() * discountFactor * fwdVega;
  }

  public FxOptionSensitivity presentValueSensitivityBlackVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double fwdVega = BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), option.getExpiryDate());
    double vega = (option.getLongShort().isLong() ? 1d : -1d)
        * underlying.getReceiveCurrencyAmount().getAmount() * discountFactor * fwdVega;

    return FxOptionSensitivity
        .of(strikePair, option.getExpiryDate(), strikeRate, forwardRate, option.getPayoffCurrency(), vega);
  }

  // TODO requires forward sensitivity to spot to compute spot-related Greeks, i.e., spotDelta, theta, etc.
  // TODO requires forward sensitivity to curve to compute parallel and bucketed pv01
}
