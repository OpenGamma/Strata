/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.opengamma.collect.ArgChecker;

/**
 * Constants and implementations for standard roll conventions.
 * <p>
 * The purpose of this convention is to define how to roll dates when building a schedule.
 * The standard approach to building a schedule is based on unadjusted dates, which do not
 * have a {@linkplain BusinessDayConvention business day convention} applied.
 * To get the next date in the schedule, take the base date and the
 * {@linkplain Frequency periodic frequency}. Once this date is calculated,
 * the roll convention is applied to produce the next schedule date.
 * <p>
 * In most cases the specific values for day-of-month and day-of-week are not needed.
 * A one month periodic frequency will naturally select the same day-of-month as the
 * input date, thus the day-of-month does not need to be additionally specified.
 */
public final class RollConventions {

  /**
   * The 'None' roll convention which makes no adjustment.
   * <p>
   * The input date will not be adjusted.
   */
  public static final RollConvention NONE = Standard.NONE;
  /**
   * The 'EOM' roll convention which adjusts the date to the end of the month.
   * <p>
   * The input date will be adjusted ensure it is the last valid day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention EOM = Standard.EOM;
  /**
   * The 'IMM' roll convention which adjusts the date to the third Wednesday.
   * <p>
   * The input date will be adjusted ensure it is the third Wednesday of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention IMM = Standard.IMM;
  /**
   * The 'IMMAUD' roll convention which adjusts the date to the Thursday before the second Friday.
   * <p>
   * The input date will be adjusted ensure it is the Thursday before the second Friday of the month.
   * This is intended to be used with a business day adjuster of 'Preceding' in Sydney.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention IMMAUD = Standard.IMMAUD;
  /**
   * The 'IMMNZD' roll convention which adjusts the date to the first Wednesday
   * on or after the ninth day of the month.
   * <p>
   * The input date will be adjusted to the ninth day of the month, and then it will
   * be adjusted to be a Wednesday. If the ninth is a Wednesday, then that is returned.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention IMMNZD = Standard.IMMNZD;
  /**
   * The 'SFE' roll convention which adjusts the date to the second Friday.
   * <p>
   * The input date will be adjusted ensure it is the second Friday of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention SFE = Standard.SFE;
  /**
   * The 'Implied' roll convention is used when the convention should be implied
   * favoring end-of-month when ambiguous.
   * <p>
   * In many cases, the convention to be used can be implied from an unadjusted date
   * in the sequence and the {@linkplain Frequency periodic frequency}.
   * For example, if the start date of the sequence is 2014-06-20 and the periodic
   * frequency is 'P3M' (month-based), then the implied convention is 'Day20'.
   * Whereas, if the frequency is 'P2W' (week-based), then the implied convention
   * is 'DayFri', because 2014-06-20 is a Friday.
   * <p>
   * The rules are as follows.
   * If the input frequency is week-based, then the implied convention is based on
   * the day-of-week of the input date.
   * If the input frequency is month-based, then the implied convention is based on
   * the day-of-month of the input date, unless the input date is at the end of the
   * month, in which case then the implied convention is 'EOM'.
   * In all other cases, the implied convention is 'None'.
   * <p>
   * It is not intended that this convention is directly used for calculation.
   * It must be converted to a specific convention using
   * {@link RollConvention#imply(LocalDate, Frequency)}
   * before use with {@code next()} or {@code previous()}.
   */
  public static final RollConvention IMPLIED = Standard.IMPLIED;

  /**
   * The 'Day1' roll convention which adjusts the date to day-of-month 1.
   * <p>
   * The input date will be adjusted ensure it is the 1st day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_1 = Dom.of(1);
  /**
   * The 'Day2' roll convention which adjusts the date to day-of-month 2.
   * <p>
   * The input date will be adjusted ensure it is the 2nd day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_2 = Dom.of(2);
  /**
   * The 'Day3' roll convention which adjusts the date to day-of-month 3.
   * <p>
   * The input date will be adjusted ensure it is the 3rd day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_3 = Dom.of(3);
  /**
   * The 'Day4' roll convention which adjusts the date to day-of-month 4.
   * <p>
   * The input date will be adjusted ensure it is the 4th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_4 = Dom.of(4);
  /**
   * The 'Day5' roll convention which adjusts the date to day-of-month 5.
   * <p>
   * The input date will be adjusted ensure it is the 5th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_5 = Dom.of(5);
  /**
   * The 'Day6' roll convention which adjusts the date to day-of-month 6.
   * <p>
   * The input date will be adjusted ensure it is the 6th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_6 = Dom.of(6);
  /**
   * The 'Day7' roll convention which adjusts the date to day-of-month 7.
   * <p>
   * The input date will be adjusted ensure it is the 7th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_7 = Dom.of(7);
  /**
   * The 'Day8' roll convention which adjusts the date to day-of-month 8.
   * <p>
   * The input date will be adjusted ensure it is the 8th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_8 = Dom.of(8);
  /**
   * The 'Day9' roll convention which adjusts the date to day-of-month 9.
   * <p>
   * The input date will be adjusted ensure it is the 9th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_9 = Dom.of(9);
  /**
   * The 'Day10' roll convention which adjusts the date to day-of-month 10.
   * <p>
   * The input date will be adjusted ensure it is the 10th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_10 = Dom.of(10);
  /**
   * The 'Day11' roll convention which adjusts the date to day-of-month 11.
   * <p>
   * The input date will be adjusted ensure it is the 11th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_11 = Dom.of(11);
  /**
   * The 'Day12' roll convention which adjusts the date to day-of-month 12.
   * <p>
   * The input date will be adjusted ensure it is the 12th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_12 = Dom.of(12);
  /**
   * The 'Day13' roll convention which adjusts the date to day-of-month 13
   * <p>
   * The input date will be adjusted ensure it is the 13th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_13 = Dom.of(13);
  /**
   * The 'Day14' roll convention which adjusts the date to day-of-month 14.
   * <p>
   * The input date will be adjusted ensure it is the 14th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_14 = Dom.of(14);
  /**
   * The 'Day15' roll convention which adjusts the date to day-of-month 15.
   * <p>
   * The input date will be adjusted ensure it is the 15th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_15 = Dom.of(15);
  /**
   * The 'Day16' roll convention which adjusts the date to day-of-month 16.
   * <p>
   * The input date will be adjusted ensure it is the 16th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_16 = Dom.of(16);
  /**
   * The 'Day17' roll convention which adjusts the date to day-of-month 17.
   * <p>
   * The input date will be adjusted ensure it is the 17th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_17 = Dom.of(17);
  /**
   * The 'Day18' roll convention which adjusts the date to day-of-month 18.
   * <p>
   * The input date will be adjusted ensure it is the 18th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_18 = Dom.of(18);
  /**
   * The 'Day19' roll convention which adjusts the date to day-of-month 19.
   * <p>
   * The input date will be adjusted ensure it is the 19th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_19 = Dom.of(19);
  /**
   * The 'Day20' roll convention which adjusts the date to day-of-month 20.
   * <p>
   * The input date will be adjusted ensure it is the 20th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_20 = Dom.of(20);
  /**
   * The 'Day21' roll convention which adjusts the date to day-of-month 21.
   * <p>
   * The input date will be adjusted ensure it is the 21st day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_21 = Dom.of(21);
  /**
   * The 'Day22' roll convention which adjusts the date to day-of-month 22.
   * <p>
   * The input date will be adjusted ensure it is the 22nd day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_22 = Dom.of(22);
  /**
   * The 'Day23' roll convention which adjusts the date to day-of-month 23.
   * <p>
   * The input date will be adjusted ensure it is the 23rd day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_23 = Dom.of(23);
  /**
   * The 'Day24' roll convention which adjusts the date to day-of-month 24.
   * <p>
   * The input date will be adjusted ensure it is the 24th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_24 = Dom.of(24);
  /**
   * The 'Day25' roll convention which adjusts the date to day-of-month 25.
   * <p>
   * The input date will be adjusted ensure it is the 25th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_25 = Dom.of(25);
  /**
   * The 'Day26' roll convention which adjusts the date to day-of-month 26.
   * <p>
   * The input date will be adjusted ensure it is the 26th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_26 = Dom.of(26);
  /**
   * The 'Day27' roll convention which adjusts the date to day-of-month 27.
   * <p>
   * The input date will be adjusted ensure it is the 27th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_27 = Dom.of(27);
  /**
   * The 'Day28' roll convention which adjusts the date to day-of-month 28.
   * <p>
   * The input date will be adjusted ensure it is the 28th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_28 = Dom.of(28);
  /**
   * The 'Day29' roll convention which adjusts the date to day-of-month 29.
   * <p>
   * The input date will be adjusted ensure it is the 29th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_29 = Dom.of(29);
  /**
   * The 'Day30' roll convention which adjusts the date to day-of-month 30.
   * <p>
   * The input date will be adjusted ensure it is the 30th day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention DAY_30 = Dom.of(30);

  /**
   * The 'DayMon' roll convention which adjusts the date to be Monday.
   * <p>
   * The input date will be adjusted ensure it is a Monday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_MON = Dow.of(DayOfWeek.MONDAY);
  /**
   * The 'DayTue' roll convention which adjusts the date to be Tuesday.
   * <p>
   * The input date will be adjusted ensure it is a Tuesday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_TUE = Dow.of(DayOfWeek.TUESDAY);
  /**
   * The 'DayWed' roll convention which adjusts the date to be Wednesday.
   * <p>
   * The input date will be adjusted ensure it is a Wednesday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_WED = Dow.of(DayOfWeek.WEDNESDAY);
  /**
   * The 'DayThu' roll convention which adjusts the date to be Thursday.
   * <p>
   * The input date will be adjusted ensure it is a Thursday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_THU = Dow.of(DayOfWeek.THURSDAY);
  /**
   * The 'DayFri' roll convention which adjusts the date to be Friday.
   * <p>
   * The input date will be adjusted ensure it is a Friday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_FRI = Dow.of(DayOfWeek.FRIDAY);
  /**
   * The 'DaySat' roll convention which adjusts the date to be Saturday.
   * <p>
   * The input date will be adjusted ensure it is a Saturday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_SAT = Dow.of(DayOfWeek.SATURDAY);
  /**
   * The 'DaySun' roll convention which adjusts the date to be Sunday.
   * <p>
   * The input date will be adjusted ensure it is a Sunday.
   * This convention is intended for use with periods that are a multiple of weeks.
   */
  public static final RollConvention DAY_SUN = Dow.of(DayOfWeek.SUNDAY);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private RollConventions() {
  }

  //-------------------------------------------------------------------------
  /**
   * Standard roll conventions.
   */
  static enum Standard implements RollConvention {

    // no adjustment
    NONE("None") {
      @Override
      public LocalDate adjust(LocalDate date) {
        return ArgChecker.notNull(date, "date");
      }
    },
    // last day of month
    EOM("EOM") {
      @Override
      public LocalDate adjust(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.withDayOfMonth(date.lengthOfMonth());
      }
    },
    // 3rd Wednesday
    IMM("IMM") {
      @Override
      public LocalDate adjust(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY));
      }
    },
    // day before 2nd Friday
    IMMAUD("IMMAUD") {
      @Override
      public LocalDate adjust(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY)).minusDays(1);
      }
    },
    // Wednesday on or after 9th
    IMMNZD("IMMNZD") {
      @Override
      public LocalDate adjust(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.withDayOfMonth(9).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
      }
    },
    // 2nd Friday
    SFE("SFE") {
      @Override
      public LocalDate adjust(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY));
      }
    },
    // implied from date and frequency
    IMPLIED("Implied") {
      @Override
      public LocalDate adjust(LocalDate date) {
        return ArgChecker.notNull(date, "date");
      }
      @Override
      public RollConvention imply(LocalDate date, Frequency periodicFrequency) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(periodicFrequency, "periodicFrequency");
        if (periodicFrequency.isMonthBased()) {
          if (date.getDayOfMonth() == date.lengthOfMonth()) {
            return EOM;
          }
          return RollConvention.ofDayOfMonth(date.getDayOfMonth());
        } else if (periodicFrequency.isWeekBased()) {
          return RollConvention.ofDayOfWeek(date.getDayOfWeek());
        }
        return NONE;
      }
      @Override
      public LocalDate next(LocalDate date, Frequency periodicFrequency) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(periodicFrequency, "periodicFrequency");
        throw new IllegalStateException("RollConventions.IMPLIED must be resolved before use using imply()");
      }
      @Override
      public LocalDate previous(LocalDate date, Frequency periodicFrequency) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(periodicFrequency, "periodicFrequency");
        throw new IllegalStateException("RollConventions.IMPLIED must be resolved before use using imply()");
      }
    };

    // name
    private final String name;

    // create
    private Standard(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-month roll convention.
   */
  static final class Dom implements RollConvention, Serializable {
    // singleton, so no equals/hashCode

    // Serialization version
    private static final long serialVersionUID = 1L;
    // cache of conventions
    private static final RollConvention[] CONVENTIONS = new RollConvention[30];
    static {
      for (int i = 0; i < 30; i++) {
        CONVENTIONS[i] = new Dom(i + 1);
      }
    }

    // day-of-month
    private final int day;
    // unique name
    private final String name;

    // obtains instance
    static RollConvention of(int day) {
      if (day == 31) {
        return RollConventions.EOM;
      } else if (day < 1 || day > 30) {
        throw new IllegalArgumentException("Invalid day-of-month: " + day);
      }
      return CONVENTIONS[day - 1];
    }

    // create
    private Dom(int day) {
      this.day = day;
      this.name = "Day" + day;
    }

    private Object readResolve() {
      return Dom.of(day);
    }

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      if (day >= 29 && date.getMonthValue() == 2) {
        return date.withDayOfMonth(date.lengthOfMonth());
      }
      return date.withDayOfMonth(day);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-week roll convention.
   */
  static final class Dow implements RollConvention, Serializable {
    // singleton, so no equals/hashCode

    // Serialization version
    private static final long serialVersionUID = 1L;
    // convention names
    private static final String NAMES = "DayMonDayTueDayWedDayThuDayFriDaySatDaySun";
    // cache of conventions
    private static final RollConvention[] CONVENTIONS = new RollConvention[7];
    static {
      for (int i = 0; i < 7; i++) {
        DayOfWeek dow = DayOfWeek.of(i + 1);
        String name = NAMES.substring(i * 6, (i + 1) * 6);
        CONVENTIONS[i] = new Dow(dow, name);
      }
    }

    // day-of-week
    private final DayOfWeek day;
    // unique name
    private final String name;

    // obtains instance
    static RollConvention of(DayOfWeek dayOfWeek) {
      ArgChecker.notNull(dayOfWeek, "dayOfWeek");
      return CONVENTIONS[dayOfWeek.getValue() - 1];
    }

    private Object readResolve() {
      return Dow.of(day);
    }

    // create
    private Dow(DayOfWeek dayOfWeek, String name) {
      this.day = dayOfWeek;
      this.name = name;
    }

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.nextOrSame(day));
    }

    @Override
    public LocalDate next(LocalDate date, Frequency periodicFrequency) {
      ArgChecker.notNull(date, "inputDate");
      ArgChecker.notNull(periodicFrequency, "periodicFrequency");
      LocalDate calculated = date.plus(periodicFrequency);
      return calculated.with(TemporalAdjusters.nextOrSame(day));
    }

    @Override
    public LocalDate previous(LocalDate date, Frequency periodicFrequency) {
      ArgChecker.notNull(date, "inputDate");
      ArgChecker.notNull(periodicFrequency, "periodicFrequency");
      LocalDate calculated = date.minus(periodicFrequency);
      return calculated.with(TemporalAdjusters.previousOrSame(day));
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
