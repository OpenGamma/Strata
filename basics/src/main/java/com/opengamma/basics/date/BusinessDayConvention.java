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
public interface BusinessDayConvention {

  /**
   * The 'NoAdjust' convention which makes no adjustment.
   * <p>
   * The input date will not be adjusted even if it is not a business day.
   */
  public static final BusinessDayConvention NO_ADJUST = BusinessDayConventions.NO_ADJUST;
  /**
   * The 'Following' convention which adjusts to the next business day.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day.
   */
  public static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;
  /**
   * The 'ModifiedFollowing' convention which adjusts to the next business day without crossing month end.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day unless that day is in a different
   * calendar month, in which case the previous business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_FOLLOWING = BusinessDayConventions.MODIFIED_FOLLOWING;
  /**
   * The 'ModifiedFollowingBiMonthly' convention which adjusts to the next business day without
   * crossing mid-month or month end.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The month is divided into two parts, the first half, the 1st to 15th and the 16th onwards.
   * The adjusted date is the next business day unless that day is in a different half-month,
   * in which case the previous business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_FOLLOWING_BI_MONTHLY =
      BusinessDayConventions.MODIFIED_FOLLOWING_BI_MONTHLY;
  /**
   * The 'Preceding' convention which adjusts to the previous business day.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day.
   */
  public static final BusinessDayConvention PRECEDING = BusinessDayConventions.PRECEDING;
  /**
   * The 'ModifiedPreceding' convention which adjusts to the previous business day without crossing month start.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day unless that day is in a different
   * calendar month, in which case the next business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_PRECEDING = BusinessDayConventions.MODIFIED_PRECEDING;
  /**
   * The 'Nearest' convention which adjusts Sunday and Monday forward, and other days backward.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * If the input is Sunday or Monday then the next business day is returned.
   * Otherwise the previous business day is returned.
   * <p>
   * Note that despite the name, the algorithm may not return the business day that is actually nearest.
   */
  public static final BusinessDayConvention NEAREST = BusinessDayConventions.NEAREST;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code BusinessDayConvention} from a unique name.
   * 
   * @param name  the unique name
   * @return the business convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static BusinessDayConvention of(String name) {
    ArgChecker.notNull(name, "name");
    return Stream.of(BusinessDayConventions.values())
        .filter(bdc -> bdc.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the date as necessary if the date is not a business day.
   * 
   * @param inputDate  the date to adjust
   * @param businessDays  the definition of which days are business days
   * @return the adjusted date
   */
  LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays);

  /**
   * Gets the name that uniquely identifies this convention.
   * 
   * @return the unique name
   */
  @ToString
  String getName();

}
