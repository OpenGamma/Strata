/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.collect.ArgChecker;

import java.time.LocalDate;
import java.time.Period;

/**
 * Utility for producing sets of CDS dates.
 * These are always quarterly on Mar, Jun, Sep, Dec on the 20th of each month
 */
public abstract class CdsDatesLogic {

  private static final int s_roll_day = 20;
  private static final int[] s_qtr_months = new int[]{3, 6, 9, 12};
  private static final int[] s_index_roll_months = new int[]{3, 9};

  /**
   * CDS dates are 20th March, June, September and December
   *
   * @param date the date
   * @return true is date is an CDS date
   */
  public static boolean isCdsDate(final LocalDate date) {
    return date.getDayOfMonth() == s_roll_day && (date.getMonthValue() % 3) == 0;
  }

  /**
   * Index roll dates are 20th March and September
   *
   * @param date the date
   * @return true is date is an CDS date
   */
  public static boolean isIndexRollDate(final LocalDate date) {
    if (date.getDayOfMonth() != s_roll_day) {
      return false;
    }
    final int month = date.getMonthValue();
    return month == s_index_roll_months[0] || month == s_index_roll_months[1];
  }

  /**
   * Get a set of CDS dates fixed periods from an initial CDS date.
   *
   * @param baseCdsDate The base CDS date (all dates are some interval on from this)
   * @param tenors      The periods (typically this would look like 6M, 1Y, 2Y, 3Y, 5Y, 10Y)
   * @return Set of CDS dates
   */
  public static LocalDate[] getCdsDateSet(final LocalDate baseCdsDate, final Period[] tenors) {
    ArgChecker.notNull(baseCdsDate, "baseCdsDate");
    ArgChecker.noNulls(tenors, "tenors");
    final int n = tenors.length;
    ArgChecker.isTrue(isCdsDate(baseCdsDate), "start is not an CDS date");
    final LocalDate[] res = new LocalDate[n];
    for (int i = 0; i < n; i++) {
      res[i] = baseCdsDate.plus(tenors[i]);
    }
    return res;
  }

  /**
   * Get a complete set of CDS dates from some starting CDS date
   *
   * @param startCdsDate The starting CDS date (this will be the first entry)
   * @param size         number of dates
   * @return set of CDS dates
   */
  public static LocalDate[] getCdsDateSet(final LocalDate startCdsDate, final int size) {
    ArgChecker.isTrue(isCdsDate(startCdsDate), "start is not an CDS date");
    final LocalDate[] res = new LocalDate[size];
    res[0] = startCdsDate;
    for (int i = 1; i < size; i++) {
      final int tMonth = res[i - 1].getMonthValue();
      final int tYear = res[i - 1].getYear();
      if (tMonth != 12) {
        res[i] = LocalDate.of(tYear, tMonth + 3, s_roll_day);
      } else {
        res[i] = LocalDate.of(tYear + 1, 3, s_roll_day);
      }
    }
    return res;
  }

  /**
   * CDS dates are 20th March, June, September and December. This returns the next CDS date from the given date - if the date
   * is an CDS date the next CDS date (i.e. 3 months on) is returned.
   *
   * @param date a given date
   * @return the next CDS date
   */
  public static LocalDate getNextCdsDate(final LocalDate date) {

    final int day = date.getDayOfMonth();
    final int month = date.getMonthValue();
    final int year = date.getYear();
    if (month % 3 == 0) { //in an CDS month
      if (day < s_roll_day) {
        return LocalDate.of(year, month, s_roll_day);
      } else {
        if (month != 12) {
          return LocalDate.of(year, month + 3, s_roll_day);
        } else {
          return LocalDate.of(year + 1, s_qtr_months[0], s_roll_day);
        }
      }
    } else {
      return LocalDate.of(year, s_qtr_months[month / 3], s_roll_day);
    }
  }

  /**
   * CDS dates are 20th March, June, September and December. This returns the previous CDS date from the given date - if the date
   * is an CDS date the previous CDS date (i.e. 3 months before) is returned.
   *
   * @param date a given date
   * @return the next CDS date
   */
  public static LocalDate getPrevCdsDate(final LocalDate date) {

    final int day = date.getDayOfMonth();
    final int month = date.getMonthValue();
    final int year = date.getYear();
    if (month % 3 == 0) { //in an CDS month
      if (day > s_roll_day) {
        return LocalDate.of(year, month, s_roll_day);
      } else {
        if (month != 3) {
          return LocalDate.of(year, month - 3, s_roll_day);
        } else {
          return LocalDate.of(year - 1, s_qtr_months[3], s_roll_day);
        }
      }
    } else {
      final int i = month / 3;
      if (i == 0) {
        return LocalDate.of(year - 1, s_qtr_months[3], s_roll_day);
      } else {
        return LocalDate.of(year, s_qtr_months[i - 1], s_roll_day);
      }
    }
  }

  /**
   * Index roll dates  are 20th March and September. This returns the next roll date from the given date - if the date
   * is a roll date the next roll date (i.e. 6 months on) is returned.
   *
   * @param date a given date
   * @return the next Index roll date
   */
  public static LocalDate getNextIndexRollDate(final LocalDate date) {

    final int day = date.getDayOfMonth();
    final int month = date.getMonthValue();
    final int year = date.getYear();
    if (isIndexRollDate(date)) { //on an index roll 
      if (month == s_index_roll_months[0]) {
        return LocalDate.of(year, s_index_roll_months[1], s_roll_day);
      } else {
        return LocalDate.of(year + 1, s_index_roll_months[0], s_roll_day);
      }
    } else {
      if (month < s_index_roll_months[0]) {
        return LocalDate.of(year, s_index_roll_months[0], s_roll_day);
      } else if (month == s_index_roll_months[0]) {
        if (day < s_roll_day) {
          return LocalDate.of(year, month, s_roll_day);
        } else {
          return LocalDate.of(year, s_index_roll_months[1], s_roll_day);
        }
      } else if (month < s_index_roll_months[1]) {
        return LocalDate.of(year, s_index_roll_months[1], s_roll_day);
      } else if (month == s_index_roll_months[1]) {
        if (day < s_roll_day) {
          return LocalDate.of(year, month, s_roll_day);
        } else {
          return LocalDate.of(year + 1, s_index_roll_months[0], s_roll_day);
        }
      } else {
        return LocalDate.of(year + 1, s_index_roll_months[0], s_roll_day);
      }
    }
  }

}
