/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.io.Serializable;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * A tenor indicating how long it will take for a financial
 * instrument to reach maturity.
 */
public class Tenor implements Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1;

  /**
   * A tenor of one day.
   */
  public static final Tenor DAY = ofDays(1);

  /**
   * A tenor of one day.
   */
  public static final Tenor ONE_DAY = DAY;

  /**
   * A tenor of two days.
   */
  public static final Tenor TWO_DAYS = ofDays(2);

  /**
   * A tenor of two days.
   */
  public static final Tenor THREE_DAYS = ofDays(3);

  /**
   * A tenor of 1 week.
   */
  public static final Tenor ONE_WEEK = ofWeeks(1);

  /**
   * A tenor of 2 weeks.
   */
  public static final Tenor TWO_WEEKS = ofWeeks(2);

  /**
   * A tenor of 3 weeks.
   */
  public static final Tenor THREE_WEEKS = ofWeeks(3);

  /**
   * A tenor of 6 weeks.
   */
  public static final Tenor SIX_WEEKS = ofWeeks(6);

  /**
   * A tenor of 1 month.
   */
  public static final Tenor ONE_MONTH = ofMonths(1);

  /**
   * A tenor of 2 months.
   */
  public static final Tenor TWO_MONTHS = ofMonths(2);

  /**
   * A tenor of 3 months.
   */
  public static final Tenor THREE_MONTHS = ofMonths(3);

  /**
   * A tenor of 4 months.
   */
  public static final Tenor FOUR_MONTHS = ofMonths(4);

  /**
   * A tenor of 5 months.
   */
  public static final Tenor FIVE_MONTHS = ofMonths(5);

  /**
   * A tenor of 6 months.
   */
  public static final Tenor SIX_MONTHS = ofMonths(6);

  /**
   * A tenor of 7 months.
   */
  public static final Tenor SEVEN_MONTHS = ofMonths(7);

  /**
   * A tenor of 8 months.
   */
  public static final Tenor EIGHT_MONTHS = ofMonths(8);

  /**
   * A tenor of 9 months.
   */
  public static final Tenor NINE_MONTHS = ofMonths(9);

  /**
   * A tenor of 10 months.
   */
  public static final Tenor TEN_MONTHS = ofMonths(10);

  /**
   * A tenor of 11 months.
   */
  public static final Tenor ELEVEN_MONTHS = ofMonths(11);

  /**
   * A tenor of 12 months.
   */
  public static final Tenor TWELVE_MONTHS = ofMonths(12);

  /**
   * A tenor of 18 months.
   */
  public static final Tenor EIGHTEEN_MONTHS = ofMonths(18);

  /**
   * A tenor of 1 year.
   */
  public static final Tenor ONE_YEAR = ofYears(1);

  /**
   * A tenor of 2 years.
   */
  public static final Tenor TWO_YEARS = ofYears(2);

  /**
   * A tenor of 3 years.
   */
  public static final Tenor THREE_YEARS = ofYears(3);

  /**
   * A tenor of 4 years.
   */
  public static final Tenor FOUR_YEARS = ofYears(4);

  /**
   * A tenor of 5 years.
   */
  public static final Tenor FIVE_YEARS = ofYears(5);

  /**
   * A tenor of 6 years.
   */
  public static final Tenor SIX_YEARS = ofYears(6);

  /**
   * A tenor of 7 years.
   */
  public static final Tenor SEVEN_YEARS = ofYears(7);

  /**
   * A tenor of 8 years.
   */
  public static final Tenor EIGHT_YEARS = ofYears(8);

  /**
   * A tenor of 9 years.
   */
  public static final Tenor NINE_YEARS = ofYears(9);

  /**
   * A tenor of 10 years.
   */
  public static final Tenor TEN_YEARS = ofYears(10);

  /**
   * The period of the tenor.
   */
  private final Period period;

  /**
   * Obtains a {@code Tenor} from a {@code Period}.
   *
   * @param period  the period to convert to a tenor
   * @return the tenor
   */
  public static Tenor of(Period period) {
    return new Tenor(period);
  }

  /**
   * Parses a formatted string representing the tenor.
   * <p>
   * The format is based on ISO-8601, such as 'P3M'.
   *
   * @param toParse  the string representing the tenor, not null
   * @return the tenor, not null
   */
  @FromString
  public static Tenor parse(String toParse) {
    return new Tenor(Period.parse(ArgChecker.notNull(toParse, "toParse")));
  }

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

  /**
   * Gets the tenor period.
   *
   * @return the period
   */
  public Period getPeriod() {
    return period;
  }

  /**
   * Returns a tenor backed by a period of days.
   *
   * @param days The number of days
   * @return The tenor
   */
  public static Tenor ofDays(int days) {
    return of(Period.ofDays(days));
  }

  /**
   * Returns a tenor backed by a period of weeks.
   *
   * @param weeks The number of weeks
   * @return The tenor
   */
  public static Tenor ofWeeks(int weeks) {
    return of(Period.ofDays(weeks * 7));
  }

  /**
   * Returns a tenor backed by a period of months.
   *
   * @param months The number of months
   * @return The tenor
   */
  public static Tenor ofMonths(int months) {
    return of(Period.ofMonths(months));
  }

  /**
   * Returns a tenor backed by a period of years.
   *
   * @param years The number of years
   * @return The tenor
   */
  public static Tenor ofYears(int years) {
    return of(Period.ofYears(years));
  }

  /**
   * Returns a formatted string representing the tenor.
   * <p>
   * The format is based on ISO-8601, such as 'P3M'.
   *
   * @return the formatted tenor
   */
  @ToString
  public String toFormattedString() {
    return getPeriod().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Tenor)) {
      return false;
    }
    return period.equals(((Tenor) o).period);
  }

  @Override
  public int hashCode() {
    return getPeriod().hashCode();
  }

  @Override
  public String toString() {
    return "Tenor[" + getPeriod().toString() + "]";
  }

}
