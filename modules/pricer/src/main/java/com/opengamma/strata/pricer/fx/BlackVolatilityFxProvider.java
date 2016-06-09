/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.product.fx.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;

/**
 * Data provider of volatility for FX options in the lognormal or Black-Scholes model.
 */
public interface BlackVolatilityFxProvider {

  /**
   * Gets the valuation date-time.
   * <p>
   * The data in this provider is calibrated for this date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  /**
   * Gets the currency pair of the provider.
   * <p>
   * The data in this provider is calibrated for this currency pair.
   * 
   * @return the currency pair
   */
  public abstract CurrencyPair getCurrencyPair();

  //-------------------------------------------------------------------------
  /**
   * Converts a date to a relative {@code double} time.
   * 
   * @param zonedDateTime  the zoned date time
   * @return the relative time
   */
  public abstract double relativeTime(ZonedDateTime zonedDateTime);

  /**
   * Calculates the Black volatility.
   * 
   * @param currencyPair  the currency pair
   * @param expiryDateTime  the option expiry
   * @param strike  the option strike
   * @param forward  the underling forward
   * @return the volatility
   */
  public default double volatility(
      CurrencyPair currencyPair,
      ZonedDateTime expiryDateTime,
      double strike,
      double forward) {

    double expiryTime = relativeTime(expiryDateTime);
    return volatility(currencyPair, expiryTime, strike, forward);
  }

  /**
   * Calculates the Black volatility.
   * 
   * @param currencyPair  the currency pair
   * @param expiryTime  the option expiry
   * @param strike  the option strike
   * @param forward  the underling forward
   * @return the volatility
   */
  public abstract double volatility(
      CurrencyPair currencyPair,
      double expiryTime,
      double strike,
      double forward);

  /**
   * Computes the sensitivity to the nodes used in the description of
   * the Black volatility from a point sensitivity.
   * 
   * @param point  the point sensitivity at a given key
   * @return the sensitivity to the nodes
   */
  public abstract CurrencyParameterSensitivity surfaceParameterSensitivity(FxOptionSensitivity point);

  /**
   * Calculates the surface parameter sensitivities from the point sensitivities. 
   * 
   * @param sensitivities  the point sensitivities
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public default CurrencyParameterSensitivities surfaceParameterSensitivity(
      PointSensitivities sensitivities) {
    List<CurrencyParameterSensitivity> sensitivitiesTotal =
        sensitivities.getSensitivities()
            .stream()
            .filter(pointSensitivity -> pointSensitivity instanceof FxOptionSensitivity)
            .map(pointSensitivity -> surfaceParameterSensitivity((FxOptionSensitivity) pointSensitivity))
            .collect(Collectors.toList());
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.of(sensitivitiesTotal);
    // sensi should be single CurrencyParameterSensitivity or empty
    ArgChecker.isTrue(sensi.getSensitivities().size() <= 1, "The underlying surface must be unique");
    return sensi;
  }

}
