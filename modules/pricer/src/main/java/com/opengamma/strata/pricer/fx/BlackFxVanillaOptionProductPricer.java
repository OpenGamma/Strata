/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleProduct;
import com.opengamma.strata.product.fx.FxVanillaOption;
import com.opengamma.strata.product.fx.FxVanillaOptionProduct;

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
      new BlackFxVanillaOptionProductPricer(DiscountingFxSingleProductPricer.DEFAULT);

  /**
   * Underlying FX pricer.
   */
  private final DiscountingFxSingleProductPricer fxPricer;

  /**
   * Creates an instance.
   * 
   * @param fxPricer  the pricer for {@link FxSingleProduct}
   */
  public BlackFxVanillaOptionProductPricer(
      DiscountingFxSingleProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the underlying FX product.
   * 
   * @return the pricer
   */
  public DiscountingFxSingleProductPricer getDiscountingFxSingleProductPricer() {
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

    FxSingle underlying = option.getUnderlying();
    double forwardPrice = undiscountedPrice(option, ratesProvider, volatilityProvider);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
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

  // the price without discounting
  private double undiscountedPrice(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    double timeToExpiry = volatilityProvider.relativeTime(option.getExpiryDateTime());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(
        strikePair, option.getExpiryDateTime(), strikeRate, forwardRate);
    return BlackFormulaRepository.price(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
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

    FxSingle underlying = option.getUnderlying();
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

    if (volatilityProvider.relativeTime(option.getExpiryDateTime()) <= 0d) {
      return PointSensitivities.empty();
    }
    FxSingle underlying = option.getUnderlying();
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

  // the delta without discounting
  private double undiscountedDelta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    double timeToExpiry = volatilityProvider.relativeTime(option.getExpiryDateTime());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilityProvider.getVolatility(
        strikePair, option.getExpiryDateTime(), strikeRate, forwardRate);
    return BlackFormulaRepository.delta(forwardRate, strikeRate, timeToExpiry, volatility, isCall);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the gamma of the foreign exchange vanilla option product.
   * <p>
   * The gamma is the second derivative of the option price with respect to spot. 
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

    double timeToExpiry = volatilityProvider.relativeTime(option.getExpiryDateTime());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    double volatility = volatilityProvider.getVolatility(
        strikePair, option.getExpiryDateTime(), strikeRate, forwardRate);
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

    double timeToExpiry = volatilityProvider.relativeTime(option.getExpiryDateTime());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    double volatility = volatilityProvider.getVolatility(
        strikePair, option.getExpiryDateTime(), strikeRate, forwardRate);
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
  public PointSensitivityBuilder presentValueSensitivityBlackVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    if (volatilityProvider.relativeTime(option.getExpiryDateTime()) <= 0d) {
      return PointSensitivityBuilder.none();
    }
    FxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    CurrencyAmount valueVega = presentValueVega(option, ratesProvider, volatilityProvider);
    return FxOptionSensitivity.of(strikePair, option.getExpiryDateTime(), strike.fxRate(strikePair),
        forward.fxRate(strikePair), valueVega.getCurrency(), valueVega.getAmount());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the Black theta of the foreign exchange vanilla option product.
   * <p>
   * The theta is the negative of the first derivative of the option price with respect to time parameter
   * in Black formula (the discounted driftless theta).
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

    double timeToExpiry = volatilityProvider.relativeTime(option.getExpiryDateTime());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    FxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = strike.fxRate(strikePair);
    double volatility = volatilityProvider.getVolatility(
        strikePair, option.getExpiryDateTime(), strikeRate, forwardRate);
    double fwdTheta = BlackFormulaRepository.driftlessTheta(forwardRate, strikeRate, timeToExpiry, volatility);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * fwdTheta;
  }

  /**
   * Calculates the present value theta of the foreign exchange vanilla option product.
   * <p>
   * The present value theta is the negative of the first derivative of the present value with time parameter
   * in Black formula, i.e., the driftless theta of the present value.
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
   * @throws IllegalArgumentException if the option has expired
   */
  public double impliedVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    double timeToExpiry = volatilityProvider.relativeTime(option.getExpiryDateTime());
    if (timeToExpiry <= 0d) {
      throw new IllegalArgumentException("valuation is after option's expiry.");
    }
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    return volatilityProvider.getVolatility(
        strikePair, option.getExpiryDateTime(), strike.fxRate(strikePair), forward.fxRate(strikePair));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the foreign exchange vanilla option product.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    CurrencyPair strikePair = option.getStrike().getPair();
    double price = price(option, ratesProvider, volatilityProvider);
    double delta = delta(option, ratesProvider, volatilityProvider);
    double spot = ratesProvider.fxRate(strikePair);
    double signedNotional = signedNotional(option);
    CurrencyAmount domestic = CurrencyAmount.of(option.getPayoffCurrency(), (price - delta * spot) * signedNotional);
    CurrencyAmount foreign = CurrencyAmount.of(strikePair.getBase(), delta * signedNotional);
    return MultiCurrencyAmount.of(domestic, foreign);
  }

  //-------------------------------------------------------------------------
  // signed notional amount to computed present value and value Greeks
  private double signedNotional(FxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) * option.getUnderlying().getReceiveCurrencyAmount().getAmount();
  }

}
