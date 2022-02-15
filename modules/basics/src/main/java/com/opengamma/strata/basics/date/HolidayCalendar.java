/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.LocalDateUtils.plusDays;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.Stream;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * A holiday calendar, classifying dates as holidays or business days.
 * <p>
 * Many calculations in finance require knowledge of whether a date is a business day or not.
 * This class encapsulates that knowledge, with each day treated as a holiday or a business day.
 * Weekends are effectively treated as a special kind of holiday.
 * <p>
 * Applications should refer to holidays using {@link HolidayCalendarId}.
 * The identifier must be {@linkplain HolidayCalendarId#resolve(ReferenceData) resolved}
 * to a {@link HolidayCalendar} before the holiday data methods can be accessed.
 * See {@link HolidayCalendarIds} for a standard set of identifiers available in {@link ReferenceData#standard()}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 * 
 * @see ImmutableHolidayCalendar
 */
public interface HolidayCalendar
    extends Named {

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
   * If the amount is zero, the input date is returned.
   * If the amount is positive, later business days are chosen.
   * If the amount is negative, earlier business days are chosen.
   * 
   * @param date  the date to adjust
   * @param amount  the number of business days to adjust by
   * @return the shifted date
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate shift(LocalDate date, int amount) {
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
   * Finds the next business day, always returning a later date.
   * <p>
   * Given a date, this method returns the next business day.
   * 
   * @param date  the date to adjust
   * @return the first business day after the input date
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate next(LocalDate date) {
    LocalDate next = plusDays(date, 1);
    return isHoliday(next) ? next(next) : next;
  }

  /**
   * Finds the next business day, returning the input date if it is a business day.
   * <p>
   * Given a date, this method returns a business day.
   * If the input date is a business day, it is returned.
   * Otherwise, the next business day is returned.
   * 
   * @param date  the date to adjust
   * @return the input date if it is a business day, or the next business day
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate nextOrSame(LocalDate date) {
    return isHoliday(date) ? next(date) : date;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the previous business day, always returning an earlier date.
   * <p>
   * Given a date, this method returns the previous business day.
   * 
   * @param date  the date to adjust
   * @return the first business day before the input date
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate previous(LocalDate date) {
    LocalDate previous = plusDays(date, -1);
    return isHoliday(previous) ? previous(previous) : previous;
  }

  /**
   * Finds the previous business day, returning the input date if it is a business day.
   * <p>
   * Given a date, this method returns a business day.
   * If the input date is a business day, it is returned.
   * Otherwise, the previous business day is returned.
   * 
   * @param date  the date to adjust
   * @return the input date if it is a business day, or the previous business day
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate previousOrSame(LocalDate date) {
    return isHoliday(date) ? previous(date) : date;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the next business day within the month, returning the input date if it is a business day,
   * or the last business day of the month if the next business day is in a different month.
   * <p>
   * Given a date, this method returns a business day.
   * If the input date is a business day, it is returned.
   * If the next business day is within the same month, it is returned.
   * Otherwise, the last business day of the month is returned.
   * <p>
   * Note that the result of this method may be earlier than the input date.
   * <p>
   * This corresponds to the {@linkplain BusinessDayConventions#MODIFIED_FOLLOWING modified following}
   * business day convention.
   * 
   * @param date  the date to adjust
   * @return the input date if it is a business day, the next business day if within the same month
   *   or the last business day of the month
   * @throws IllegalArgumentException if the calculation is outside the supported range
   */
  public default LocalDate nextSameOrLastInMonth(LocalDate date) {
    LocalDate nextOrSame = nextOrSame(date);
    return (nextOrSame.getMonthValue() != date.getMonthValue() ? previous(date) : nextOrSame);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the specified date is the last business day of the month.
   * <p>
   * This returns true if the date specified is the last valid business day of the month.
   * 
   * @param date  the date to check
   * @return true if the specified date is the last business day of the month
   * @throws IllegalArgumentException if the date is outside the supported range
   */
  public default boolean isLastBusinessDayOfMonth(LocalDate date) {
    return isBusinessDay(date) && next(date).getMonthValue() != date.getMonthValue();
  }

  /**
   * Calculates the last business day of the month.
   * <p>
   * Given a date, this method returns the date of the last business day of the month.
   * 
   * @param date  the date to check
   * @return the date of the last business day of the month
   * @throws IllegalArgumentException if the date is outside the supported range
   */
  public default LocalDate lastBusinessDayOfMonth(LocalDate date) {
    return previousOrSame(date.withDayOfMonth(date.lengthOfMonth()));
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
   * @throws IllegalArgumentException if either date is outside the supported range
   */
  public default int daysBetween(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive");
    return Math.toIntExact(LocalDateUtils.stream(startInclusive, endExclusive)
        .filter(this::isBusinessDay)
        .count());
  }

  /**
   * Gets the stream of business days between the two dates.
   * <p>
   * This method will treat weekends as holidays.
   * If the dates are equal, an empty stream is returned.
   * If the end is before the start, an exception is thrown.
   * 
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the stream of business days
   * @throws IllegalArgumentException if either date is outside the supported range
   */
  public default Stream<LocalDate> businessDays(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive");
    return LocalDateUtils.stream(startInclusive, endExclusive)
        .filter(this::isBusinessDay);
  }

  /**
   * Gets the stream of holidays between the two dates.
   * <p>
   * This method will treat weekends as holidays.
   * If the dates are equal, an empty stream is returned.
   * If the end is before the start, an exception is thrown.
   * 
   * @param startInclusive  the start date
   * @param endExclusive  the end date
   * @return the stream of holidays
   * @throws IllegalArgumentException if either date is outside the supported range
   */
  public default Stream<LocalDate> holidays(LocalDate startInclusive, LocalDate endExclusive) {
    ArgChecker.inOrderOrEqual(startInclusive, endExclusive, "startInclusive", "endExclusive");
    return LocalDateUtils.stream(startInclusive, endExclusive)
        .filter(this::isHoliday);
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
  public default HolidayCalendar combinedWith(HolidayCalendar other) {
    if (this.equals(other)) {
      return this;
    }
    if (other == HolidayCalendars.NO_HOLIDAYS) {
      return this;
    }
    return new CombinedHolidayCalendar(this, other);
  }

  /**
   * Combines this holiday calendar with another.
   * <p>
   * The resulting calendar will declare a day as a business day if it is a
   * business day in either source calendar.
   * 
   * @param other  the other holiday calendar
   * @return the combined calendar
   */
  public default HolidayCalendar linkedWith(HolidayCalendar other) {
    if (this.equals(other)) {
      return this;
    }
    if (this == HolidayCalendars.NO_HOLIDAYS || other == HolidayCalendars.NO_HOLIDAYS) {
      return HolidayCalendars.NO_HOLIDAYS;
    }
    return new LinkedHolidayCalendar(this, other);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the identifier for the calendar.
   * <p>
   * This identifier is used to locate the index in {@link ReferenceData}.
   * 
   * @return the identifier
   */
  public abstract HolidayCalendarId getId();

  //-------------------------------------------------------------------------
  /**
   * Gets the name that identifies this calendar.
   * <p>
   * This is the name associated with the {@linkplain HolidayCalendarId identifier}.
   * 
   * @return the name
   */
  @Override
  public default String getName() {
    return getId().getName();
  }

}
