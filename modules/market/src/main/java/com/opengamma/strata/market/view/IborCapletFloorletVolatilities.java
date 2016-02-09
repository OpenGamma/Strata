/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.sensitivity.IborCapletFloorletSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;

/**
 * Volatilities for pricing Ibor caplet/floorlet.
 * <p>
 * This provides access to the volatilities for various pricing models, such as normal and Black.
 * The price and derivatives are also made available.
 */
public interface IborCapletFloorletVolatilities 
    extends MarketDataView {

  /**
   * Gets the Ibor index for which the data is valid.
   * 
   * @return the Ibor index
   */
  public abstract IborIndex getIndex();

  @Override
  public default LocalDate getValuationDate() {
    return getValuationDateTime().toLocalDate();
  }

  /**
   * Gets the valuation date-time.
   * <p>
   * The raw data in this provider is calibrated for this date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility at the specified date-time.
   * 
   * @param expiryDateTime  the option expiry
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public default double volatility(ZonedDateTime expiryDateTime, double strike, double forward) {
    return volatility(relativeTime(expiryDateTime), strike, forward);
  }

  /**
   * Calculates the volatility at the specified date-time.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double volatility(double expiry, double strike, double forward);

  /**
   * Calculates the surface parameter sensitivities from the point sensitivities. 
   * 
   * @param pointSensitivities  the point sensitivities
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  default SurfaceCurrencyParameterSensitivities surfaceCurrencyParameterSensitivity(
        PointSensitivities pointSensitivities) {
    List<SurfaceCurrencyParameterSensitivity> sensitivitiesTotal = pointSensitivities.getSensitivities()
              .stream()
              .filter(pointSensitivity -> (pointSensitivity instanceof IborCapletFloorletSensitivity))
              .map(pointSensitivity -> surfaceCurrencyParameterSensitivity((IborCapletFloorletSensitivity) pointSensitivity))
              .collect(Collectors.toList());
    SurfaceCurrencyParameterSensitivities sensi = SurfaceCurrencyParameterSensitivities.of(sensitivitiesTotal);
    // sensi should be single SurfaceCurrencyParameterSensitivity or empty
    ArgChecker.isTrue(sensi.getSensitivities().size() <= 1, "The underlying surface must be unique");
    return sensi;
    }

  /**
   * Calculates the surface parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to surface parameter sensitivity.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(
      IborCapletFloorletSensitivity pointSensitivity);

  //-------------------------------------------------------------------------
  /**
   * Calculates the price.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @param volatility  the volatility
   * @return the price
   * @throws RuntimeException if the value cannot be obtained
   */
  public double price(
      double expiry,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price delta.
   * <p>
   * This is the first order sensitivity of the option price to the forward.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @param volatility  the volatility
   * @return the delta
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceDelta(
      double expiry,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price gamma.
   * <p>
   * This is the second order sensitivity of the option price to the forward.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @param volatility  the volatility
   * @return the gamma
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceGamma(
      double expiry,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price theta.
   * <p>
   * This is the driftless sensitivity of the option price to a change in time to maturity.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @param volatility  the volatility
   * @return the theta
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceTheta(
      double expiry,
      PutCall putCall,
      double strike,
      double forward,
      double volatility);

  /**
   * Calculates the price vega.
   * <p>
   * This is the sensitivity of the option price to the implied volatility.
   * <p>
   * This relies on expiry supplied by {@link #relativeTime(ZonedDateTime)}.
   * This relies on volatility supplied by {@link #volatility(double, double, double)}.
   * 
   * @param expiry  the time to expiry as a year fraction
   * @param putCall  whether the option is put or call
   * @param strike  the option strike rate
   * @param forward  the forward rate
   * @param volatility  the volatility
   * @return the vega
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double priceVega(
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
   * @param date  the date/time to find the relative year fraction of
   * @return the relative year fraction
   */
  public abstract double relativeTime(ZonedDateTime date);

}
