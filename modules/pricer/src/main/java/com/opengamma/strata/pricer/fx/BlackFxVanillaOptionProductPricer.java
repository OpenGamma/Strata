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
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange vanilla option transaction products with a lognormal model.
 * <p>
 * This function provides the ability to price an {@link FxVanillaOptionProduct}.
 */
public class BlackFxVanillaOptionProductPricer {
  // TODO currency exposure

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
   * Calculates the price of the foreign exchange vanilla option product.
   * <p>
   * The price of the product is the value on the valuation date.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double forwardPrice = undiscountedPrice(option, ratesProvider, volatilityProvider);
    double discountFactor =
        ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * forwardPrice;
  }

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
    double price = price(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * price);
  }

  private double undiscountedPrice(
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
    return forwardPrice;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the delta of the foreign exchange vanilla option product.
   * <p>
   * The delta is the first derivative of the option price with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the delta of the product
   */
  public double delta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double fwdDelta = undiscountedDelta(option, ratesProvider, volatilityProvider);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double fwdRateSpotSensitivity = fxPricer.forwardFxRateSpotSensitivity(underlying, ratesProvider);
    return fwdDelta * discountFactor * fwdRateSpotSensitivity;
  }

  /**
   * Calculates the present value delta of the foreign exchange vanilla option product.
   * <p>
   * The present value delta is the first derivative of the present value with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value delta of the product
   */
  public CurrencyAmount presentValueDelta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double delta = delta(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * delta);
  }

  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivities presentValueSensitivity(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double fwdDelta = undiscountedDelta(option, ratesProvider, volatilityProvider);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double notional = signedNotional(option);
    PointSensitivityBuilder fwdSensi = fxPricer.forwardFxRatePointSensitivity(underlying, ratesProvider)
        .multipliedBy(notional * discountFactor * fwdDelta);
    double fwdPrice = undiscountedPrice(option, ratesProvider, volatilityProvider);
    PointSensitivityBuilder dscSensi = ratesProvider.discountFactors(option.getPayoffCurrency())
        .zeroRatePointSensitivity(underlying.getPaymentDate()).multipliedBy(notional * fwdPrice);
    return fwdSensi.combinedWith(dscSensi).build();
  }

  private double undiscountedDelta(
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
    double forwardPrice = BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
    return forwardPrice;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the gamma of the foreign exchange vanilla option product.
   * <p>
   * The delta is the second derivative of the option price with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the gamma of the product
   */
  public double gamma(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double forwardGamma = BlackFormulaRepository.gamma(forwardRate, strikeRate, timeToExpiry, volatility);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double fwdRateSpotSensitivity = fxPricer.forwardFxRateSpotSensitivity(underlying, ratesProvider);
    return forwardGamma * discountFactor * fwdRateSpotSensitivity * fwdRateSpotSensitivity;
  }

  /**
   * Calculates the present value delta of the foreign exchange vanilla option product.
   * <p>
   * The present value gamma is the second derivative of the present value with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value gamma of the product
   */
  public CurrencyAmount presentValueGamma(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double gamma = gamma(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * gamma);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the vega of the foreign exchange vanilla option product.
   * <p>
   * The vega is the first derivative of the option price with respect to volatility. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the vega of the product
   */
  public double vega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double fwdVega = BlackFormulaRepository.vega(forwardRate, strikeRate, timeToExpiry, volatility);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * fwdVega;
  }

  /**
   * Calculates the present value vega of the foreign exchange vanilla option product.
   * <p>
   * The present value vega is the first derivative of the present value with respect to volatility. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueVega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double vega = vega(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * vega);
  }

  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value sensitivity
   */
  public FxOptionSensitivity presentValueSensitivityBlackVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    CurrencyAmount valueVega = presentValueVega(option, ratesProvider, volatilityProvider);
    return FxOptionSensitivity.of(strikePair, option.getExpiryDate(), strike.fxRate(strikePair),
        forward.fxRate(strikePair), valueVega.getCurrency(), valueVega.getAmount());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the Black theta of the foreign exchange vanilla option product.
   * <p>
   * The theta is the first derivative of the option price with respect to time parameter in Black formula, 
   * i.e., the discounted driftless theta. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the theta of the product
   */
  public double theta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    double volatility = volatilityProvider.getVolatility(strikePair, option.getExpiryDate(), strikeRate, forwardRate);
    double timeToExpiry =
        volatilityProvider.relativeTime(option.getExpiryDate(), option.getExpiryTime(), option.getExpiryZone());
    double fwdTheta = BlackFormulaRepository.driftlessTheta(forwardRate, strikeRate, timeToExpiry, volatility);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * fwdTheta;
  }

  /**
   * Calculates the present value theta of the foreign exchange vanilla option product.
   * <p>
   * The present value theta is the first derivative of the present value with time parameter in Black formula, 
   * i.e., the driftless theta of the present value.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueTheta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double theta = theta(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * theta);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied Black volatility of the foreign exchange vanilla option product.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the implied volatility of the product
   */
  public double impliedVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    return volatilityProvider.getVolatility(
        strikePair, option.getExpiryDate(), strike.fxRate(strikePair), forward.fxRate(strikePair));
  }

  //-------------------------------------------------------------------------
  // signed notional amount to computed present value and value Greeks
  private double signedNotional(FxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) * option.getUnderlying().getReceiveCurrencyAmount().getAmount();
  }
}
