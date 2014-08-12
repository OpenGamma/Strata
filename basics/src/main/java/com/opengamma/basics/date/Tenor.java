/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * A tenor indicating how long it will take for a financial instrument to reach maturity.
 * <p>
 * A tenor is allowed to be any non-negative period of days, weeks, month or years.
 * This class provides constants for common tenors which are best used by static import.
 * 
 * <h4>Usage</h4>
 * {@code Tenor} implements {@code TemporalAmount} allowing it to be directly added to a date:
 * <pre>
 *  LocalDate later = baseDate.plus(tenor);
 * </pre>
 */
public final class Tenor
    implements TemporalAmount, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1;

  /**
   * A tenor of one day.
   */
  public static final Tenor TENOR_1D = ofDays(1);
  /**
   * A tenor of two days.
   */
  public static final Tenor TENOR_2D = ofDays(2);
  /**
   * A tenor of three days.
   */
  public static final Tenor TENOR_3D = ofDays(3);
  /**
   * A tenor of 1 week.
   */
  public static final Tenor TENOR_1W = ofWeeks(1);
  /**
   * A tenor of 2 weeks.
   */
  public static final Tenor TENOR_2W = ofWeeks(2);
  /**
   * A tenor of 3 weeks.
   */
  public static final Tenor TENOR_3W = ofWeeks(3);
  /**
   * A tenor of 6 weeks.
   */
  public static final Tenor TENOR_6W = ofWeeks(6);
  /**
   * A tenor of 1 month.
   */
  public static final Tenor TENOR_1M = ofMonths(1);
  /**
   * A tenor of 2 months.
   */
  public static final Tenor TENOR_2M = ofMonths(2);
  /**
   * A tenor of 3 months.
   */
  public static final Tenor TENOR_3M = ofMonths(3);
  /**
   * A tenor of 4 months.
   */
  public static final Tenor TENOR_4M = ofMonths(4);
  /**
   * A tenor of 5 months.
   */
  public static final Tenor TENOR_5M = ofMonths(5);
  /**
   * A tenor of 6 months.
   */
  public static final Tenor TENOR_6M = ofMonths(6);
  /**
   * A tenor of 7 months.
   */
  public static final Tenor TENOR_7M = ofMonths(7);
  /**
   * A tenor of 8 months.
   */
  public static final Tenor TENOR_8M = ofMonths(8);
  /**
   * A tenor of 9 months.
   */
  public static final Tenor TENOR_9M = ofMonths(9);
  /**
   * A tenor of 10 months.
   */
  public static final Tenor TENOR_10M = ofMonths(10);
  /**
   * A tenor of 11 months.
   */
  public static final Tenor TENOR_11M = ofMonths(11);
  /**
   * A tenor of 12 months.
   */
  public static final Tenor TENOR_12M = ofMonths(12);
  /**
   * A tenor of 18 months.
   */
  public static final Tenor TENOR_18M = ofMonths(18);
  /**
   * A tenor of 1 year.
   */
  public static final Tenor TENOR_1Y = ofYears(1);
  /**
   * A tenor of 2 years.
   */
  public static final Tenor TENOR_2Y = ofYears(2);
  /**
   * A tenor of 3 years.
   */
  public static final Tenor TENOR_3Y = ofYears(3);
  /**
   * A tenor of 4 years.
   */
  public static final Tenor TENOR_4Y = ofYears(4);
  /**
   * A tenor of 5 years.
   */
  public static final Tenor TENOR_5Y = ofYears(5);
  /**
   * A tenor of 6 years.
   */
  public static final Tenor TENOR_6Y = ofYears(6);
  /**
   * A tenor of 7 years.
   */
  public static final Tenor TENOR_7Y = ofYears(7);
  /**
   * A tenor of 8 years.
   */
  public static final Tenor TENOR_8Y = ofYears(8);
  /**
   * A tenor of 9 years.
   */
  public static final Tenor TENOR_9Y = ofYears(9);
  /**
   * A tenor of 10 years.
   */
  public static final Tenor TENOR_10Y = ofYears(10);
  /**
   * A tenor of 12 years.
   */
  public static final Tenor TENOR_12Y = ofYears(12);
  /**
   * A tenor of 15 years.
   */
  public static final Tenor TENOR_15Y = ofYears(15);
  /**
   * A tenor of 20 years.
   */
  public static final Tenor TENOR_20Y = ofYears(20);
  /**
   * A tenor of 25 years.
   */
  public static final Tenor TENOR_25Y = ofYears(25);
  /**
   * A tenor of 30 years.
   */
  public static final Tenor TENOR_30Y = ofYears(30);

  /**
   * The period of the tenor.
   */
  private final Period period;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code Tenor} from a {@code Period}.
   * <p>
   * The period normally consists of either days and weeks, or months and years.
   * It must also be positive and non-zero.
   *
   * @param period  the period to convert to a tenor
   * @return the tenor
   * @throws IllegalArgumentException if the period is negative
   */
  public static Tenor of(Period period) {
    return new Tenor(period);
  }

  /**
   * Returns a tenor backed by a period of days.
   *
   * @param days  the number of days
   * @return the tenor
   * @throws IllegalArgumentException if the period is negative
   */
  public static Tenor ofDays(int days) {
    return of(Period.ofDays(days));
  }

  /**
   * Returns a tenor backed by a period of weeks.
   *
   * @param weeks  the number of weeks
   * @return the tenor
   * @throws IllegalArgumentException if the period is negative
   */
  public static Tenor ofWeeks(int weeks) {
    return of(Period.ofWeeks(weeks));
  }

  /**
   * Returns a tenor backed by a period of months.
   *
   * @param months  the number of months
   * @return the tenor
   * @throws IllegalArgumentException if the period is negative
   */
  public static Tenor ofMonths(int months) {
    return of(Period.ofMonths(months));
  }

  /**
   * Returns a tenor backed by a period of years.
   *
   * @param years  the number of years
   * @return the tenor
   * @throws IllegalArgumentException if the period is negative
   */
  public static Tenor ofYears(int years) {
    return of(Period.ofYears(years));
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a formatted string representing the tenor.
   * <p>
   * The format can either be based on ISO-8601, such as 'P3M'
   * or without the 'P' prefix e.g. '2W'.
   *
   * @param toParse  the string representing the tenor
   * @return the tenor
   * @throws IllegalArgumentException if the tenor cannot be parsed
   */
  @FromString
  public static Tenor parse(String toParse) {
    ArgChecker.notNull(toParse, "toParse");
    String prefixed = toParse.startsWith("P") ? toParse : "P" + toParse;
    try {
      return Tenor.of(Period.parse(prefixed));
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a tenor.
   *
   * @param period  the period to represent
   */
  private Tenor(Period period) {
    ArgChecker.notNull(period, "period");
    ArgChecker.isFalse(period.isNegative(), "Period must not be negative");
    this.period = period;
  }

  // safe deserialization
  private Object readResolve() {
    return new Tenor(period);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying period of the tenor.
   *
   * @return the period
   */
  public Period getPeriod() {
    return period;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value of the specified unit.
   * <p>
   * This will return a value for the years, months and days units.
   * Note that weeks are not included.
   * All other units throw an exception.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   *
   * @param unit  the unit to query
   * @return the value of the unit
   * @throws UnsupportedTemporalTypeException if the unit is not supported
   */
  @Override
  public long get(TemporalUnit unit) {
    return period.get(unit);
  }

  /**
   * Gets the units supported by a tenor.
   * <p>
   * This returns a list containing years, months and days.
   * Note that weeks are not included.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   *
   * @return a list containing the years, months and days units
   */
  @Override
  public List<TemporalUnit> getUnits() {
    return period.getUnits();
  }

  /**
   * Adds this tenor to the specified date.
   * <p>
   * This is an implementation method used by {@link LocalDate#plus(TemporalAmount)}.
   * See {@link Period#addTo(Temporal)} for more details.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   * Use {@link LocalDate#plus(TemporalAmount)} instead.
   *
   * @param temporal  the temporal object to add to
   * @return the result with this tenor added
   * @throws DateTimeException if unable to add
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public Temporal addTo(Temporal temporal) {
    return period.addTo(temporal);
  }

  /**
   * Subtracts this tenor from the specified date.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   * Use {@link LocalDate#minus(TemporalAmount)} instead.
   *
   * @param temporal  the temporal object to subtract from
   * @return the result with this tenor subtracted
   * @throws DateTimeException if unable to subtract
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public Temporal subtractFrom(Temporal temporal) {
    return period.subtractFrom(temporal);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this tenor equals another tenor.
   * <p>
   * The comparison checks the tenor period.
   * 
   * @param obj  the other tenor, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Tenor)) {
      return false;
    }
    return period.equals(((Tenor) obj).period);
  }

  /**
   * Returns a suitable hash code for the tenor.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return period.hashCode();
  }

  /**
   * Returns a formatted string representing the tenor.
   * <p>
   * The format is a combination of the quantity and unit, such as 1D, 2W, 3M, 4Y.
   *
   * @return the formatted tenor
   */
  @ToString
  @Override
  public String toString() {
    return representsWeeks() ?
        (period.getDays() / 7) + "W" :
        period.toString().substring(1);
  }

  // Does this period represent an exact number of weeks
  private boolean representsWeeks() {
    return period.getDays() > 0 && period.getDays() % 7 == 0;
  }

}
