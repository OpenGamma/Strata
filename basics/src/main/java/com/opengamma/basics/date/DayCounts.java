/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;

/**
 * Implementation of standard day count conventions.
 */
enum DayCounts implements DayCount {

  /**
   * See {@link DayCount#ACT_ACT_ISDA}.
   */
  ACT_ACT_ISDA {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      check(firstDate, secondDate);
      int y1 = firstDate.getYear();
      int y2 = secondDate.getYear();
      double firstYearLength = firstDate.lengthOfYear();
      if (y1 == y2) {
        double actualDays = secondDate.getDayOfYear() - firstDate.getDayOfYear();
        return actualDays / firstYearLength;
      }
      double firstRemainderOfYear = firstYearLength - firstDate.getDayOfYear() + 1;
      double secondRemainderOfYear = secondDate.getDayOfYear() - 1;
      double secondYearLength = secondDate.lengthOfYear();
      return firstRemainderOfYear / firstYearLength +
          secondRemainderOfYear / secondYearLength +
          (y2 - y1 - 1);
    }
    @Override
    public String getName() {
      return "Act/Act ISDA";
    }
  },
  /**
   * See {@link DayCount#ACT_360}.
   */
  ACT_360 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = checkGetActualDays(firstDate, secondDate);
      return actualDays / 360d;
    }
    @Override
    public String getName() {
      return "Act/360";
    }
  },
  /**
   * See {@link DayCount#ACT_364}.
   */
  ACT_364 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = checkGetActualDays(firstDate, secondDate);
      return actualDays / 364d;
    }
    @Override
    public String getName() {
      return "Act/364";
    }
  },
  /**
   * See {@link DayCount#ACT_365F}.
   */
  ACT_365F {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = checkGetActualDays(firstDate, secondDate);
      return actualDays / 365d;
    }
    @Override
    public String getName() {
      return "Act/365F";
    }
  },
  /**
   * See {@link DayCount#ACT_365_25}.
   */
  ACT_365_25 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = checkGetActualDays(firstDate, secondDate);
      return actualDays / 365.25d;
    }
    @Override
    public String getName() {
      return "Act/365.25";
    }
  },
  /**
   * See {@link DayCount#NL_365}.
   */
  NL_365 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = checkGetActualDays(firstDate, secondDate);
      int numberOfLeapDays = 0;
      LocalDate temp = DateAdjusters.nextLeapDay(firstDate);
      while (temp.isAfter(secondDate) == false) {
        numberOfLeapDays++;
        temp = DateAdjusters.nextLeapDay(temp);
      }
      return (actualDays - numberOfLeapDays) / 365d;
    }
    @Override
    public String getName() {
      return "NL/365";
    }
  },
  /**
   * See {@link DayCount#_30_360_ISDA}.
   */
  _30_360_ISDA {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      check(firstDate, secondDate);
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31) {
        d1 = 30;
      }
      if (d2 == 31 && d1 == 30) {
        d2 = 30;
      }
      return thirty360(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
    @Override
    public String getName() {
      return "30/360 ISDA";
    }
  },
  /**
   * See {@link DayCount#_30U_360}.
   */
  _30U_360 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      check(firstDate, secondDate);
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      boolean lastFeb1 = (firstDate.getMonthValue() == 2 && d1 == firstDate.lengthOfMonth());
      boolean lastFeb2 = (secondDate.getMonthValue() == 2 && d2 == secondDate.lengthOfMonth());
      if (lastFeb1) {
        if (lastFeb2) {
          d2 = 30;
        }
        d1 = 30;
      }
      if (d1 == 31) {
        d1 = 30;
      }
      if (d2 == 31 && d1 == 30) {
        d2 = 30;
      }
      return thirty360(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
    @Override
    public String getName() {
      return "30U/360";
    }
  },
  /**
   * See {@link DayCount#_30_360_ISDA}.
   */
  _30E_360_ISDA {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      check(firstDate, secondDate);
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      boolean lastFeb1 = (firstDate.getMonthValue() == 2 && d1 == firstDate.lengthOfMonth());
      boolean lastFeb2 = (secondDate.getMonthValue() == 2 && d2 == secondDate.lengthOfMonth());
      if (d1 == 31 || lastFeb1) {
        d1 = 30;
      }
      if (d2 == 31 || lastFeb2) {
        d2 = 30;
      }
      return thirty360(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
    @Override
    public String getName() {
      return "30/360 German";
    }
  },
  /**
   * See {@link DayCount#_30E_360}.
   */
  _30E_360 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      check(firstDate, secondDate);
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31) {
        d1 = 30;
      }
      if (d2 == 31) {
        d2 = 30;
      }
      return thirty360(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
    @Override
    public String getName() {
      return "30E/360";
    }
  },
  /**
   * See {@link DayCount#_30EPLUS_360}.
   */
  _30EPLUS_360 {
    @Override
    public double getDayCountFraction(LocalDate firstDate, LocalDate secondDate) {
      check(firstDate, secondDate);
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      int m1 = firstDate.getMonthValue();
      int m2 = secondDate.getMonthValue();
      if (d1 == 31) {
        d1 = 30;
      }
      if (d2 == 31) {
        d2 = 1;
        m2 = m2 + 1;  // nature of calculation means no need to adjust Dec to Jan
      }
      return thirty360(
          firstDate.getYear(), m1, d1,
          secondDate.getYear(), m2, d2);
    }
    @Override
    public String getName() {
      return "30E+/360";
    }
  };

  // calculate using the standard 30/360 function - 360(y2 - y1) + 30(m2 - m1) + (d2 - d1)) / 360
  private static double thirty360(int y1, int m1, int d1, int y2, int m2, int d2) {
    return (360 * (y2 - y1) + 30 * (m2 - m1) + (d2 - d1)) / 360d;
  }

  // validate inputs and return actual days difference
  private static long checkGetActualDays(LocalDate firstDate, LocalDate secondDate ) {
    ArgChecker.notNull(firstDate, "firstDate");
    ArgChecker.notNull(secondDate, "secondDate");
    long actualDays = secondDate.toEpochDay() - firstDate.toEpochDay();
    ArgChecker.isTrue(actualDays >= 0, "Dates must be in order");
    return actualDays;
  }

  // validate inputs
  private static void check(LocalDate firstDate, LocalDate secondDate ) {
    ArgChecker.notNull(firstDate, "firstDate");
    ArgChecker.notNull(secondDate, "secondDate");
    ArgChecker.isFalse(secondDate.isBefore(firstDate), "Dates must be in order");
  }

}
