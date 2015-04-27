/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.basics.index.IborIndex;

/**
 * Volatility environment for Ibor future options in the normal or Bachelier model.
 */
public interface NormalVolatilityIborFutureProvider 
    extends IborFutureParameters, ImmutableBean {

  /**
   * Returns the normal volatility.
   * @param expiryDate  the option expiry
   * @param fixingDate  the underlying future fixing date
   * @param strikePrice  the option strike price
   * @param futurePrice  the price of the underlying future
   * @return the volatility
   */
  public double getVolatility(LocalDate expiryDate, LocalDate fixingDate, double strikePrice, double futurePrice);
  
  /**
   * Returns the index on which the underlying future is based.
   * @return the index
   */
  public IborIndex getFutureIndex();

  //-------------------------------------------------------------------------
  /**
   * Converts a date to a relative {@code double} time.
   * 
   * @param date  the date to find the relative time of
   * @param time  the time to find the relative time of
   * @param zone  the time zone
   * @return the relative time
   */
  public abstract double relativeTime(LocalDate date, LocalTime time, ZoneId zone);

}
