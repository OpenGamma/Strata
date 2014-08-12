/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;

/**
 * Constants and implementations for standard business day conventions.
 * <p>
 * The purpose of each convention is to define how to handle non-business days.
 * When processing dates in finance, it is typically intended that non-business days,
 * such as weekends and holidays, are converted to a nearby valid business day.
 * The convention, in conjunction with a {@linkplain BusinessDayCalendar business day calendar},
 * defines exactly how the adjustment should be made.
 */
public final class BusinessDayConventions {

  /**
   * The 'NoAdjust' convention which makes no adjustment.
   * <p>
   * The input date will not be adjusted even if it is not a business day.
   */
  public static final BusinessDayConvention NO_ADJUST = Standard.NO_ADJUST;
  /**
   * The 'Following' convention which adjusts to the next business day.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day.
   */
  public static final BusinessDayConvention FOLLOWING = Standard.FOLLOWING;
  /**
   * The 'ModifiedFollowing' convention which adjusts to the next business day without crossing month end.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the next business day unless that day is in a different
   * calendar month, in which case the previous business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_FOLLOWING = Standard.MODIFIED_FOLLOWING;
  /**
   * The 'ModifiedFollowingBiMonthly' convention which adjusts to the next business day without
   * crossing mid-month or month end.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The month is divided into two parts, the first half, the 1st to 15th and the 16th onwards.
   * The adjusted date is the next business day unless that day is in a different half-month,
   * in which case the previous business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_FOLLOWING_BI_MONTHLY = Standard.MODIFIED_FOLLOWING_BI_MONTHLY;
  /**
   * The 'Preceding' convention which adjusts to the previous business day.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day.
   */
  public static final BusinessDayConvention PRECEDING = Standard.PRECEDING;
  /**
   * The 'ModifiedPreceding' convention which adjusts to the previous business day without crossing month start.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * The adjusted date is the previous business day unless that day is in a different
   * calendar month, in which case the next business day is returned.
   */
  public static final BusinessDayConvention MODIFIED_PRECEDING = Standard.MODIFIED_PRECEDING;
  /**
   * The 'Nearest' convention which adjusts Sunday and Monday forward, and other days backward.
   * <p>
   * If the input date is not a business day then the date is adjusted.
   * If the input is Sunday or Monday then the next business day is returned.
   * Otherwise the previous business day is returned.
   * <p>
   * Note that despite the name, the algorithm may not return the business day that is actually nearest.
   */
  public static final BusinessDayConvention NEAREST = Standard.NEAREST;

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private BusinessDayConventions() {
  }

  //-------------------------------------------------------------------------
  /**
   * Standard business day conventions.
   */
  enum Standard implements BusinessDayConvention {

    // make no adjustment
    NO_ADJUST("NoAdjust") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        return date;
      }
    },
    // next business day
    FOLLOWING("Following") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        return (businessDays.isBusinessDay(date) ? date : businessDays.next(date));
      }
    },
    // next business day unless over a month end
    MODIFIED_FOLLOWING("ModifiedFollowing") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        if (businessDays.isBusinessDay(date)) {
          return date;
        }
        LocalDate adjusted = businessDays.next(date);
        if (adjusted.getMonth() != date.getMonth()) {
          adjusted = businessDays.previous(date);
        }
        return adjusted;
      }
    },
    // next business day unless over a month end or mid
    MODIFIED_FOLLOWING_BI_MONTHLY("ModifiedFollowingBiMonthly") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        if (businessDays.isBusinessDay(date)) {
          return date;
        }
        LocalDate adjusted = businessDays.next(date);
        if (adjusted.getMonth() != date.getMonth() ||
            (adjusted.getDayOfMonth() > 15 && date.getDayOfMonth() <= 15)) {
          adjusted = businessDays.previous(date);
        }
        return adjusted;
      }
    },
    // previous business day
    PRECEDING("Preceding") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        return (businessDays.isBusinessDay(date) ? date : businessDays.previous(date));
      }
    },
    // previous business day unless over a month end
    MODIFIED_PRECEDING("ModifiedPreceding") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        if (businessDays.isBusinessDay(date)) {
          return date;
        }
        LocalDate adjusted = businessDays.previous(date);
        if (adjusted.getMonth() != date.getMonth()) {
          adjusted = businessDays.next(date);
        }
        return adjusted;
      }
    },
    // next business day if Sun/Mon, otherwise previous
    NEAREST("Nearest") {
      @Override
      public LocalDate adjust(LocalDate date, BusinessDayCalendar businessDays) {
        ArgChecker.notNull(date, "date");
        ArgChecker.notNull(businessDays, "businessDays");
        if (businessDays.isBusinessDay(date)) {
          return date;
        }
        if (date.getDayOfWeek() == SUNDAY || date.getDayOfWeek() == MONDAY) {
          return businessDays.next(date);
        } else {
          return businessDays.previous(date);
        }
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

}
