/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class CreditDefaultSwapDes {

  private final CdsCouponDes[] _coupons;
  private final boolean _payAccOnDefault;
  private final DayCount _accrualDayCount;

  public CreditDefaultSwapDes(
      LocalDate accStartDate,
      LocalDate protectionStartDate,
      LocalDate protectionEndDate,
      boolean payAccOnDefault,
      Period paymentInterval,
      StubConvention stubType,
      boolean isProtectStart,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calendar, DayCount accrualDayCount) {

    ArgChecker.notNull(accStartDate, "accStartDate");
    ArgChecker.notNull(protectionStartDate, "protectionStartDate");
    ArgChecker.notNull(protectionEndDate, "protectionEndDate");
    ArgChecker.notNull(paymentInterval, "tenor");
    ArgChecker.notNull(stubType, "stubType");
    ArgChecker.notNull(businessdayAdjustmentConvention, "businessdayAdjustmentConvention");
    ArgChecker.notNull(accrualDayCount, "accuralDayCount");
    ArgChecker.isTrue(
        protectionEndDate.isAfter(protectionStartDate),
        "protectionEndDate ({}) must be after protectionStartDate ({})", protectionStartDate, protectionEndDate);

    IsdaPremiumLegSchedule fullPaymentSchedule = new IsdaPremiumLegSchedule(
        accStartDate, protectionEndDate, paymentInterval, stubType, businessdayAdjustmentConvention, calendar, isProtectStart);
    IsdaPremiumLegSchedule paymentSchedule = IsdaPremiumLegSchedule.truncateSchedule(protectionStartDate, fullPaymentSchedule);

    _coupons = CdsCouponDes.makeCoupons(paymentSchedule, accrualDayCount);
    _payAccOnDefault = payAccOnDefault;
    _accrualDayCount = accrualDayCount;
  }

  /**
   * Gets the coupons.
   * @return the coupons
   */
  public CdsCouponDes[] getCoupons() {
    return _coupons;
  }

  /**
   * Gets the payAccOnDefault.
   * @return the payAccOnDefault
   */
  public boolean isPayAccOnDefault() {
    return _payAccOnDefault;
  }

  /**
   * Gets the accrualDayCount.
   * @return the accrualDayCount
   */
  public DayCount getAccrualDayCount() {
    return _accrualDayCount;
  }

}
