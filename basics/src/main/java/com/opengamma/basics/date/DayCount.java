/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.SchedulePeriodType;
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
   * <p>
   * This uses the simple {@link ScheduleInfo} which is insufficient to be able
   * to calculate certain day counts.
   * 
   * @param firstDate  the earlier date, which should be a schedule period date
   * @param secondDate  the later date
   * @return the day count fraction
   * @throws UnsupportedOperationException if the day count cannot be obtained
   */
  public default double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
    return getDayCountFraction(firstDate, secondDate, ScheduleInfo.SIMPLE);
  }

  /**
   * Gets the day count between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   * 
   * @param firstDate  the earlier date, which should be a schedule period date
   * @param secondDate  the later date
   * @param scheduleInfo  the schedule information
   * @return the day count fraction
   * @throws UnsupportedOperationException if the day count cannot be obtained
   */
  public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo);

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

  //-------------------------------------------------------------------------
  /**
   * Information about the schedule necessary to calculate the day count.
   * <p>
   * Some {@link DayCount} implementations require additional information about the schedule.
   * Implementations of this interface provide that information.
   */
  public interface ScheduleInfo {

    /**
     * A simple schedule information object.
     * <p>
     * The returns false for maturity date and end of month and an exception for the end date.
     */
    public static final ScheduleInfo SIMPLE = new ScheduleInfo() {};

    /**
     * Checks if the specified date is the end of the whole schedule.
     * <p>
     * This is used to check for the maturity/termination date.
     * <p>
     * This is false by default.
     * 
     * @param date  the date to check
     * @return true if the date is the maturity or termination date
     */
    public default boolean isScheduleEndDate(LocalDate date) {
      return false;
    }

    /**
     * Checks if the end of month convention is in use.
     * <p>
     * This is called when a day count needs to know whether the end-of-month convention is in use.
     * <p>
     * This is true by default.
     * 
     * @return true if the end of month convention is in use
     */
    public default boolean isEndOfMonthConvention() {
      return true;
    }

    /**
     * Gets the end date of the schedule period.
     * <p>
     * This is called when a day count requires the end date of the schedule period.
     * <p>
     * This throws an exception by default.
     * 
     * @return the schedule period end date
     * @throws UnsupportedOperationException if the date cannot be obtained
     */
    public default LocalDate getEndDate() {
      throw new UnsupportedOperationException("The end date of the schedule period is required");
    }

    /**
     * Gets the periodic frequency of the schedule period.
     * <p>
     * This is called when a day count requires the periodic frequency of the schedule.
     * <p>
     * This throws an exception by default.
     * 
     * @return the periodic frequency
     * @throws UnsupportedOperationException if the frequency cannot be obtained
     */
    public default Frequency getFrequency() {
      throw new UnsupportedOperationException("The frequency of the schedule is required");
    }

    /**
     * Gets the type of the schedule period.
     * <p>
     * This is called when a day count requires the type of the schedule.
     * <p>
     * This throws an exception by default.
     * 
     * @return the type of the schedule period
     * @throws UnsupportedOperationException if the type cannot be obtained
     */
    public default SchedulePeriodType getType() {
      throw new UnsupportedOperationException("The schedule period type of the schedule is required");
    }
  }

}
