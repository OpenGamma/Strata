/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.time.DayOfWeek;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.schedule.DayRollConventions.Dom;
import com.opengamma.strata.basics.schedule.DayRollConventions.Dow;
import com.opengamma.strata.collect.named.ExtendedEnum;

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
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<RollConvention> ENUM_LOOKUP = ExtendedEnum.of(RollConvention.class);

  /**
   * The 'None' roll convention.
   * <p>
   * The input date will not be adjusted.
   * <p>
   * When calculating a schedule, there will be no further adjustment after the
   * periodic frequency is added or subtracted.
   */
  public static final RollConvention NONE = RollConvention.of(StandardRollConventions.NONE.getName());
  /**
   * The 'EOM' roll convention which adjusts the date to the end of the month.
   * <p>
   * The input date will be adjusted ensure it is the last valid day of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention EOM = RollConvention.of(StandardRollConventions.EOM.getName());
  /**
   * The 'IMM' roll convention which adjusts the date to the third Wednesday.
   * <p>
   * The input date will be adjusted ensure it is the third Wednesday of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention IMM = RollConvention.of(StandardRollConventions.IMM.getName());
  /**
   * The 'IMMAUD' roll convention which adjusts the date to the Thursday before the second Friday.
   * <p>
   * The input date will be adjusted ensure it is the Thursday before the second Friday of the month.
   * This is intended to be used with a business day adjuster of 'Preceding' in Sydney.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention IMMAUD = RollConvention.of(StandardRollConventions.IMMAUD.getName());
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
  public static final RollConvention IMMNZD = RollConvention.of(StandardRollConventions.IMMNZD.getName());
  /**
   * The 'SFE' roll convention which adjusts the date to the second Friday.
   * <p>
   * The input date will be adjusted ensure it is the second Friday of the month.
   * The year and month of the result date will be the same as the input date.
   * <p>
   * This convention is intended for use with periods that are a multiple of months.
   */
  public static final RollConvention SFE = RollConvention.of(StandardRollConventions.SFE.getName());

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

}
