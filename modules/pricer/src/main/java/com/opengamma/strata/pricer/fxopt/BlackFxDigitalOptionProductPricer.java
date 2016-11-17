/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.fx.DiscountingFxSingleProductPricer;
import com.opengamma.strata.pricer.impl.option.BlackDigitalPriceFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxDigitalOption;

/**
 * Pricer for foreign exchange Digital option transaction products with a lognormal model.
 * p>
 * This function provides the ability to price an {@link ResolvedFxDigitalOption}.
 * <p>
 * All of the computation is be based on the counter currency of the underlying FX transaction.
 * For example, price, PV and risk measures of the product will be expressed in USD for an option on EUR/USD.
 */
public class BlackFxDigitalOptionProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackFxDigitalOptionProductPricer DEFAULT =
      new BlackFxDigitalOptionProductPricer(DiscountingFxSingleProductPricer.DEFAULT);

  /**
   * Underlying FX pricer.
   */
  private final DiscountingFxSingleProductPricer fxPricer;

  /**
   * Creates an instance.
   *
   * @param fxPricer  the pricer for {@link ResolvedFxSingle}
   */
  public BlackFxDigitalOptionProductPricer(
      DiscountingFxSingleProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the underlying FX product.
   *
   * @return the pricer
   */
  DiscountingFxSingleProductPricer getDiscountingFxSingleProductPricer() {
    return fxPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the foreign exchange vanilla option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency
   * and is expressed in the counter currency. The price does not take into account the long/short flag.
   * See {@link #presentValue} for scaling and currency.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxSingle underlying = option.getUnderlying();
    // forward option price
    double forwardPrice = undiscountedPrice(option, ratesProvider, volatilities);
    double discountFactor = ratesProvider.discountFactor(option.getCounterCurrency(), underlying.getPaymentDate());
    //discounted option price
    return discountFactor * forwardPrice;
  }

  /**
   * Calculates the present value of the foreign exchange vanilla option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * It is expressed in the counter currency.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {
    // disc option price
    double price = price(option, ratesProvider, volatilities);
    // L/S currency amount
    return CurrencyAmount.of(option.getCounterCurrency(), signedNotional(option) * price);
  }

  // the price without discounting
  private double undiscountedPrice(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry < 0d) {
      return 0d;
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    CurrencyPair strikePair = underlying.getCurrencyPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = option.getStrike();
    boolean isCall = option.getPutCall().isCall();

    double volatility = volatilities.volatility(strikePair, option.getExpiry(), strikeRate, forwardRate);
    return BlackDigitalPriceFormulaRepository.price(forwardRate,strikeRate,timeToExpiry,volatility,isCall,1.0);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the delta of the foreign exchange vanilla option product.
   * <p>
   * The delta is the first derivative of {@link #price} with respect to spot.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the delta of the product
   */
  public double delta(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxSingle underlying = option.getUnderlying();
    double fwdDelta = undiscountedDelta(option, ratesProvider, volatilities);
    double discountFactor = ratesProvider.discountFactor(option.getCounterCurrency(), underlying.getPaymentDate());
    double fwdRateSpotSensitivity = fxPricer.forwardFxRateSpotSensitivity(
        option.getPutCall().isCall() ? underlying : underlying.inverse(), ratesProvider);
    return fwdDelta * discountFactor * fwdRateSpotSensitivity;
  }

  /**
   * Calculates the present value delta of the foreign exchange vanilla option product.
   * <p>
   * The present value delta is the first derivative of {@link #presentValue} with respect to spot.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value delta of the product
   */
  public CurrencyAmount presentValueDelta(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double delta = delta(option, ratesProvider, volatilities);
    return CurrencyAmount.of(option.getCounterCurrency(), signedNotional(option) * delta);
  }

  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the product
   * @deprecated Use presentValueSensitivityRatesStickyStrike
   */
  @Deprecated
  public PointSensitivities presentValueSensitivityRates(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    return presentValueSensitivityRatesStickyStrike(option, ratesProvider, volatilities);
  }

  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    if (volatilities.relativeTime(option.getExpiry()) < 0d) {
      return PointSensitivities.empty();
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    double fwdDelta = undiscountedDelta(option, ratesProvider, volatilities);
    double discountFactor = ratesProvider.discountFactor(option.getCounterCurrency(), underlying.getPaymentDate());
    double notional = signedNotional(option);
    PointSensitivityBuilder fwdSensi = fxPricer.forwardFxRatePointSensitivity(
        option.getPutCall().isCall() ? underlying : underlying.inverse(), ratesProvider)
        .multipliedBy(notional * discountFactor * fwdDelta);
    double fwdPrice = undiscountedPrice(option, ratesProvider, volatilities);
    PointSensitivityBuilder dscSensi = ratesProvider.discountFactors(option.getCounterCurrency())
        .zeroRatePointSensitivity(underlying.getPaymentDate()).multipliedBy(notional * fwdPrice);
    return fwdSensi.combinedWith(dscSensi).build().convertedTo(option.getCounterCurrency(), ratesProvider);
  }

  // the delta without discounting
  private double undiscountedDelta(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry < 0d) {
      return 0d;
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    CurrencyPair strikePair = underlying.getCurrencyPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = option.getStrike();
    boolean isCall = option.getPutCall().isCall();
    if (timeToExpiry == 0d) {
      return isCall ? (forwardRate > strikeRate ? 1d : 0d) : (strikeRate > forwardRate ? -1d : 0d);
    }
    //set<Currency> cr = ratesProvider.getDiscountCurrencies();

    double volatility = volatilities.volatility(strikePair, option.getExpiry(), strikeRate, forwardRate);
    return BlackDigitalPriceFormulaRepository.delta(forwardRate,strikeRate,timeToExpiry,volatility,isCall,1.0);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the gamma of the foreign exchange vanilla option product.
   * <p>
   * The gamma is the second derivative of {@link #price} with respect to spot.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the gamma of the product
   */
  public double gamma(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    CurrencyPair strikePair = underlying.getCurrencyPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = option.getStrike();
    double volatility = volatilities.volatility(
        strikePair, option.getExpiry(), strikeRate, forwardRate);
    boolean isCall = option.getPutCall().isCall();
    double forwardGamma = BlackDigitalPriceFormulaRepository.gamma(forwardRate,strikeRate,timeToExpiry,volatility,isCall,1.0);
    double discountFactor = ratesProvider.discountFactor(option.getCounterCurrency(), underlying.getPaymentDate());
    double fwdRateSpotSensitivity = fxPricer.forwardFxRateSpotSensitivity(
        option.getPutCall().isCall() ? underlying : underlying.inverse(), ratesProvider);
    return forwardGamma * discountFactor * fwdRateSpotSensitivity * fwdRateSpotSensitivity;
  }

  /**
   * Calculates the present value delta of the foreign exchange vanilla option product.
   * <p>
   * The present value gamma is the second derivative of the {@link #presentValue} with respect to spot.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value gamma of the product
   */
  public CurrencyAmount presentValueGamma(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double gamma = gamma(option, ratesProvider, volatilities);
    return CurrencyAmount.of(option.getCounterCurrency(), signedNotional(option) * gamma);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the vega of the foreign exchange vanilla option product.
   * <p>
   * The vega is the first derivative of {@link #price} with respect to volatility.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the vega of the product
   */
  public double vega(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    CurrencyPair strikePair = underlying.getCurrencyPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = option.getStrike();
    boolean isCall = option.getPutCall().isCall();
    double volatility = volatilities.volatility(strikePair, option.getExpiry(), strikeRate, forwardRate);
    double fwdVega = BlackDigitalPriceFormulaRepository.vega(forwardRate,strikeRate,timeToExpiry,volatility,isCall,1.0);
    double discountFactor = ratesProvider.discountFactor(option.getCounterCurrency(), underlying.getPaymentDate());
    return discountFactor * fwdVega;
  }

  /**
   * Calculates the present value vega of the foreign exchange vanilla option product.
   * <p>
   * The present value vega is the first derivative of the {@link #presentValue} with respect to volatility.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueVega(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double vega = vega(option, ratesProvider, volatilities);
    return CurrencyAmount.of(option.getCounterCurrency(), signedNotional(option) * vega);
  }

  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    if (volatilities.relativeTime(option.getExpiry()) <= 0d) {
      return PointSensitivityBuilder.none();
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    CurrencyPair strikePair = underlying.getCurrencyPair();
    CurrencyAmount valueVega = presentValueVega(option, ratesProvider, volatilities);
    return FxOptionSensitivity.of(
        volatilities.getName(),
        strikePair,
        volatilities.relativeTime(option.getExpiry()),
        option.getStrike(),
        forward.fxRate(strikePair),
        valueVega.getCurrency(),
        valueVega.getAmount());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the Black theta of the foreign exchange vanilla option product.
   * <p>
   * The theta is the negative of the first derivative of {@link #price} with respect to time parameter
   * in Black formula (the discounted driftless theta).
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the theta of the product
   */
  public double theta(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    ResolvedFxSingle underlying = option.getUnderlying();
    FxRate forward = fxPricer.forwardFxRate(underlying, ratesProvider);
    CurrencyPair strikePair = underlying.getCurrencyPair();
    double forwardRate = forward.fxRate(strikePair);
    double strikeRate = option.getStrike();
    double volatility = volatilities.volatility(
        strikePair, option.getExpiry(), strikeRate, forwardRate);
    boolean isCall = option.getPutCall().isCall();
    double Rd = 0.0; // used to set discountfactor = 1
    double fwdTheta = BlackDigitalPriceFormulaRepository.theta(forwardRate,strikeRate,timeToExpiry,Rd,volatility,isCall,1.0);
    double discountFactor = ratesProvider.discountFactor(option.getCounterCurrency(), underlying.getPaymentDate());
    return discountFactor * fwdTheta;
  }

  /**
   * Calculates the present value theta of the foreign exchange vanilla option product.
   * <p>
   * The present value theta is the negative of the first derivative of {@link #presentValue} with time parameter
   * in Black formula, i.e., the driftless theta of the present value.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueTheta(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double theta = theta(option, ratesProvider, volatilities);
    return CurrencyAmount.of(option.getCounterCurrency(), signedNotional(option) * theta);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied Black volatility of the foreign exchange vanilla option product.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the implied volatility of the product
   * @throws IllegalArgumentException if the option has expired
   */
  public double impliedVolatility(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    if (timeToExpiry <= 0d) {
      throw new IllegalArgumentException("valuation is after option's expiry.");
    }
    FxRate forward = fxPricer.forwardFxRate(option.getUnderlying(), ratesProvider);
    CurrencyPair strikePair = option.getUnderlying().getCurrencyPair();
    return volatilities.volatility(
        strikePair, option.getExpiry(), option.getStrike(), forward.fxRate(strikePair));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the foreign exchange vanilla option product.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    CurrencyPair strikePair = option.getUnderlying().getCurrencyPair();
    double price = price(option, ratesProvider, volatilities);
    double delta = delta(option, ratesProvider, volatilities);
    double spot = ratesProvider.fxRate(strikePair);
    double signedNotional = signedNotional(option);
    CurrencyAmount domestic = CurrencyAmount.of(strikePair.getCounter(), (price - delta * spot) * signedNotional);
    CurrencyAmount foreign = CurrencyAmount.of(strikePair.getBase(), delta * signedNotional);
    return MultiCurrencyAmount.of(domestic, foreign);
  }

  //-------------------------------------------------------------------------
  // signed notional amount to computed present value and value Greeks
  private double signedNotional(ResolvedFxDigitalOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) *
        Math.abs(option.getUnderlying().getBaseCurrencyPayment().getAmount());
  }

}
