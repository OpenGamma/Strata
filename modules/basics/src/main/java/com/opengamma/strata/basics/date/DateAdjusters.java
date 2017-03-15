/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.time.LocalDate;
import java.time.Year;

/**
 * Date adjusters that perform useful operations on {@code LocalDate}.
 * <p>
 * This is a static utility class.
 * Returned objects are immutable and thread-safe.
 */
public final class DateAdjusters {

  /**
   * Restricted constructor.
   */
  private DateAdjusters() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that finds the next leap day after the input date.
   * <p>
   * The adjuster returns the next occurrence of February 29 after the input date.
   * 
   * @return an adjuster that finds the next leap day
   */
  public static DateAdjuster nextLeapDay() {
    return DateAdjusters::nextLeapDay;
  }

  /**
   * Finds the next leap day after the input date.
   * 
   * @param input  the input date
   * @return the next leap day date
   */
  static LocalDate nextLeapDay(LocalDate input) {
    // already a leap day, move forward either 4 or 8 years
    if (input.getMonthValue() == 2 && input.getDayOfMonth() == 29) {
      return ensureLeapDay(input.getYear() + 4);
    }
    // handle if before February 29 in a leap year
    if (input.isLeapYear() && input.getMonthValue() <= 2) {
      return LocalDate.of(input.getYear(), 2, 29);
    }
    // handle any other date
    return ensureLeapDay(((input.getYear() / 4) * 4) + 4);
  }

  /**
   * Obtains a date adjuster that finds the next leap day on or after the input date.
   * <p>
   * If the input date is February 29, the input date is returned unaltered.
   * Otherwise, the adjuster returns the next occurrence of February 29 after the input date.
   * 
   * @return an adjuster that finds the next leap day
   */
  public static DateAdjuster nextOrSameLeapDay() {
    return DateAdjusters::nextOrSameLeapDay;
  }

  /**
   * Finds the next leap day on or after the input date.
   * <p>
   * If the input date is February 29, the input date is returned unaltered.
   * Otherwise, the adjuster returns the next occurrence of February 29 after the input date.
   * 
   * @param input  the input date
   * @return the next leap day date
   */
  static LocalDate nextOrSameLeapDay(LocalDate input) {
    // already a leap day, return it
    if (input.getMonthValue() == 2 && input.getDayOfMonth() == 29) {
      return input;
    }
    // handle if before February 29 in a leap year
    if (input.isLeapYear() && input.getMonthValue() <= 2) {
      return LocalDate.of(input.getYear(), 2, 29);
    }
    // handle any other date
    return ensureLeapDay(((input.getYear() / 4) * 4) + 4);
  }

  // handle 2100, which is not a leap year
  private static LocalDate ensureLeapDay(int possibleLeapYear) {
    if (Year.isLeap(possibleLeapYear)) {
      return LocalDate.of(possibleLeapYear, 2, 29);
    } else {
      return LocalDate.of(possibleLeapYear + 4, 2, 29);
    }
  }

}
