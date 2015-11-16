/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.collect.id.StandardId;

/**
 * Data provider of volatility for bond future options in the lognormal or Black model.
 */
public interface BlackVolatilityBondFutureProvider extends BondFutureProvider {

  /**
   * Gets the valuation date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  /**
   * Returns the normal volatility.
   * 
   * @param expiry  the expiry date-time of the option
   * @param fixingDate  the underlying future fixing date
   * @param strikePrice  the option strike price
   * @param futurePrice  the price of the underlying future
   * @return the volatility
   */
  public abstract double getVolatility(
      ZonedDateTime expiry,
      LocalDate fixingDate,
      double strikePrice,
      double futurePrice);

  /**
   * Returns the ID on which the underlying future is based.
   * 
   * @return the ID
   */
  public abstract StandardId getFutureSecurityId();

  /**
   * Converts a date to a relative {@code double} time.
   * 
   * @param zonedDateTime  the zoned date time
   * @return the relative time
   */
  public abstract double relativeTime(ZonedDateTime zonedDateTime);
}
