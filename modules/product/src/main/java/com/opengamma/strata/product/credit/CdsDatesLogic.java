/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Utility for producing sets of CDS dates.
 * These are always quarterly on Mar, Jun, Sep, Dec on the 20th of each month
 */
public final class CdsDatesLogic {

  private static final int ROLL_DAY = 20;
  private static final int[] QUARTER_MONTHS = new int[] {3, 6, 9, 12};
  private static final int[] INDEX_ROLL_MONTHS = new int[] {3, 9};

  /**
   * Restricted constructor.
   */
  private CdsDatesLogic() {
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the specified date is a CDS date.
   * <p>
   * CDS dates are the 20th of March, June, September and December.
   *
   * @param date the date
   * @return true is date is an CDS date
   */
  public static boolean isCdsDate(LocalDate date) {
    return date.getDayOfMonth() == ROLL_DAY && (date.getMonthValue() % 3) == 0;
  }

  /**
   * Checks if the specified date is an index roll date.
   * <p>
   * Index roll dates are the 20th of March and September.
   *
   * @param date the date
   * @return true is date is an CDS date
   */
  public static boolean isIndexRollDate(LocalDate date) {
    if (date.getDayOfMonth() != ROLL_DAY) {
      return false;
    }
    int month = date.getMonthValue();
    return month == INDEX_ROLL_MONTHS[0] || month == INDEX_ROLL_MONTHS[1];
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a set of CDS dates fixed periods from an initial CDS date.
   * <p>
   * The specified date must be a CDS date.
   *
   * @param baseCdsDate  the base CDS date, where all dates are some interval on from this
   * @param tenors  the periods, typically this would look like 6M, 1Y, 2Y, 3Y, 5Y, 10Y
   * @return the list of CDS dates
   */
  public static LocalDate[] getCdsDateSet(LocalDate baseCdsDate, Period[] tenors) {
    // TODO: use lists not arrays?
    ArgChecker.notNull(baseCdsDate, "baseCdsDate");
    ArgChecker.isTrue(isCdsDate(baseCdsDate), "Start date must be a CDS date");
    ArgChecker.noNulls(tenors, "tenors");
    int size = tenors.length;
    LocalDate[] result = new LocalDate[size];
    for (int i = 0; i < size; i++) {
      result[i] = baseCdsDate.plus(tenors[i]);
    }
    return result;
  }

  /**
   * Gets a complete set of CDS dates from some starting CDS date.
   * <p>
   * The specified date will be the first date in the array.
   *
   * @param startCdsDate  the starting CDS date
   * @param size  the number of dates
   * @return the list of CDS dates, including the specified date
   */
  public static LocalDate[] getCdsDateSet(LocalDate startCdsDate, int size) {
    // TODO: use lists not arrays?
    ArgChecker.notNull(startCdsDate, "startCdsDate");
    ArgChecker.isTrue(isCdsDate(startCdsDate), "Start date must be a CDS date");
    LocalDate[] result = new LocalDate[size];
    result[0] = startCdsDate;
    for (int i = 1; i < size; i++) {
      result[i] = result[i - 1].plusMonths(3);
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the next CDS date after the specified date.
   * <p>
   * This returns the next CDS date from the given date.
   * If the date is already a CDS date then the next CDS date is returned, 3 months later.
   * <p>
   * CDS dates are the 20th of March, June, September and December.
   *
   * @param date  the date to start from
   * @return the next CDS date
   */
  public static LocalDate getNextCdsDate(LocalDate date) {
    int year = date.getYear();
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();
    if (month % 3 == 0) { //in a CDS month
      if (day < ROLL_DAY) {
        return LocalDate.of(year, month, ROLL_DAY);
      } else {
        return date.withDayOfMonth(ROLL_DAY).plusMonths(3);
      }
    } else {
      return LocalDate.of(year, QUARTER_MONTHS[month / 3], ROLL_DAY);
    }
  }

  /**
   * Finds the previous CDS date after the specified date.
   * <p>
   * This returns the previous CDS date from the given date.
   * If the date is already a CDS date then the previous CDS date is returned, 3 months earlier.
   * <p>
   * CDS dates are the 20th of March, June, September and December.
   *
   * @param date  the date to start from
   * @return the previous CDS date
   */
  public static LocalDate getPrevCdsDate(LocalDate date) {
    // TODO: rename to previous
    int year = date.getYear();
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();
    if (month % 3 == 0) { //in a CDS month
      if (day > ROLL_DAY) {
        return LocalDate.of(year, month, ROLL_DAY);
      } else {
        return date.withDayOfMonth(ROLL_DAY).minusMonths(3);
      }
    } else {
      return LocalDate.of(year, QUARTER_MONTHS[month / 3], ROLL_DAY).minusMonths(3);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the next CDS index roll date after the specified date.
   * <p>
   * This returns the next CDS index roll date from the given date.
   * If the date is already a CDS index roll date then the next date is returned, 6 months later.
   * <p>
   * CDS index roll dates are the 20th of March and September.
   *
   * @param date  the date to start from
   * @return the next CDS index roll date
   */
  public static LocalDate getNextIndexRollDate(LocalDate date) {
    int year = date.getYear();
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();
    if (isIndexRollDate(date)) { // on an index roll 
      return date.plusMonths(6);
    } else {
      if (month < INDEX_ROLL_MONTHS[0]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[0], ROLL_DAY);
      } else if (month == INDEX_ROLL_MONTHS[0]) {
        if (day < ROLL_DAY) {
          return LocalDate.of(year, month, ROLL_DAY);
        } else {
          return LocalDate.of(year, INDEX_ROLL_MONTHS[1], ROLL_DAY);
        }
      } else if (month < INDEX_ROLL_MONTHS[1]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[1], ROLL_DAY);
      } else if (month == INDEX_ROLL_MONTHS[1]) {
        if (day < ROLL_DAY) {
          return LocalDate.of(year, month, ROLL_DAY);
        } else {
          return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], ROLL_DAY);
        }
      } else {
        return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], ROLL_DAY);
      }
    }
  }

}
