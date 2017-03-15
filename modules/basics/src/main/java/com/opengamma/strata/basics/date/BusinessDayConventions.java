/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard business day conventions.
 * <p>
 * The purpose of each convention is to define how to handle non-business days.
 * When processing dates in finance, it is typically intended that non-business days,
 * such as weekends and holidays, are converted to a nearby valid business day.
 * The convention, in conjunction with a {@linkplain HolidayCalendar holiday calendar},
 * defines exactly how the adjustment should be made.
 */
public final class BusinessDayConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<BusinessDayConvention> ENUM_LOOKUP = ExtendedEnum.of(BusinessDayConvention.class);

  /**
   * The 'NoAdjust' convention which makes no adjustment.
   * <p>
   * The input date will not be adjusted even if it is not a business day.
   */
  public static final BusinessDayConvention NO_ADJUST =
      BusinessDayConvention.of(StandardBusinessDayConventions.NO_ADJUST.getName());
  /**
   * The 'Following' convention which adjusts to the next business day.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day.
   */
  public static final BusinessDayConvention FOLLOWING =
      BusinessDayConvention.of(StandardBusinessDayConventions.FOLLOWING.getName());
  /**
   * The 'ModifiedFollowing' convention which adjusts to the next business day without crossing month end.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day unless that day is in a different
   * calendar month, in which case the previous business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_FOLLOWING =
      BusinessDayConvention.of(StandardBusinessDayConventions.MODIFIED_FOLLOWING.getName());
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
      BusinessDayConvention.of(StandardBusinessDayConventions.MODIFIED_FOLLOWING_BI_MONTHLY.getName());
  /**
   * The 'Preceding' convention which adjusts to the previous business day.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day.
   */
  public static final BusinessDayConvention PRECEDING =
      BusinessDayConvention.of(StandardBusinessDayConventions.PRECEDING.getName());
  /**
   * The 'ModifiedPreceding' convention which adjusts to the previous business day without crossing month start.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day unless that day is in a different
   * calendar month, in which case the next business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_PRECEDING =
      BusinessDayConvention.of(StandardBusinessDayConventions.MODIFIED_PRECEDING.getName());
  /**
   * The 'Nearest' convention which adjusts Sunday and Monday forward, and other days backward.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * If the input is Sunday or Monday then the next business day is returned.
   * Otherwise the previous business day is returned.
   * <p>
   * Note that despite the name, the algorithm may not return the business day that is actually nearest.
   */
  public static final BusinessDayConvention NEAREST =
      BusinessDayConvention.of(StandardBusinessDayConventions.NEAREST.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private BusinessDayConventions() {
  }

}
