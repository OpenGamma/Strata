/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.common.PutCall;

/**
 * Pricer for caplet/floorlet based on volatilities.
 * <p>
 * The pricing methodologies are defined in individual implementations of the volatilities, {@link IborCapletFloorletVolatilities}. 
 * <p>
 * The value of the caplet/floorlet after expiry is a fixed payoff amount. The value is zero if valuation date is 
 * after payment date of the caplet/floorlet.
 * <p>
 * The consistency between {@code RatesProvider} and {@code IborCapletFloorletVolatilities} is not checked in this 
 * class, but validated only once in {@link VolatilityIborCapFloorLegPricer}.
 */
public class VolatilityIborCapletFloorletPeriodPricer {

  /**
   * Default implementation.
   */
  public static final VolatilityIborCapletFloorletPeriodPricer DEFAULT = new VolatilityIborCapletFloorletPeriodPricer();

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor caplet/floorlet period.
   * <p>
   * The result is expressed using the currency of the period.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    Currency currency = period.getCurrency();
    if (ratesProvider.getValuationDate().isAfter(period.getPaymentDate())) {
      return CurrencyAmount.of(currency, 0d);
    }
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double indexRate = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    if (expiry < 0d) { // Option has expired already
      double sign = putCall.isCall() ? 1d : -1d;
      double payoff = Math.max(sign * (indexRate - strike), 0d);
      return CurrencyAmount.of(currency, df * payoff * period.getYearFraction() * period.getNotional());
    }
    double volatility = volatilities.volatility(expiry, strike, indexRate);
    double price = df * period.getYearFraction() * volatilities.price(expiry, putCall, strike, indexRate, volatility);
    return CurrencyAmount.of(currency, price * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility of the Ibor caplet/floorlet.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the implied volatility
   */
  public double impliedVolatility(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    double strike = period.getStrike();
    return volatilities.volatility(expiry, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the Ibor caplet/floorlet period.
   * <p>
   * The present value delta is given by the first derivative of the present value with respect to forward.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value delta
   */
  public CurrencyAmount presentValueDelta(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double priceDelta = df * period.getYearFraction() *
        volatilities.priceDelta(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, priceDelta * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the Ibor caplet/floorlet period.
   * <p>
   * The present value gamma is given by the second derivative of the present value with respect to forward.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value gamma
   */
  public CurrencyAmount presentValueGamma(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double priceGamma = df * period.getYearFraction() *
        volatilities.priceGamma(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, priceGamma * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value theta of the Ibor caplet/floorlet period.
   * <p>
   * The present value theta is given by the minus of the present value sensitivity to the {@code timeToExpiry} 
   * parameter of the model.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value theta
   */
  public CurrencyAmount presentValueTheta(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    Currency currency = period.getCurrency();
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(currency, 0d);
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    double strike = period.getStrike();
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double priceTheta = df * period.getYearFraction() *
        volatilities.priceTheta(expiry, putCall, strike, forward, volatility);
    return CurrencyAmount.of(currency, priceTheta * period.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the Ibor caplet/floorlet.
   * <p>
   * The present value rates sensitivity of the caplet/floorlet is the sensitivity
   * of the present value to the underlying curves.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value curve sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRates(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    Currency currency = period.getCurrency();
    if (ratesProvider.getValuationDate().isAfter(period.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double indexRate = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    PointSensitivityBuilder dfSensi =
        ratesProvider.discountFactors(currency).zeroRatePointSensitivity(period.getPaymentDate());
    if (expiry < 0d) { // Option has expired already
      double sign = putCall.isCall() ? 1d : -1d;
      double payoff = Math.max(sign * (indexRate - strike), 0d);
      return dfSensi.multipliedBy(payoff * period.getYearFraction() * period.getNotional());
    }
    PointSensitivityBuilder indexRateSensiSensi =
        ratesProvider.iborIndexRates(period.getIndex()).ratePointSensitivity(period.getIborRate().getObservation());
    double volatility = volatilities.volatility(expiry, strike, indexRate);
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double factor = period.getNotional() * period.getYearFraction();
    double fwdPv = factor * volatilities.price(expiry, putCall, strike, indexRate, volatility);
    double fwdDelta = factor * volatilities.priceDelta(expiry, putCall, strike, indexRate, volatility);
    return dfSensi.multipliedBy(fwdPv).combinedWith(indexRateSensiSensi.multipliedBy(fwdDelta * df));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value volatility sensitivity of the Ibor caplet/floorlet.
   * <p>
   * The present value volatility sensitivity of the caplet/floorlet is the sensitivity
   * of the present value to the implied volatility.
   * <p>
   * The sensitivity to the implied volatility is also called vega.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the point sensitivity to the volatility
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(volatilities);
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    double strike = period.getStrike();
    Currency currency = period.getCurrency();
    if (expiry <= 0d) { // Option has expired already or at expiry
      return PointSensitivityBuilder.none();
    }
    double forward = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    double volatility = volatilities.volatility(expiry, strike, forward);
    PutCall putCall = period.getPutCall();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double vega = df * period.getYearFraction() * volatilities.priceVega(expiry, putCall, strike, forward, volatility);
    return IborCapletFloorletSensitivity.of(
        volatilities.getName(),
        expiry,
        strike,
        forward,
        currency,
        vega * period.getNotional());
  }

  /**
   * Validate the volatilities provider.
   * <p>
   * This validate method should be overridden such that a correct implementation of
   * {@code IborCapletFloorletVolatilities} is used for pricing.
   * 
   * @param volatilities  the volatilities
   */
  protected void validate(IborCapletFloorletVolatilities volatilities) {
  }

}
