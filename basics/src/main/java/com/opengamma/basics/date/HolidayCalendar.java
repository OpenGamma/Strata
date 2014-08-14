/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.range.LocalDateRange;

/**
 * A holiday calendar, classifying dates as holidays or business days.
 * <p>
 * Many calculations in finance require knowledge of whether a date is a business day or not.
 * This class encapsulates that knowledge, with each day treated as a holiday or a business day.
 * Weekends are effectively treated as a special kind of holiday.
 * <p>
 * The most common implementations are provided in {@link HolidayCalendars}.
 * Additional implementations may be added by implementing this interface.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface HolidayCalendar {

  /**
   * Obtains a {@code HolidayCalendar} from a unique name.
   * <p>
   * The unique name identifies a calendar in an underlying source of calendars.
   * The calendar itself is looked up on demand when required.
   * <p>
   * It is possible to combine two or more calendars using the '+' symbol.
   * For example, 'GBLO+USNY' will combine the separate 'GBLO' and 'USNY' calendars.
   * 
   * @param uniqueName  the unique name of the calendar
   * @return the holiday calendar
   */
  @FromString
  public static HolidayCalendar of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return HolidayCalendars.of(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the specified date is a holiday.
   * <p>
   * This is the opposite of {@link #isBusinessDay(LocalDate)}.
   * A weekend is treated as a holiday.
   * 
   * @param date  the date to check
   * @return true if the specified date is a holiday
   * @throws IllegalArgumentException if the date is outside the supported range
   */
  public abstract boolean isHoliday(LocalDate date);

  /**
   * Checks if the specified date is a business day.
   * <p>
   * This is the opposite of {@link #isHoliday(LocalDate)}.
   * A weekend is treated as a holiday.
   * 
   * @param date  the date to check
   * @return true if the specified date is a business day
   * @throws IllegalArgumentException if the date is outside the supported range
   */
  public default boolean isBusinessDay(LocalDate date) {
    return !isHoliday(date);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an adjuster that changes the date.
   * <p>
   * The adjuster is intended to be used with the method {@link Temporal#with(TemporalAdjuster)}.
   * For example:
   * <pre>
   * threeDaysLater = date.with(businessDays.adjustBy(3));
   * twoDaysEarlier = date.with(businessDays.adjustBy(-2));
   * </pre>
   * 
   * @param amount  the number of business days to adjust by
   * @return the first business day after this one
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default TemporalAdjuster adjustBy(int amount) {
    return TemporalAdjusters.ofDateAdjuster(date -> shift(date, amount));
  }

  //-------------------------------------------------------------------------
  /**
   * Shifts the date by the specified number of business days.
   * <p>
   * If the amount is positive, later business days are chosen.
   * If the amount is negative, earlier business days are chosen.
   * 
   * @param date  the date to adjust
   * @param amount  the number of business days to adjust by
   * @return the shifted date
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate shift(LocalDate date, int amount) {
    ArgChecker.notNull(date, "date");
    LocalDate adjusted = date;
    if (amount > 0) {
      for (int i = 0; i < amount; i++) {
        adjusted = next(adjusted);
      }
    } else if (amount < 0) {
      for (int i = 0; i > amount; i--) {
        adjusted = previous(adjusted);
      }
    }
    return adjusted;
  }

  /**
   * Finds the next business day.
   * <p>
   * Given a date, this method returns the next business day.
   * 
   * @param date  the date to adjust
   * @return the first business day after this one
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate next(LocalDate date) {
    ArgChecker.notNull(date, "date");
    LocalDate next = date.plusDays(1);
    return isHoliday(next) ? next(next) : next;
  }

  /**
   * Finds the previous business day.
   * <p>
   * Given a date, this method returns the previous business day.
   * 
   * @param date  the date to adjust
   * @return the first business day before this one
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate previous(LocalDate date) {
    ArgChecker.notNull(date, "date");
    LocalDate previous = date.minusDays(1);
    return isHoliday(previous) ? previous(previous) : previous;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number of business days between two dates.
   * <p>
   * This calculates the number of business days within the range.
   * If the dates are equal, zero is returned.
   * If the end is before the start, an exception is thrown.
   * 
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the total number of business days between the start and end date
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default int daysBetween(LocalDate startInclusive, LocalDate endExclusive) {
    return daysBetween(LocalDateRange.of(startInclusive, endExclusive));
  }

  /**
   * Calculates the number of business days in a date range.
   * <p>
   * This calculates the number of business days within the range.
   * 
   * @param dateRange  the date range to calculate business days for
   * @return the total number of business days between the start and end date
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default int daysBetween(LocalDateRange dateRange) {
    ArgChecker.notNull(dateRange, "dateRange");
    return Math.toIntExact(dateRange.stream()
        .filter(this::isBusinessDay)
        .count());
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this holiday calendar with another.
   * <p>
   * The resulting calendar will declare a day as a business day if it is a
   * business day in both source calendars.
   * 
   * @param other  the other holiday calendar
   * @return the combined calendar
   * @throws IllegalArgumentException if unable to combine the calendars
   */
  public default HolidayCalendar combineWith(HolidayCalendar other) {
    ArgChecker.notNull(other, "other");
    if (this.equals(other)) {
      return this;
    }
    if (other == HolidayCalendars.NONE) {
      return this;
    }
    return new HolidayCalendars.Combined(this, other);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this calendar.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  public String getName();

}
