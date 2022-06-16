/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;

/**
 * Volatilities for bond options.
 * <p>
 * The volatilities are stored as bond yield equivalent volatilities but are converted to bond price volatilities
 * through the formula "price volatility = duration * yield * yield volatility".
 */
public interface BondYieldVolatilities
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the currency for which the data is valid.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the name of these volatilities.
   * 
   * @return the name
   */
  public abstract BondVolatilitiesName getName();

  /**
   * Gets the type of volatility returned by the {@link BondYieldVolatilities#volatility} method.
   * 
   * @return the type
   */
  public abstract ValueType getVolatilityType();

  /**
   * Gets the valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  /**
   * Gets the valuation date.
   * <p>
   * The volatilities are calibrated for this date.
   * 
   * @return the valuation date
   */
  @Override
  public default LocalDate getValuationDate() {
    return getValuationDateTime().toLocalDate();
  }

  @Override
  public abstract BondYieldVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract BondYieldVolatilities withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------

  /**
   * Calculates the volatility at the specified expiry.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param duration  the modified duration of the instrument as a year fraction
   * @param strike  the strike yield
   * @param forward  the forward yield of the underlying bond
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double volatility(double expiry, double duration, double strike, double forward);

  /**
   * Calculates the volatility at the specified expiry.
   * 
   * @param expiryDateTime  the option expiry
   * @param duration  the modified duration of the instrument as a year fraction
   * @param strike  the strike yield
   * @param forward  the forward yield of the underlying bond
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double volatility(ZonedDateTime expiryDateTime, double duration, double strike, double forward) {
    return volatility(relativeTime(expiryDateTime), duration, strike, forward);
  }

  /**
   * Calculates the price volatility equivalent to the yield volatility.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param duration  the modified duration of the instrument as a year fraction
   * @param strike  the strike yield
   * @param forward  the forward yield of the underlying bond
   * @return the price volatility
   */
  public default double priceVolatilityEquivalent(double expiry, double duration, double strike, double forward) {
    double yieldVolatility = volatility(expiry, duration, strike, forward);
    return priceVolatilityEquivalent(duration, yieldVolatility);
  }

  /**
   * Calculates the price volatility equivalent to the yield volatility and its derivatives.
   * <p>
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to the duration
   * <li>[1] derivative with respect to the yieldVolatility
   * </ul>
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param duration  the modified duration of the instrument as a year fraction
   * @param strike  the strike yield
   * @param forward  the forward yield of the underlying bond
   * @return the price volatility
   */
  public default ValueDerivatives priceVolatilityEquivalentAd(double expiry, double duration, double strike, double forward) {
    double yieldVolatility = volatility(expiry, duration, strike, forward);
    return priceVolatilityEquivalentAd(duration, yieldVolatility);
  }

  /**
   * Calculates the price volatility equivalent to the yield volatility.
   * 
   * @param expiryDateTime  the option expiry
   * @param duration  the modified duration of the instrument as a year fraction
   * @param strike  the strike yield
   * @param forward  the forward yield of the underlying bond
   * @return the price volatility
   */
  public default double priceVolatilityEquivalent(
      ZonedDateTime expiryDateTime,
      double duration,
      double strike,
      double forward) {

    double yieldVolatility = volatility(expiryDateTime, duration, strike, forward);
    return priceVolatilityEquivalent(duration, yieldVolatility);
  }

  /**
   * Calculates the price volatility equivalent to the yield volatility and its derivatives.
   * <p>
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to the duration
   * <li>[1] derivative with respect to the yieldVolatility
   * </ul>
   * 
   * @param expiryDateTime  the option expiry
   * @param duration  the modified duration of the instrument as a year fraction
   * @param strike  the strike yield
   * @param forward  the forward yield of the underlying bond
   * @return the price volatility
   */
  public default ValueDerivatives priceVolatilityEquivalentAd(
      ZonedDateTime expiryDateTime,
      double duration,
      double strike,
      double forward) {

    double yieldVolatility = volatility(expiryDateTime, duration, strike, forward);
    return priceVolatilityEquivalentAd(duration, yieldVolatility);
  }

  /**
   * Calculates the price volatility equivalent to the yield volatility.
   * 
   * @param duration  the modified duration
   * @param yieldVolatility  the yield volatility
   * @return the price volatility
   */
  public default double priceVolatilityEquivalent(double duration, double yieldVolatility) {
    return duration * yieldVolatility;
  }

  /**
   * Calculates the price volatility equivalent to the yield volatility and its derivatives.
   * <p>
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to the duration
   * <li>[1] derivative with respect to the yieldVolatility
   * </ul>
   * 
   * @param duration  the modified duration
   * @param yieldVolatility  the yield volatility
   * @return the price volatility
   */
  public default ValueDerivatives priceVolatilityEquivalentAd(double duration, double yieldVolatility) {
    double value = duration * yieldVolatility;
    return ValueDerivatives.of(value, DoubleArray.of(yieldVolatility, duration));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the parameter sensitivity.
   * <p>
   * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the internal parameters representation.
   * 
   * @param pointSensitivities  the point sensitivities
   * @return the sensitivity to the underlying parameters
   */
  public default CurrencyParameterSensitivities parameterSensitivity(PointSensitivity... pointSensitivities) {
    return parameterSensitivity(PointSensitivities.of(pointSensitivities));
  }

  /**
   * Calculates the parameter sensitivity.
   * <p>
   * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the internal parameters representation.
   * 
   * @param pointSensitivities  the point sensitivities
   * @return the sensitivity to the underlying parameters
   */
  public abstract CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities);

  //-------------------------------------------------------------------------
  /**
   * Converts a time and date to a relative year fraction.
   * <p>
   * When the date is after the valuation date (and potentially time), the returned number is negative.
   * 
   * @param dateTime  the date-time to find the relative year fraction of
   * @return the relative year fraction
   */
  public abstract double relativeTime(ZonedDateTime dateTime);

}
