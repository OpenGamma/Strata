/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;

/**
 * Volatilities for pricing Ibor futures.
 * <p>
 * This provides access to the volatilities for various pricing models, such as normal.
 */
public interface IborFutureOptionVolatilities
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the name of these volatilities.
   * 
   * @return the name
   */
  public abstract IborFutureOptionVolatilitiesName getName();

  /**
   * Gets the index of the underlying future for which the data is valid.
   * 
   * @return the index
   */
  public abstract IborIndex getIndex();

  /**
   * Gets the type of volatility returned by the {@link IborFutureOptionVolatilities#volatility} method.
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
  public abstract IborFutureOptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract IborFutureOptionVolatilities withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility at the specified expiry.
   * 
   * @param expiryDateTime  the option expiry
   * @param fixingDate  the underlying future fixing date
   * @param strikePrice  the option strike price
   * @param futurePrice  the price of the underlying future
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double volatility(
      ZonedDateTime expiryDateTime,
      LocalDate fixingDate,
      double strikePrice,
      double futurePrice) {

    return volatility(relativeTime(expiryDateTime), fixingDate, strikePrice, futurePrice);
  }

  /**
   * Calculates the volatility at the specified expiry.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param fixingDate  the underlying future fixing date
   * @param strikePrice  the option strike price
   * @param futurePrice  the price of the underlying future
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double volatility(double expiry, LocalDate fixingDate, double strikePrice, double futurePrice);

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
