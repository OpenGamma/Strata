/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.provider;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.basics.index.IborIndex;

/**
 * Volatility environment for swaption in the normal or Bachelier model.
 */
public interface NormalVolatilitySwaptionProvider 
    extends ImmutableBean {
  
  /**
   * Returns the normal volatility.
   * @param expiryDate  the option expiry
   * @param tenor  the swaption tenor in years
   * @param strike  the option strike rate
   * @param forwardRate  the forward rate of the underlying swap
   * @return the volatility
   */
  public double getVolatility(ZonedDateTime expiryDate, double tenor, double strike, double forwardRate);
  
  /**
   * Returns the index on which the underlying swap is based.
   * TODO: replace by the full swap conventions
   * @return the index
   */
  public IborIndex getIndex();

  //-------------------------------------------------------------------------
  /**
   * Converts a time and date to a relative year fraction.
   * 
   * @param date  the date/time to find the relative year fraction of
   * @return the relative year fraction
   */
  public abstract double relativeYearFraction(ZonedDateTime date);
  
  /**
   * Returns the tenor of the swap based on its start date and end date.
   * @param startDate  the start date
   * @param endDate  the end date
   * @return the tenor
   */
  public abstract double tenor(LocalDate startDate, LocalDate endDate);

  /**
   * Returns the valuation date. All data items in this provider are calibrated for this date.
   * @return the date
   */
  public LocalDate getValuationDate();

}
