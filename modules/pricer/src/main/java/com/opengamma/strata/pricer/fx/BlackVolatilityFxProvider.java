/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.SurfaceCurrencyParameterSensitivity;

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
  public abstract double getVolatility(
      CurrencyPair currencyPair,
      ZonedDateTime expiryDateTime,
      double strike,
      double forward);

  /**
   * Computes the sensitivity to the nodes used in the description of
   * the Black volatility from a point sensitivity.
   * 
   * @param point  the point sensitivity at a given key
   * @return the sensitivity to the nodes
   */
  public abstract SurfaceCurrencyParameterSensitivity surfaceParameterSensitivity(FxOptionSensitivity point);

}
