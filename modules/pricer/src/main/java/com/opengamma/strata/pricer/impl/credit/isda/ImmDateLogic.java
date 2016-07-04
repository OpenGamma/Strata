/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Utility for producing sets of IMM dates.
 */
public abstract class ImmDateLogic {

  private static final int IMM_DAY = 20;
  private static final int[] IMM_MONTHS = new int[] {3, 6, 9, 12};
  private static final int[] INDEX_ROLL_MONTHS = new int[] {3, 9};

  /**
   * IMM dates are 20th March, June, September and December.
   * 
   * @param date  the date
   * @return true is date is an IMM date
   */
  public static boolean isIMMDate(LocalDate date) {
    return date.getDayOfMonth() == IMM_DAY && (date.getMonthValue() % 3) == 0;
  }

  /**
   * Index roll dates are 20th March and September.
   * 
   * @param date  the date
   * @return true is date is an IMM date
   */
  public static boolean isIndexRollDate(LocalDate date) {
    if (date.getDayOfMonth() != IMM_DAY) {
      return false;
    }
    int month = date.getMonthValue();
    return month == INDEX_ROLL_MONTHS[0] || month == INDEX_ROLL_MONTHS[1];
  }

  /**
   * Get a set of IMM dates fixed periods from an initial IMM date.
   * 
   * @param baseIMMDate  the base IMM date (all dates are some interval on from this)
   * @param tenors  the periods (typically this would look like 6M, 1Y, 2Y, 3Y, 5Y, 10Y) 
   * @return set of IMM dates 
   */
  public static LocalDate[] getIMMDateSet(LocalDate baseIMMDate, Period[] tenors) {
    ArgChecker.notNull(baseIMMDate, "startIMMDate");
    ArgChecker.noNulls(tenors, "tenors");
    int n = tenors.length;
    ArgChecker.isTrue(isIMMDate(baseIMMDate), "start is not an IMM date");
    LocalDate[] res = new LocalDate[n];
    for (int i = 0; i < n; i++) {
      res[i] = baseIMMDate.plus(tenors[i]);
    }
    return res;
  }

  /**
   * Get a complete set of IMM dates from some starting IMM date.
   * 
   * @param startIMMDate  the starting IMM date (this will be the first entry)
   * @param size  number of dates 
   * @return set of IMM dates
   */
  public static LocalDate[] getIMMDateSet(LocalDate startIMMDate, int size) {
    ArgChecker.isTrue(isIMMDate(startIMMDate), "start is not an IMM date");
    LocalDate[] res = new LocalDate[size];
    res[0] = startIMMDate;
    for (int i = 1; i < size; i++) {
      int tMonth = res[i - 1].getMonthValue();
      int tYear = res[i - 1].getYear();
      if (tMonth != 12) {
        res[i] = LocalDate.of(tYear, tMonth + 3, IMM_DAY);
      } else {
        res[i] = LocalDate.of(tYear + 1, 3, IMM_DAY);
      }
    }
    return res;
  }

  /**
   * IMM dates are 20th March, June, September and December.
   * This returns the next IMM date from the given date - if the date
   * is an IMM date the next IMM date (i.e. 3 months on) is returned.
   * 
   * @param date  a given date
   * @return the next IMM date
   */
  public static LocalDate getNextIMMDate(LocalDate date) {

    int day = date.getDayOfMonth();
    int month = date.getMonthValue();
    int year = date.getYear();
    if (month % 3 == 0) { //in an IMM month
      if (day < IMM_DAY) {
        return LocalDate.of(year, month, IMM_DAY);
      } else {
        if (month != 12) {
          return LocalDate.of(year, month + 3, IMM_DAY);
        } else {
          return LocalDate.of(year + 1, IMM_MONTHS[0], IMM_DAY);
        }
      }
    } else {
      return LocalDate.of(year, IMM_MONTHS[month / 3], IMM_DAY);
    }
  }

  /**
   * IMM dates are 20th March, June, September and December.
   * This returns the previous IMM date from the given date - if the date
   * is an IMM date the previous IMM date (i.e. 3 months before) is returned.
   * 
   * @param date  a given date
   * @return the next IMM date
   */
  public static LocalDate getPrevIMMDate(LocalDate date) {

    int day = date.getDayOfMonth();
    int month = date.getMonthValue();
    int year = date.getYear();
    if (month % 3 == 0) { //in an IMM month
      if (day > IMM_DAY) {
        return LocalDate.of(year, month, IMM_DAY);
      } else {
        if (month != 3) {
          return LocalDate.of(year, month - 3, IMM_DAY);
        } else {
          return LocalDate.of(year - 1, IMM_MONTHS[3], IMM_DAY);
        }
      }
    } else {
      int i = month / 3;
      if (i == 0) {
        return LocalDate.of(year - 1, IMM_MONTHS[3], IMM_DAY);
      } else {
        return LocalDate.of(year, IMM_MONTHS[i - 1], IMM_DAY);
      }
    }
  }

  /**
   * Index roll dates  are 20th March and September. This returns the next roll date from the
   * given date - if the date is a roll date the next roll date (i.e. 6 months on) is returned.
   * 
   * @param date  a given date
   * @return the next Index roll date
   */
  public static LocalDate getNextIndexRollDate(LocalDate date) {

    int day = date.getDayOfMonth();
    int month = date.getMonthValue();
    int year = date.getYear();
    if (isIndexRollDate(date)) { //on an index roll 
      if (month == INDEX_ROLL_MONTHS[0]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[1], IMM_DAY);
      } else {
        return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], IMM_DAY);
      }
    } else {
      if (month < INDEX_ROLL_MONTHS[0]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[0], IMM_DAY);
      } else if (month == INDEX_ROLL_MONTHS[0]) {
        if (day < IMM_DAY) {
          return LocalDate.of(year, month, IMM_DAY);
        } else {
          return LocalDate.of(year, INDEX_ROLL_MONTHS[1], IMM_DAY);
        }
      } else if (month < INDEX_ROLL_MONTHS[1]) {
        return LocalDate.of(year, INDEX_ROLL_MONTHS[1], IMM_DAY);
      } else if (month == INDEX_ROLL_MONTHS[1]) {
        if (day < IMM_DAY) {
          return LocalDate.of(year, month, IMM_DAY);
        } else {
          return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], IMM_DAY);
        }
      } else {
        return LocalDate.of(year + 1, INDEX_ROLL_MONTHS[0], IMM_DAY);
      }
    }
  }

}
