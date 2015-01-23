/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.LocalDate;

import com.opengamma.basics.schedule.Frequency;
import com.opengamma.collect.ArgChecker;

/**
 * Standard day count convention implementations.
 * <p>
 * See {@link DayCounts} for the description of each.
 */
enum StandardDayCounts implements DayCount {

  // always one
  ONE_ONE("1/1") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return 1;
    }
  },

  // actual days / actual days in year
  ACT_ACT_ISDA("Act/Act ISDA") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
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
  },

  // complex ICMA calculation
  ACT_ACT_ICMA("Act/Act ICMA") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      // avoid using ScheduleInfo in this case
      if (firstDate.equals(secondDate)) {
        return 0d;
      }
      // calculation is based on the schedule period, firstDate assumed to be the start of the period
      LocalDate scheduleStartDate = scheduleInfo.getStartDate();
      LocalDate scheduleEndDate = scheduleInfo.getEndDate();
      LocalDate nextCouponDate = scheduleInfo.getPeriodEndDate(firstDate);
      Frequency freq = scheduleInfo.getFrequency();
      boolean eom = scheduleInfo.isEndOfMonthConvention();
      // final period, also handling single period schedules
      if (nextCouponDate.equals(scheduleEndDate)) {
        return finalPeriod(firstDate, secondDate, freq, eom);
      }
      // initial period
      if (firstDate.equals(scheduleStartDate)) {
        return initPeriod(firstDate, secondDate, nextCouponDate, freq, eom);
      }
      long firstEpochDay = firstDate.toEpochDay();
      double actualDays = secondDate.toEpochDay() - firstEpochDay;
      double periodDays = nextCouponDate.toEpochDay() - firstEpochDay;
      return actualDays / (freq.eventsPerYear() * periodDays);
    }

    // calculate nominal periods backwards from couponDate
    private double initPeriod(LocalDate startDate, LocalDate endDate, LocalDate couponDate, Frequency freq, boolean eom) {
      LocalDate currentNominal = couponDate;
      LocalDate prevNominal = eom(couponDate, currentNominal.minus(freq), eom);
      double result = 0;
      while (prevNominal.isAfter(startDate)) {
        result += calc(prevNominal, currentNominal, startDate, endDate, freq);
        currentNominal = prevNominal;
        prevNominal = eom(couponDate, currentNominal.minus(freq), eom);
      }
      return result + calc(prevNominal, currentNominal, startDate, endDate, freq);
    }

    // calculate nominal periods forwards from couponDate
    private double finalPeriod(LocalDate couponDate, LocalDate endDate, Frequency freq, boolean eom) {
      LocalDate curNominal = couponDate;
      LocalDate nextNominal = eom(couponDate, curNominal.plus(freq), eom);
      double result = 0;
      while (nextNominal.isBefore(endDate)) {
        result += calc(curNominal, nextNominal, curNominal, endDate, freq);
        curNominal = nextNominal;
        nextNominal = eom(couponDate, curNominal.plus(freq), eom);
      }
      return result + calc(curNominal, nextNominal, curNominal, endDate, freq);
    }

    // apply eom convention
    private LocalDate eom(LocalDate base, LocalDate calc, boolean eom) {
      return (eom && base.getDayOfMonth() == base.lengthOfMonth() ? calc.withDayOfMonth(calc.lengthOfMonth()) : calc);
    }

    // calculate the result
    private double calc(LocalDate prevNominal, LocalDate curNominal, LocalDate start, LocalDate end, Frequency freq) {
      if (end.isAfter(prevNominal)) {
        long curNominalEpochDay = curNominal.toEpochDay();
        long prevNominalEpochDay = prevNominal.toEpochDay();
        long startEpochDay = start.toEpochDay();
        long endEpochDay = end.toEpochDay();
        double periodDays = curNominalEpochDay - prevNominalEpochDay;
        double actualDays = Math.min(endEpochDay, curNominalEpochDay) - Math.max(startEpochDay, prevNominalEpochDay);
        return actualDays / (freq.eventsPerYear() * periodDays);
      }
      return 0;
    }
  },

  // AFB year-based calculation
  ACT_ACT_AFB("Act/Act AFB") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      // tests show that there is no need to perform an initial check of period less than
      // or equal one year when using the OpenGamma interpretation of end-of-February rule
      // calculate the number of whole years back from the end
      // OpenGamma interpretation: reject ISDA end-of-Feb if 28th Feb, apply simple subtraction from secondDate
      LocalDate end = secondDate;
      LocalDate start = secondDate.minusYears(1);
      int years = 0;
      while (!start.isBefore(firstDate)) {
        years++;
        end = start;
        start = secondDate.minusYears(years + 1);
      }
      // calculate the remaining fraction, including start, excluding end
      long actualDays = end.toEpochDay() - firstDate.toEpochDay();
      LocalDate nextLeap = DateAdjusters.nextOrSameLeapDay(firstDate);
      return years + (actualDays / (nextLeap.isBefore(end) ? 366d : 365d));
    }
  },

  // actual days / 365 or 366
  ACT_365_ACTUAL("Act/365 Actual") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      long actualDays = actualDays(firstDate, secondDate);
      LocalDate nextLeap = DateAdjusters.nextLeapDay(firstDate);
      return actualDays / (nextLeap.isAfter(secondDate) ? 365d : 366d);
    }
  },

  // actual days / 365 or 366
  ACT_365L("Act/365L") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      long actualDays = actualDays(firstDate, secondDate);
      // avoid using ScheduleInfo in this case
      if (firstDate.equals(secondDate)) {
        return 0d;
      }
      // calculation is based on the end of the schedule period (next coupon date) and annual/non-annual frequency
      LocalDate nextCouponDate = scheduleInfo.getPeriodEndDate(firstDate);
      if (scheduleInfo.getFrequency().eventsPerYear() == 1) {
        LocalDate nextLeap = DateAdjusters.nextLeapDay(firstDate);
        return actualDays / (nextLeap.isAfter(nextCouponDate) ? 365d : 366d);
      } else {
        return actualDays / (nextCouponDate.isLeapYear() ? 366d : 365d);
      }
    }
  },

  // simple actual days / 360
  ACT_360("Act/360") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return actualDays(firstDate, secondDate) / 360d;
    }
  },

  // simple actual days / 364
  ACT_364("Act/364") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return actualDays(firstDate, secondDate) / 364d;
    }
  },

  // simple actual days / 365
  ACT_365F("Act/365F") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return actualDays(firstDate, secondDate) / 365d;
    }
  },

  // simple actual days / 365.25
  ACT_365_25("Act/365.25") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return actualDays(firstDate, secondDate) / 365.25d;
    }
  },

  // no leaps / 365
  NL_365("NL/365") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      long actualDays = actualDays(firstDate, secondDate);
      int numberOfLeapDays = 0;
      LocalDate temp = DateAdjusters.nextLeapDay(firstDate);
      while (temp.isAfter(secondDate) == false) {
        numberOfLeapDays++;
        temp = DateAdjusters.nextLeapDay(temp);
      }
      return (actualDays - numberOfLeapDays) / 365d;
    }
  },

  // ISDA thirty day months / 360
  THIRTY_360_ISDA("30/360 ISDA") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
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
  },

  // US thirty day months / 360
  THIRTY_U_360("30U/360") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      boolean lastFeb1 = (firstDate.getMonthValue() == 2 && d1 == firstDate.lengthOfMonth());
      boolean lastFeb2 = (secondDate.getMonthValue() == 2 && d2 == secondDate.lengthOfMonth());
      if (scheduleInfo.isEndOfMonthConvention() && lastFeb1) {
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
  },

  // ISDA EU thirty day months / 360
  THIRTY_E_360_ISDA("30E/360 ISDA") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      boolean lastFeb1 = (firstDate.getMonthValue() == 2 && d1 == firstDate.lengthOfMonth());
      boolean lastFeb2 = (secondDate.getMonthValue() == 2 && d2 == secondDate.lengthOfMonth());
      if (d1 == 31 || lastFeb1) {
        d1 = 30;
      }
      if (d2 == 31 || (lastFeb2 && !secondDate.equals(scheduleInfo.getEndDate()))) {
        d2 = 30;
      }
      return thirty360(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
  },

  // E thirty day months / 360
  THIRTY_E_360("30E/360") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
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
  },

  // E+ thirty day months / 360
  THIRTY_EPLUS_360("30E+/360") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
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
  };

  // name
  private final String name;

  // create
  private StandardDayCounts(String name) {
    this.name = name;
  }

  // calculate using the standard 30/360 function - 360(y2 - y1) + 30(m2 - m1) + (d2 - d1)) / 360
  private static double thirty360(int y1, int m1, int d1, int y2, int m2, int d2) {
    return (360 * (y2 - y1) + 30 * (m2 - m1) + (d2 - d1)) / 360d;
  }

  // return actual days difference between the dates
  private static long actualDays(LocalDate firstDate, LocalDate secondDate) {
    return secondDate.toEpochDay() - firstDate.toEpochDay();
  }

  @Override
  public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
    ArgChecker.notNull(firstDate, "firstDate");
    ArgChecker.notNull(secondDate, "secondDate");
    ArgChecker.notNull(scheduleInfo, "scheduleInfo");
    if (secondDate.isBefore(firstDate)) {
      throw new IllegalArgumentException("Dates must be in time-line order");
    }
    return calculateYearFraction(firstDate, secondDate, scheduleInfo);
  }

  // calculate the year fraction, using validated inputs
  abstract double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo);

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
