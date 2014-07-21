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
 * Implementation of standard business day conventions.
 */
enum BusinessDayConventions implements BusinessDayConvention {

  /**
   * See {@link BusinessDayConvention#NO_ADJUST}.
   */
  NO_ADJUST {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      return inputDate;
    }
    @Override
    public String getName() {
      return "NoAdjust";
    }
  },
  /**
   * See {@link BusinessDayConvention#FOLLOWING}.
   */
  FOLLOWING {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      return (businessDays.isBusinessDay(inputDate) ? inputDate : businessDays.next(inputDate));
    }
    @Override
    public String getName() {
      return "Following";
    }
  },
  /**
   * See {@link BusinessDayConvention#MODIFIED_FOLLOWING}.
   */
  MODIFIED_FOLLOWING {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      if (businessDays.isBusinessDay(inputDate)) {
        return inputDate;
      }
      LocalDate adjusted = businessDays.next(inputDate);
      if (adjusted.getMonth() != inputDate.getMonth()) {
        adjusted = businessDays.previous(inputDate);
      }
      return adjusted;
    }
    @Override
    public String getName() {
      return "ModifiedFollowing";
    }
  },
  /**
   * See {@link BusinessDayConvention#MODIFIED_FOLLOWING_BI_MONTHLY}.
   */
  MODIFIED_FOLLOWING_BI_MONTHLY {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      if (businessDays.isBusinessDay(inputDate)) {
        return inputDate;
      }
      LocalDate adjusted = businessDays.next(inputDate);
      if (adjusted.getMonth() != inputDate.getMonth() ||
          (adjusted.getDayOfMonth() > 15 && inputDate.getDayOfMonth() <= 15)) {
        adjusted = businessDays.previous(inputDate);
      }
      return adjusted;
    }
    @Override
    public String getName() {
      return "ModifiedFollowingBiMonthly";
    }
  },
  /**
   * See {@link BusinessDayConvention#PRECEDING}.
   */
  PRECEDING {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      return (businessDays.isBusinessDay(inputDate) ? inputDate : businessDays.previous(inputDate));
    }
    @Override
    public String getName() {
      return "Preceding";
    }
  },
  /**
   * See {@link BusinessDayConvention#MODIFIED_PRECEDING}.
   */
  MODIFIED_PRECEDING {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      if (businessDays.isBusinessDay(inputDate)) {
        return inputDate;
      }
      LocalDate adjusted = businessDays.previous(inputDate);
      if (adjusted.getMonth() != inputDate.getMonth()) {
        adjusted = businessDays.next(inputDate);
      }
      return adjusted;
    }
    @Override
    public String getName() {
      return "ModifiedPreceding";
    }
  },
  /**
   * See {@link BusinessDayConvention#NEAREST}.
   */
  NEAREST {
    @Override
    public LocalDate adjust(LocalDate inputDate, BusinessDayCalendar businessDays) {
      ArgChecker.notNull(inputDate, "inputDate");
      ArgChecker.notNull(businessDays, "businessDays");
      if (businessDays.isBusinessDay(inputDate)) {
        return inputDate;
      }
      if (inputDate.getDayOfWeek() == SUNDAY || inputDate.getDayOfWeek() == MONDAY) {
        return businessDays.next(inputDate);
      } else {
        return businessDays.previous(inputDate);
      }
    }
    @Override
    public String getName() {
      return "Nearest";
    }
  };

}
