/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.LocalDateUtils.plusDays;

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

import com.opengamma.strata.collect.ArgChecker;

/**
 * A tenor indicating how long it will take for a financial instrument to reach maturity.
 * <p>
 * A tenor is allowed to be any non-negative non-zero period of days, weeks, month or years.
 * This class provides constants for common tenors which are best used by static import.
 * <p>
 * Each tenor is based on a {@link Period}. The months and years of the period are not normalized,
 * thus it is possible to have a tenor of 12 months and a different one of 1 year.
 * When used, standard date addition rules apply, thus there is no difference between them.
 * Call {@link #normalized()} to apply normalization.
 * 
 * <h4>Usage</h4>
 * {@code Tenor} implements {@code TemporalAmount} allowing it to be directly added to a date:
 * <pre>
 *  LocalDate later = baseDate.plus(tenor);
 * </pre>
 */
public final class Tenor
    implements Comparable<Tenor>, TemporalAmount, Serializable {

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
   * A tenor of 4 weeks.
   */
  public static final Tenor TENOR_4W = ofWeeks(4);
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
  /**
   * The name of the tenor.
   */
  private final String name;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a {@code Period}.
   * <p>
   * The period normally consists of either days and weeks, or months and years.
   * It must also be positive and non-zero.
   * <p>
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   * Months are not normalized into years.
   *
   * @param period  the period to convert to a tenor
   * @return the tenor
   * @throws IllegalArgumentException if the period is negative or zero
   */
  public static Tenor of(Period period) {
    int days = period.getDays();
    long months = period.toTotalMonths();
    if (months == 0 && days != 0) {
      return ofDays(days);
    }
    return new Tenor(period, period.toString().substring(1));
  }

  /**
   * Obtains an instance backed by a period of days.
   * <p>
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   *
   * @param days  the number of days
   * @return the tenor
   * @throws IllegalArgumentException if days is negative or zero
   */
  public static Tenor ofDays(int days) {
    if (days % 7 == 0) {
      return ofWeeks(days / 7);
    }
    return new Tenor(Period.ofDays(days), days + "D");
  }

  /**
   * Obtains an instance backed by a period of weeks.
   *
   * @param weeks  the number of weeks
   * @return the tenor
   * @throws IllegalArgumentException if weeks is negative or zero
   */
  public static Tenor ofWeeks(int weeks) {
    return new Tenor(Period.ofWeeks(weeks), weeks + "W");
  }

  /**
   * Obtains an instance backed by a period of months.
   * <p>
   * Months are not normalized into years.
   *
   * @param months  the number of months
   * @return the tenor
   * @throws IllegalArgumentException if months is negative or zero
   */
  public static Tenor ofMonths(int months) {
    return new Tenor(Period.ofMonths(months), months + "M");
  }

  /**
   * Obtains an instance backed by a period of years.
   *
   * @param years  the number of years
   * @return the tenor
   * @throws IllegalArgumentException if years is negative or zero
   */
  public static Tenor ofYears(int years) {
    return new Tenor(Period.ofYears(years), years + "Y");
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
   * @param name  the name
   */
  private Tenor(Period period, String name) {
    ArgChecker.notNull(period, "period");
    ArgChecker.isFalse(period.isZero(), "Period must not be zero");
    ArgChecker.isFalse(period.isNegative(), "Period must not be negative");
    this.period = period;
    this.name = name;
  }

  // safe deserialization
  private Object readResolve() {
    return new Tenor(period, name);
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
   * Normalizes the months and years of this tenor.
   * <p>
   * This method returns a normalized tenor of an equivalent length.
   * If the period is exactly 1 year then the result will be expressed as 12 months.
   * Otherwise, the result will be expressed using {@link Period#normalized()}.
   *
   * @return the normalized tenor
   */
  public Tenor normalized() {
    if (period.getDays() == 0 && period.toTotalMonths() == 12) {
      return TENOR_12M;
    }
    Period norm = period.normalized();
    return (norm != period ? Tenor.of(norm) : this);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the tenor is week-based.
   * <p>
   * A week-based tenor consists of an integral number of weeks.
   * There must be no day, month or year element.
   *
   * @return true if this is week-based
   */
  public boolean isWeekBased() {
    return period.toTotalMonths() == 0 && period.getDays() % 7 == 0;
  }

  /**
   * Checks if the tenor is month-based.
   * <p>
   * A month-based tenor consists of an integral number of months.
   * Any year-based tenor is also counted as month-based.
   * There must be no day or week element.
   *
   * @return true if this is month-based
   */
  public boolean isMonthBased() {
    return period.toTotalMonths() > 0 && period.getDays() == 0;
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
    // special case for performance
    if (temporal instanceof LocalDate) {
      LocalDate date = (LocalDate) temporal;
      return plusDays(date.plusMonths(period.toTotalMonths()), period.getDays());
    }
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
    // special case for performance
    if (temporal instanceof LocalDate) {
      LocalDate date = (LocalDate) temporal;
      return plusDays(date.minusMonths(period.toTotalMonths()), -period.getDays());
    }
    return period.subtractFrom(temporal);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this tenor to another tenor.
   * <p>
   * Comparing tenors is a hard problem in general, but for commonly used tenors the outcome is as expected.
   * If the two tenors are both based on days, then comparison is easy.
   * If the two tenors are both based on months/years, then comparison is easy.
   * Otherwise, months are converted to days to form an estimated length in days which is compared.
   * The conversion from months to days divides by 12 and then multiplies by 365.25.
   * <p>
   * The resulting order places:
   * <ul>
   * <li>a 1 month tenor between 30 and 31 days
   * <li>a 2 month tenor between 60 and 61 days
   * <li>a 3 month tenor between 91 and 92 days
   * <li>a 6 month tenor between 182 and 183 days
   * <li>a 1 year tenor between 365 and 366 days
   * </ul>
   * 
   * @param other  the other tenor
   * @return negative if this is less than the other, zero if equal and positive if greater
   */
  @Override
  public int compareTo(Tenor other) {
    int thisDays = this.getPeriod().getDays();
    long thisMonths = this.getPeriod().toTotalMonths();
    int otherDays = other.getPeriod().getDays();
    long otherMonths = other.getPeriod().toTotalMonths();
    // both day-only
    if (thisMonths == 0 && otherMonths == 0) {
      return Integer.compare(thisDays, otherDays);
    }
    // both month-only
    if (thisDays == 0 && otherDays == 0) {
      return Long.compare(thisMonths, otherMonths);
    }
    // complex
    double thisMonthsInDays = (thisMonths / 12d) * 365.25d;
    double otherMonthsInDays = (otherMonths / 12d) * 365.25d;
    return Double.compare(thisDays + thisMonthsInDays, otherDays + otherMonthsInDays);
  }

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
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Tenor other = (Tenor) obj;
    return period.equals(other.period);
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
    return name;
  }

}
