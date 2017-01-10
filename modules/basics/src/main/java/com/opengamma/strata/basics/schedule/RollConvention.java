/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.schedule.DayRollConventions.Dom;
import com.opengamma.strata.basics.schedule.DayRollConventions.Dow;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * A convention defining how to roll dates.
 * <p>
 * A {@linkplain PeriodicSchedule periodic schedule} is determined using a periodic frequency.
 * When applying the frequency, the roll convention is used to fine tune the dates.
 * This might involve selecting the last day of the month, or the third Wednesday.
 * <p>
 * To get the next date in the schedule, take the base date and the
 * {@linkplain Frequency periodic frequency}. Once this date is calculated,
 * the roll convention is applied to produce the next schedule date.
 * <p>
 * The most common implementations are provided as constants on {@link RollConventions}.
 * Additional implementations may be added by implementing this interface.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RollConvention
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the roll convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static RollConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
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
    return Dom.of(dayOfMonth);
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
   */
  public static RollConvention ofDayOfWeek(DayOfWeek dayOfWeek) {
    return Dow.of(dayOfWeek);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<RollConvention> extendedEnum() {
    return RollConventions.ENUM_LOOKUP;
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
  public abstract LocalDate adjust(LocalDate date);

  /**
   * Checks if the date matches the rules of the roll convention.
   * <p>
   * See the description of each roll convention to understand the rule applied.
   * 
   * @param date  the date to check
   * @return true if the date matches this convention
   */
  public default boolean matches(LocalDate date) {
    ArgChecker.notNull(date, "date");
    return date.equals(adjust(date));
  }

  //-------------------------------------------------------------------------
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
   * @param periodicFrequency  the periodic frequency of the schedule
   * @return the adjusted date
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

  //-------------------------------------------------------------------------
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
   * @param periodicFrequency  the periodic frequency of the schedule
   * @return the adjusted date
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

  //-------------------------------------------------------------------------
  /**
   * Gets the day-of-month that the roll convention implies.
   * <p>
   * This extracts the day-of-month for simple roll conventions.
   * The numeric roll conventions will return their day-of-month.
   * The 'EOM' convention will return 31.
   * All other conventions will return zero.
   * 
   * @return the day-of-month that the roll convention implies, zero if not applicable
   */
  public default int getDayOfMonth() {
    return 0;
  }

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

}
