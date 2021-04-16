/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;

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
   * Obtains an instance from the specified unique name.
   * <p>
   * This parses names from {@link IborIndex} and {@link OvernightIndex}.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static RateIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return Indices.RATE_INDEX_LOOKUP.lookup(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

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
