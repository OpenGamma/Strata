/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * A convention defining how to roll dates.
 * <p>
 * The purpose of this convention is to define how to roll dates when building a schedule.
 * The standard approach to building a schedule is based on unadjusted dates, which do not
 * have a {@linkplain BusinessDayConvention business day convention} applied.
 * To get the next date in the schedule, take the base date and the
 * {@linkplain Frequency periodic frequency}. Once this date is calculated,
 * the roll convention is applied to produce the next schedule date.
 * <p>
 * In most cases the specific values for day-of-month and day-of-week are not needed.
 * A one month periodic frequency will naturally select the same day-of-month as the
 * input date, thus the day-of-month does not need to be additionally specified.
 * The {@link RollConventions#IMPLIED IMPLIED} convention can be used to indicate this.
 * <p>
 * The most common implementations are provided as constants on {@link RollConventions}.
 * Additional implementations may be added by implementing this interface.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RollConvention
    extends DateAdjuster {

  /**
   * Obtains a {@code RollConvention} from a unique name.
   *
   * @param name  the unique name
   * @return the roll convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static RollConvention of(String name) {
    ArgChecker.notNull(name, "name");
    return Stream.of(RollConventions.Standard.values())
        .filter(convention -> convention.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
  }

  /**
   * Obtains an instance from the day-of-month.
   * <p>
   * This convention will adjust the input date to the specified day-of-month.
   * The year and month of the result date will be the same as the input date.
   * It is intended for use with periods that are a multiple of months.
   * <p>
   * If the month being adjusted has a length less than the requested day-of-month
   * then the last valid day-of-month will be chosen. As such, passing 31 to this
   * method is equivalent to selecting the end-of-month convention.
   * 
   * @param dayOfMonth  the day-of-month, from 1 to 31
   * @return the roll convention
   * @throws IllegalArgumentException if the day-of-month is invalid
   */
  public static RollConvention ofDayOfMonth(int dayOfMonth) {
    return RollConventions.Dom.of(dayOfMonth);
  }

  /**
   * Obtains an instance from the day-of-week.
   * <p>
   * This convention will adjust the input date to the specified day-of-week.
   * It is intended for use with periods that are a multiple of weeks.
   * <p>
   * In {@code adjust()}, if the input date is not the required day-of-week,
   * then the next occurrence of the day-of-week is selected, up to 6 days later.
   * <p>
   * In {@code next()}, the day-of-week is selected after the frequency is added.
   * If the calculated date is not the required day-of-week, then the next occurrence
   * of the day-of-week is selected, up to 6 days later.
   * <p>
   * In {@code previous()}, the day-of-week is selected after the frequency is subtracted.
   * If the calculated date is not the required day-of-week, then the previous occurrence
   * of the day-of-week is selected, up to 6 days earlier.
   * 
   * @param dayOfWeek  the day-of-week
   * @return the roll convention
   * @throws IllegalArgumentException if the day-of-month is invalid
   */
  public static RollConvention ofDayOfWeek(DayOfWeek dayOfWeek) {
    return RollConventions.Dow.of(dayOfWeek);
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date according to the rules of the roll convention.
   * <p>
   * See the description of each roll convention to understand the rule applied.
   * <p>
   * It is recommended to use {@code next()} and {@code previous()} rather than
   * directly using this method.
   * 
   * @param date  the date to adjust
   * @return the adjusted temporal
   */
  @Override
  public LocalDate adjust(LocalDate date);

  //-------------------------------------------------------------------------
  /**
   * Implies the correct convention if necessary.
   * <p>
   * In most cases, calling this method has no effect, returning {@code this}.
   * In the case of the {@link RollConventions#IMPLIED IMPLIED} convention, a different
   * convention is returned, implied from the date and frequency.
   * <p>
   * The default implementation returns {@code this} and should not normally be overridden.
   * 
   * @param date  the date to adjust
   * @param periodicFrequency  the period to add or subtract
   * @return the adjusted date
   */
  public default RollConvention imply(LocalDate date, Frequency periodicFrequency) {
    ArgChecker.notNull(date, "inputDate");
    ArgChecker.notNull(periodicFrequency, "periodicFrequency");
    return this;
  }

  /**
   * Calculates the next date in the sequence after the input date.
   * <p>
   * This takes the input date, adds the periodic frequency and adjusts the date
   * as necessary to match the roll convention rules.
   * The result will always be after the input date.
   * <p>
   * The default implementation is suitable for month-based conventions.
   * 
   * @param date  the date to adjust
   * @param periodicFrequency  the period to add or subtract
   * @return the adjusted date
   * @throws IllegalStateException if the convention needs to be implied
   */
  public default LocalDate next(LocalDate date, Frequency periodicFrequency) {
    ArgChecker.notNull(date, "date");
    ArgChecker.notNull(periodicFrequency, "periodicFrequency");
    LocalDate calculated = adjust(date.plus(periodicFrequency));
    if (calculated.isAfter(date) == false) {
      calculated = adjust(date.plusMonths(1));
    }
    return calculated;
  }

  /**
   * Calculates the previous date in the sequence after the input date.
   * <p>
   * This takes the input date, subtracts the periodic frequency and adjusts the date
   * as necessary to match the roll convention rules.
   * The result will always be before the input date.
   * <p>
   * The default implementation is suitable for month-based conventions.
   * 
   * @param date  the date to adjust
   * @param periodicFrequency  the period to add or subtract
   * @return the adjusted date
   * @throws IllegalStateException if the convention needs to be implied
   */
  public default LocalDate previous(LocalDate date, Frequency periodicFrequency) {
    ArgChecker.notNull(date, "date");
    ArgChecker.notNull(periodicFrequency, "periodicFrequency");
    LocalDate calculated = adjust(date.minus(periodicFrequency));
    if (calculated.isBefore(date) == false) {
      calculated = adjust(date.minusMonths(1));
    }
    return calculated;
  }

  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  public String getName();

}
