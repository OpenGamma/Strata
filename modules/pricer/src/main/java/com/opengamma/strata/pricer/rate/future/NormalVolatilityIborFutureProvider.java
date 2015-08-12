/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.index.IborIndex;

/**
 * Data provider of volatility for Ibor future options in the normal or Bachelier model.
 */
public interface NormalVolatilityIborFutureProvider
    extends IborFutureProvider {

  /**
   * Gets the valuation date-time.
   * 
   * @return the valuation date-time
   */
  public abstract ZonedDateTime getValuationDateTime();

  /**
   * Returns the normal volatility.
   * 
   * @param expiration  the expiration date-time of the option
   * @param fixingDate  the underlying future fixing date
   * @param strikePrice  the option strike price
   * @param futurePrice  the price of the underlying future
   * @return the volatility
   */
  public abstract double getVolatility(ZonedDateTime expiration, LocalDate fixingDate, double strikePrice, double futurePrice);

  /**
   * Returns the index on which the underlying future is based.
   * @return the index
   */
  public abstract IborIndex getFutureIndex();

  /**
   * Converts a date to a relative {@code double} time.
   * 
   * @param zonedDateTime  the zoned date time
   * @return the relative time
   */
  public abstract double relativeTime(ZonedDateTime zonedDateTime);

}
