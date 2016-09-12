/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.LocalDateUtils.daysBetween;
import static com.opengamma.strata.basics.date.LocalDateUtils.doy;
import static java.lang.Math.toIntExact;

import java.time.LocalDate;

import com.opengamma.strata.basics.schedule.Frequency;

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

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
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
        double actualDays = doy(secondDate) - doy(firstDate);
        return actualDays / firstYearLength;
      }
      double firstRemainderOfYear = firstYearLength - doy(firstDate) + 1;
      double secondRemainderOfYear = doy(secondDate) - 1;
      double secondYearLength = secondDate.lengthOfYear();
      return firstRemainderOfYear / firstYearLength +
          secondRemainderOfYear / secondYearLength +
          (y2 - y1 - 1);
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
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
      double actualDays = daysBetween(firstDate, secondDate);
      double periodDays = daysBetween(firstDate, nextCouponDate);
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

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
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
      long actualDays = daysBetween(firstDate, end);
      LocalDate nextLeap = DateAdjusters.nextOrSameLeapDay(firstDate);
      return years + (actualDays / (nextLeap.isBefore(end) ? 366d : 365d));
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // actual days / actual days in year from start date
  ACT_ACT_YEAR("Act/Act Year") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      LocalDate startDate = firstDate;
      int yearsAdded = 0;
      while (secondDate.compareTo(startDate.plusYears(1)) > 0) {
        startDate = firstDate.plusYears(++yearsAdded);
      }
      double actualDays = daysBetween(startDate, secondDate);
      double actualDaysInYear = daysBetween(startDate, startDate.plusYears(1));
      return yearsAdded + (actualDays / actualDaysInYear);
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // actual days / 365 or 366
  ACT_365_ACTUAL("Act/365 Actual") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      long actualDays = daysBetween(firstDate, secondDate);
      LocalDate nextLeap = DateAdjusters.nextLeapDay(firstDate);
      return actualDays / (nextLeap.isAfter(secondDate) ? 365d : 366d);
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // actual days / 365 or 366
  ACT_365L("Act/365L") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      long actualDays = daysBetween(firstDate, secondDate);
      // avoid using ScheduleInfo in this case
      if (firstDate.equals(secondDate)) {
        return 0d;
      }
      // calculation is based on the end of the schedule period (next coupon date) and annual/non-annual frequency
      LocalDate nextCouponDate = scheduleInfo.getPeriodEndDate(firstDate);
      if (scheduleInfo.getFrequency().isAnnual()) {
        LocalDate nextLeap = DateAdjusters.nextLeapDay(firstDate);
        return actualDays / (nextLeap.isAfter(nextCouponDate) ? 365d : 366d);
      } else {
        return actualDays / (nextCouponDate.isLeapYear() ? 366d : 365d);
      }
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // simple actual days / 360
  ACT_360("Act/360") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return daysBetween(firstDate, secondDate) / 360d;
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // simple actual days / 364
  ACT_364("Act/364") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return daysBetween(firstDate, secondDate) / 364d;
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // simple actual days / 365
  ACT_365F("Act/365F") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return daysBetween(firstDate, secondDate) / 365d;
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // simple actual days / 365.25
  ACT_365_25("Act/365.25") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      return daysBetween(firstDate, secondDate) / 365.25d;
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      return toIntExact(actualDays);
    }
  },

  // no leaps / 365
  NL_365("NL/365") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      long actualDays = daysBetween(firstDate, secondDate);
      int numberOfLeapDays = 0;
      LocalDate temp = DateAdjusters.nextLeapDay(firstDate);
      while (temp.isAfter(secondDate) == false) {
        numberOfLeapDays++;
        temp = DateAdjusters.nextLeapDay(temp);
      }
      return (actualDays - numberOfLeapDays) / 365d;
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      long actualDays = daysBetween(firstDate, secondDate);
      int numberOfLeapDays = 0;
      LocalDate temp = DateAdjusters.nextLeapDay(firstDate);
      while (temp.isAfter(secondDate) == false) {
        numberOfLeapDays++;
        temp = DateAdjusters.nextLeapDay(temp);
      }
      return toIntExact(actualDays) - numberOfLeapDays;
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

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31) {
        d1 = 30;
      }
      if (d2 == 31 && d1 == 30) {
        d2 = 30;
      }
      return thirty360Days(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
  },

  // US thirty day months / 360 with dynamic EOM rule
  THIRTY_U_360("30U/360") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      if (scheduleInfo.isEndOfMonthConvention()) {
        return THIRTY_U_360_EOM.calculateYearFraction(firstDate, secondDate, scheduleInfo);
      } else {
        return THIRTY_360_ISDA.calculateYearFraction(firstDate, secondDate, scheduleInfo);
      }
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      return THIRTY_360_ISDA.days(firstDate, secondDate);
    }
  },

  // US thirty day months / 360 with fixed EOM rule
  THIRTY_U_360_EOM("30U/360 EOM") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (lastDayOfFebruary(firstDate)) {
        if (lastDayOfFebruary(secondDate)) {
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
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (lastDayOfFebruary(firstDate)) {
        if (lastDayOfFebruary(secondDate)) {
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
      return thirty360Days(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }
  },

  THIRTY_360_PSA("30/360 PSA") {
    @Override
    public double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31 || lastDayOfFebruary(firstDate)) {
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
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31 || lastDayOfFebruary(firstDate)) {
        d1 = 30;
      }
      if (d2 == 31 && d1 == 30) {
        d2 = 30;
      }
      return thirty360Days(
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
      if (d1 == 31 || lastDayOfFebruary(firstDate)) {
        d1 = 30;
      }
      if (d2 == 31 || (lastDayOfFebruary(secondDate) && !secondDate.equals(scheduleInfo.getEndDate()))) {
        d2 = 30;
      }
      return thirty360(
          firstDate.getYear(), firstDate.getMonthValue(), d1,
          secondDate.getYear(), secondDate.getMonthValue(), d2);
    }

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31 || lastDayOfFebruary(firstDate)) {
        d1 = 30;
      }
      if (d2 == 31 || (lastDayOfFebruary(secondDate))) {
        d2 = 30;
      }
      return thirty360Days(
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

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
      int d1 = firstDate.getDayOfMonth();
      int d2 = secondDate.getDayOfMonth();
      if (d1 == 31) {
        d1 = 30;
      }
      if (d2 == 31) {
        d2 = 30;
      }
      return thirty360Days(
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

    @Override
    public int calculateDays(LocalDate firstDate, LocalDate secondDate) {
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
      return thirty360Days(
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

  //calculate using the 30/360 function as above but does not divide by 360, as the number of days is needed, not the fraction.
  private static int thirty360Days(int y1, int m1, int d1, int y2, int m2, int d2) {
    return 360 * (y2 - y1) + 30 * (m2 - m1) + (d2 - d1);
  }

  // determine if the date is the last day of february
  private static boolean lastDayOfFebruary(LocalDate date) {
    return date.getMonthValue() == 2 && date.getDayOfMonth() == date.lengthOfMonth();
  }

  @Override
  public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
    if (secondDate.isBefore(firstDate)) {
      throw new IllegalArgumentException("Dates must be in time-line order");
    }
    return calculateYearFraction(firstDate, secondDate, scheduleInfo);
  }

  @Override
  public int days(LocalDate firstDate, LocalDate secondDate) {
    if (secondDate.isBefore(firstDate)) {
      throw new IllegalArgumentException("Dates must be in time-line order");
    }
    return calculateDays(firstDate, secondDate);
  }

  @Override
  public double relativeYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
    // override to avoid duplicate null checks
    if (secondDate.isBefore(firstDate)) {
      return -calculateYearFraction(secondDate, firstDate, scheduleInfo);
    }
    return calculateYearFraction(firstDate, secondDate, scheduleInfo);
  }

  // calculate the year fraction, using validated inputs
  abstract double calculateYearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo);

  //calculate the number of days between the specified dates, using validated inputs
  abstract int calculateDays(LocalDate firstDate, LocalDate secondDate);

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
