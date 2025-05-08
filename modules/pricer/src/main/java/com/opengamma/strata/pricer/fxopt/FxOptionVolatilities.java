/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.product.common.PutCall;

/**
 * Volatilities for pricing FX options.
 * <p>
 * This provides access to the volatilities for pricing models, such as Black.
 */
public interface FxOptionVolatilities
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the name of these volatilities.
   * 
   * @return the name
   */
  public abstract FxOptionVolatilitiesName getName();

  /**
   * Gets the currency pair for which the data is valid.
   * 
   * @return the currency pai
   */
  public abstract CurrencyPair getCurrencyPair();

  /**
   * Gets the type of volatility returned by the {@link FxOptionVolatilities#volatility} method.
   * 
   * @return the type
   */
  public abstract ValueType getVolatilityType();

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

  /**
   * Gets the valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  @Override
  public abstract FxOptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract FxOptionVolatilities withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility at the specified expiry.
   * 
   * @param currencyPair  the currency pair
   * @param expiryDateTime  the option expiry
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @return the volatility
   */
  public default double volatility(
      CurrencyPair currencyPair,
      ZonedDateTime expiryDateTime,
      double strike,
      double forward) {

    return volatility(currencyPair, relativeTime(expiryDateTime), strike, forward);
  }

  /**
   * Calculates the volatility at the specified expiry.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * 
   * @param currencyPair  the currency pair
   * @param expiry  the time to expiry as a year fraction
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @return the volatility
   */
  public abstract double volatility(
      CurrencyPair currencyPair,
      double expiry,
      double strike,
      double forward);

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

  /**
   * Computes the partial derivatives of the volatilities.
   * <p>
   * The first derivatives are {@code dVol/dExpiry, dVol/dStrike, dVol/dForward}.
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to expiry
   * <li>[1] derivative with respect to strike
   * <li>[2] derivative with respect to forward
   * </ul>
   *
   * @param currencyPair  the currency pair
   * @param expiry  the expiry at which the partial derivative is taken
   * @param strike  the strike at which the partial derivative is taken
   * @param forward  the forward rate at which the partial derivative is taken
   * @return the z-value and it's partial first derivatives
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract ValueDerivatives firstPartialDerivatives(
      CurrencyPair currencyPair,
      double expiry,
      double strike,
      double forward);

  //-------------------------------------------------------------------------
  /**
   * Calculates the price.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on volatility supplied by {@link #volatility(CurrencyPair, double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @param volatility  the volatility
   * @return the price
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double price(
      double expiry,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

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
