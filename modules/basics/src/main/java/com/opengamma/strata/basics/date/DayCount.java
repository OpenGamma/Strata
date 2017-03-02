/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

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
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the day count
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static DayCount of(String uniqueName) {
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Obtains an instance of the 'Bus/252' day count based on a specific calendar.
   * <p>
   * The 'Bus/252' day count is unusual in that it relies on a specific holiday calendar.
   * The calendar is stored within the day count.
   * <p>
   * To avoid widespread complexity in the system, the holiday calendar associated
   * with 'Bus/252' holiday calendars is looked up using the
   * {@linkplain ReferenceData#standard() standard reference data}.
   * <p>
   * This day count is typically used in Brazil.
   * 
   * @param calendar  the holiday calendar
   * @return the day count
   */
  public static DayCount ofBus252(HolidayCalendarId calendar) {
    return Business252DayCount.INSTANCE.of(calendar.resolve(ReferenceData.standard()));
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the day count to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<DayCount> extendedEnum() {
    return DayCounts.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the year fraction between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention. The dates must be in order.
   * <p>
   * This uses a simple {@link ScheduleInfo} which has the end-of-month convention
   * set to true, but throws an exception for other methods.
   * Certain implementations of {@code DayCount} need the missing information,
   * and thus will throw an exception.
   * 
   * @param firstDate  the first date
   * @param secondDate  the second date, on or after the first date
   * @return the year fraction
   * @throws IllegalArgumentException if the dates are not in order
   * @throws UnsupportedOperationException if the year fraction cannot be obtained
   */
  public default double yearFraction(LocalDate firstDate, LocalDate secondDate) {
    return yearFraction(firstDate, secondDate, DayCounts.SIMPLE_SCHEDULE_INFO);
  }

  /**
   * Gets the year fraction between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention. The dates must be in order.
   * 
   * @param firstDate  the first date
   * @param secondDate  the second date, on or after the first date
   * @param scheduleInfo  the schedule information
   * @return the year fraction, zero or greater
   * @throws IllegalArgumentException if the dates are not in order
   * @throws UnsupportedOperationException if the year fraction cannot be obtained
   */
  public abstract double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo);

  /**
   * Gets the relative year fraction between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   * The result of this method will be negative if the first date is after the second date.
   * The result is calculated using {@link #yearFraction(LocalDate, LocalDate, ScheduleInfo)}.
   * <p>
   * This uses a simple {@link ScheduleInfo} which has the end-of-month convention
   * set to true, but throws an exception for other methods.
   * Certain implementations of {@code DayCount} need the missing information,
   * and thus will throw an exception.
   * 
   * @param firstDate  the first date
   * @param secondDate  the second date, which may be before the first date
   * @return the year fraction, may be negative
   * @throws UnsupportedOperationException if the year fraction cannot be obtained
   */
  public default double relativeYearFraction(LocalDate firstDate, LocalDate secondDate) {
    return relativeYearFraction(firstDate, secondDate, DayCounts.SIMPLE_SCHEDULE_INFO);
  }

  /**
   * Gets the relative year fraction between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   * The result of this method will be negative if the first date is after the second date.
   * The result is calculated using {@link #yearFraction(LocalDate, LocalDate, ScheduleInfo)}.
   * 
   * @param firstDate  the first date
   * @param secondDate  the second date, which may be before the first date
   * @param scheduleInfo  the schedule information
   * @return the year fraction, may be negative
   * @throws UnsupportedOperationException if the year fraction cannot be obtained
   */
  public default double relativeYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
    if (secondDate.isBefore(firstDate)) {
      return -yearFraction(secondDate, firstDate, scheduleInfo);
    }
    return yearFraction(firstDate, secondDate, scheduleInfo);
  }

  /**
   * Calculates the number of days between the specified dates using the rules of this day count.
   * <p>
   * A day count is typically defines as a count of days divided by a year estimate.
   * This method returns the count of days, which is the numerator of the division.
   * For example, the 'Act/Act' day count will return the actual number of days between
   * the two dates, but the '30/360 ISDA' will return a value based on 30 day months.
   * 
   * @param firstDate  the first date
   * @param secondDate  the second date, which may be before the first date
   * @return the number of days, as determined by the day count
   */
  public abstract int days(LocalDate firstDate, LocalDate secondDate);

  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

  //-------------------------------------------------------------------------
  /**
   * Information about the schedule necessary to calculate the day count.
   * <p>
   * Some {@link DayCount} implementations require additional information about the schedule.
   * Implementations of this interface provide that information.
   */
  public interface ScheduleInfo {

    /**
     * Gets the start date of the schedule.
     * <p>
     * The first date in the schedule.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * <p>
     * This throws an exception by default.
     * 
     * @return the schedule start date
     * @throws UnsupportedOperationException if the date cannot be obtained
     */
    public default LocalDate getStartDate() {
      throw new UnsupportedOperationException("The start date of the schedule is required");
    }

    /**
     * Gets the end date of the schedule.
     * <p>
     * The last date in the schedule.
     * If the schedule adjusts for business days, then this is the adjusted date.
     * <p>
     * This throws an exception by default.
     * 
     * @return the schedule end date
     * @throws UnsupportedOperationException if the date cannot be obtained
     */
    public default LocalDate getEndDate() {
      throw new UnsupportedOperationException("The end date of the schedule is required");
    }

    /**
     * Gets the end date of the schedule period.
     * <p>
     * This is called when a day count requires the end date of the schedule period.
     * <p>
     * This throws an exception by default.
     * 
     * @param date  the date to find the period end date for
     * @return the period end date
     * @throws UnsupportedOperationException if the date cannot be obtained
     */
    public default LocalDate getPeriodEndDate(LocalDate date) {
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
  }

}
