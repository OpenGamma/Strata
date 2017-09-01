/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;

/**
 * A index of interest rates, such as an Overnight or Inter-Bank rate.
 * <p>
 * Many financial products require knowledge of interest rate indices, such as Libor.
 * Implementations of this interface define these indices.
 * See {@link IborIndex} and {@link OvernightIndex}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RateIndex
    extends FloatingRateIndex {

  /**
   * Gets the calendar that determines which dates are fixing dates.
   * <p>
   * The rate will be fixed on each business day in this calendar.
   * 
   * @return the calendar used to determine the fixing dates of the index
   */
  public abstract HolidayCalendarId getFixingCalendar();

  /**
   * Gets the tenor of the index.
   * 
   * @return the tenor
   */
  public abstract Tenor getTenor();

}
