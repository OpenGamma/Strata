/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.named.Named;

/**
 * A convention defining how to calculate fractions of a year.
 * <p>
 * The purpose of this convention is to define how to convert dates into numeric year fractions.
 * The is of use when calculating accrued interest over time.
 * <p>
 * The most common implementations are provided in {@link DayCounts}.
 * Additional implementations may be added by implementing this interface.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface DayCount
    extends Named {

  /**
   * Obtains a {@code DayCount} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the day count
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static DayCount of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return DayCounts.of(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the day count between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   * 
   * @param firstDate  the earlier date, which should be a schedule period date
   * @param secondDate  the later date
   * @return the day count fraction
   */
  public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate);

  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public String getName();

}
