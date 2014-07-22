/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * A convention defining how to adjust a date if it falls on a day other than a business day.
 * <p>
 * This class is immutable and thread-safe.
 */
public interface DayCount {

  /**
   * The 'Act/Act ISDA' day count, which divides the actual number of days in a
   * leap year by 366 and the actual number of days in a standard year by 365.
   * <p>
   * The result is calculated in two parts.
   * The actual number of days in the period that fall in a leap year is divided by 366.
   * The actual number of days in the period that fall in a standard year is divided by 365.
   * The result is the sum of the two.
   * The first day in the period is included, the last day is excluded.
   * <p>
   * Also known as 'Actual/Actual'.
   */
  public static final DayCount DC_ACT_ACT_ISDA = DayCounts.DC_ACT_ACT_ISDA;
  /**
   * The 'Act/360' day count, which divides the actual number of days by 360.
   * <p>
   * The result is a simple division.
   * The numerator is the actual number of days in the period.
   * The denominator is always 360.
   * <p>
   * Also known as 'Actual/360' or 'French'.
   */
  public static final DayCount DC_ACT_360 = DayCounts.DC_ACT_360;
  /**
   * The 'Act/364' day count, which divides the actual number of days by 364.
   * <p>
   * The result is a simple division.
   * The numerator is the actual number of days in the period.
   * The denominator is always 364.
   * <p>
   * Also known as 'Actual/364'.
   */
  public static final DayCount DC_ACT_364 = DayCounts.DC_ACT_364;
  /**
   * The 'Act/365F' day count, which divides the actual number of days by 365.
   * <p>
   * The result is a simple division.
   * The numerator is the actual number of days in the period.
   * The denominator is always 365.
   * <p>
   * Also known as 'Actual/365 Fixed' or 'English'.
   */
  public static final DayCount DC_ACT_365F = DayCounts.DC_ACT_365F;
  /**
   * The 'Act/365.25' day count, which divides the actual number of days by 365.25.
   * <p>
   * The result is a simple division.
   * The numerator is the actual number of days in the period.
   * The denominator is always 365.25.
   */
  public static final DayCount DC_ACT_365_25 = DayCounts.DC_ACT_365_25;
  /**
   * The 'NL/365' day count, which divides the actual number of days omitting leap days by 365.
   * <p>
   * The result is a simple division.
   * The numerator is the actual number of days in the period minus the number of occurrences
   * of February 29, excluding the start date and including the end date.
   * The denominator is always 365.
   * <p>
   * Also known as 'Actual/365 No Leap'.
   */
  public static final DayCount DC_NL_365 = DayCounts.DC_NL_365;
  /**
   * The '30/360 ISDA' day count, which treats input day-of-month 31 specially.
   * <p>
   * The result is calculated as {@code (360 * deltaYear + 30 * deltaMonth + deltaDay) / 360}.
   * The deltaDay is then calculated once day-of-month adjustments have occurred.
   * If the second day-of-month is 31 and the first day-of-month is 30 or 31, change the second day-of-month to 30.
   * If the first day-of-month is 31, change the first day-of-month to 30.
   * <p>
   * Also known as '30/360 U.S. Municipal' or '30/360 Bond Basis'.
   */
  public static final DayCount DC_30_360_ISDA = DayCounts.DC_30_360_ISDA;
  /**
   * The '30U/360' day count, which treats input day-of-month 31 and end of February specially.
   * <p>
   * The result is calculated as {@code (360 * deltaYear + 30 * deltaMonth + deltaDay) / 360}.
   * The deltaDay is then calculated once day-of-month adjustments have occurred.
   * If the both dates are the last day of February, change the second day-of-month to 30.
   * If the first date is the last day of February, change the first day-of-month to 30.
   * If the second day-of-month is 31 and the first day-of-month is 30 or 31, change the second day-of-month to 30.
   * If the first day-of-month is 31, change the first day-of-month to 30.
   * <p>
   * This is the same as '30/360 ISDA' but with an additional end of February rule.
   * <p>
   * Also known as '30/360 US', '30US/360' or '30/360 SIA'.
   */
  public static final DayCount DC_30U_360 = DayCounts.DC_30U_360;
  /**
   * The '30E/360 ISDA' day count, which treats input day-of-month 31 and end of February specially.
   * <p>
   * The result is calculated as {@code (360 * deltaYear + 30 * deltaMonth + deltaDay) / 360}.
   * The deltaDay is then calculated once day-of-month adjustments have occurred.
   * If the first day-of-month is 31, change the first day-of-month to 30.
   * If the second day-of-month is 31, change the second day-of-month to 30.
   * If the first date is the last day of February, change the first day-of-month to 30.
   * If the second date is the last day of February, change the second day-of-month to 30.
   * <p>
   * Note that the last rule should be omitted when the second date is the maturity date,
   * however that is not implemented here.
   * <p>
   * Also known as '30E/360 ISDA' or 'German'.
   */
  public static final DayCount DC_30E_360_ISDA = DayCounts.DC_30E_360_ISDA;
  /**
   * The '30E/360' day count, which treats input day-of-month 31 specially.
   * <p>
   * The result is calculated as {@code (360 * deltaYear + 30 * deltaMonth + deltaDay) / 360}.
   * The deltaDay is then calculated once day-of-month adjustments have occurred.
   * If the first day-of-month is 31, it is changed to 30.
   * If the second day-of-month is 31, it is changed to 30.
   * <p>
   * Also known as '30/360 ISMA', '30/360 European', '30S/360 Special German' or 'Eurobond'.
   */
  public static final DayCount DC_30E_360 = DayCounts.DC_30E_360;
  /**
   * The '30E+/360' day count, which treats input day-of-month 31 specially.
   * <p>
   * The result is calculated as {@code (360 * deltaYear + 30 * deltaMonth + deltaDay) / 360}.
   * The deltaDay and deltaMonth are calculated once adjustments have occurred.
   * If the first day-of-month is 31, it is changed to 30.
   * If the second day-of-month is 31, it is changed to 1 and the second month is incremented.
   */
  public static final DayCount DC_30EPLUS_360 = DayCounts.DC_30EPLUS_360;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code BusinessDayConvention} from a unique name.
   * 
   * @param name  the unique name
   * @return the business convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static DayCount of(String name) {
    ArgChecker.notNull(name, "name");
    return Stream.of(DayCounts.values())
        .filter(bdc -> bdc.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the day count between the specified dates.
   * <p>
   * Given two dates, this method returns the fraction of a year between these
   * dates according to the convention.
   *
   * @param firstDate  the earlier date
   * @param secondDate  the later date
   * @return the day count fraction
   */
  double getDayCountFraction(LocalDate firstDate, LocalDate secondDate);

  /**
   * Gets the name that uniquely identifies this convention.
   * 
   * @return the unique name
   */
  @ToString
  String getName();

}
