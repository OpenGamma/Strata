/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatilities for pricing swaptions.
 * <p>
 * This provides access to the volatilities for various pricing models, such as normal, Black and SABR.
 * The price and derivatives are also made available.
 */
public interface SwaptionVolatilities
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the name of these volatilities.
   * 
   * @return the name
   */
  public abstract SwaptionVolatilitiesName getName();

  /**
   * Gets the convention of the swap for which the data is valid.
   * 
   * @return the convention
   */
  public abstract FixedIborSwapConvention getConvention();

  /**
   * Gets the type of volatility returned by the {@link SwaptionVolatilities#volatility} method.
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
  public abstract SwaptionVolatilities withParameter(int parameterIndex, double newValue);

  @Override
  public abstract SwaptionVolatilities withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility at the specified expiry.
   * <p>
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * 
   * @param expiryDateTime  the option expiry
   * @param tenor  the tenor of the instrument as a year fraction
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double volatility(ZonedDateTime expiryDateTime, double tenor, double strike, double forward) {
    return volatility(relativeTime(expiryDateTime), tenor, strike, forward);
  }

  /**
   * Calculates the volatility at the specified expiry.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double volatility(double expiry, double tenor, double strike, double forward);

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
   * Calculates the price.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @param volatility  the volatility
   * @return the price
   * @throws RuntimeException if the value cannot be obtained
   */
  public double price(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price delta.
   * <p>
   * This is the forward driftless delta.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @param volatility  the volatility
   * @return the delta
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceDelta(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price gamma.
   * <p>
   * This is the second order sensitivity of the forward option value to the forward.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @param volatility  the volatility
   * @return the gamma
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceGamma(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price theta.
   * <p>
   * This is the driftless sensitivity of the present value to a change in time to maturity.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @param volatility  the volatility
   * @return the theta
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceTheta(
      double expiry,
      double tenor,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price vega.
   * <p>
   * This is the sensitivity of the option forward price to the implied volatility.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on tenor supplied by {@link #tenor(LocalDate, LocalDate)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param tenor  the tenor of the instrument as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate of the underlying swap
   * @param volatility  the volatility
   * @return the vega
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceVega(
      double expiry,
      double tenor,
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

  /**
   * Calculates the tenor of the swap based on its start date and end date.
   * 
   * @param startDate  the start date
   * @param endDate  the end date
   * @return the tenor
   */
  public abstract double tenor(LocalDate startDate, LocalDate endDate);

}
