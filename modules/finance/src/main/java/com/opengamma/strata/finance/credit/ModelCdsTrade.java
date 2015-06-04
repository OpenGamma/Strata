package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.StubConvention;

import java.time.LocalDate;
import java.time.Period;

public interface ModelCdsTrade {

  /**
   * tradeDate The trade date
   */
  LocalDate getTradeDate();

  /**
   * Typically T+1 unadjusted. Required by the model.
   */
  LocalDate getStepInDate();

  /**
   * The date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
   */
  LocalDate getCashSettleDate();

  /**
   * accStartDate This is when the CDS nominally starts in terms of premium payments.
   * i.e. the number of days in the first period (and thus the amount of the first premium payment)
   * is counted from this date.
   * <p>
   * This should be adjusted according business day and holidays
   */
  LocalDate getAccStartDate();


  /**
   * endDate (aka maturity date) This is when the contract expires and protection ends -
   * any default after this date does not trigger a payment. (the protection ends at end of day)
   * <p>
   * This is an adjusted date and can fall on a holiday or weekend.
   */
  LocalDate getEndDate();

  /**
   * payAccOnDefault Is the accrued premium paid in the event of a default
   */
  boolean isPayAccOnDefault();

  /**
   * paymentInterval The nominal step between premium payments (e.g. 3 months, 6 months).
   */
  Period getPaymentInterval();

  /**
   * stubType stubType Options are FRONTSHORT, FRONTLONG, BACKSHORT, BACKLONG or NONE
   * - <b>Note</b> in this code NONE is not allowed
   */
  StubConvention getStubConvention();

  /**
   * businessdayAdjustmentConvention How are adjustments for non-business days made
   */
  BusinessDayConvention getBusinessdayAdjustmentConvention();

  /**
   * calendar HolidayCalendar defining what is a non-business day
   */
  HolidayCalendar getCalendar();

  /**
   * accrualDayCount Day count used for accrual
   */
  DayCount getAccrualDayCount();

  /**
   * are we buying protection and paying fees or are we selling protection and receiving fees
   */
  BuySell getBuySellProtection();

  /**
   * optional upfront fee amount, will be NaN if there is no fee
   */
  double getUpfrontFeeAmount();

  /**
   * optional upfront fee date, will throw if called and there is no fee
   * check the fee amount first before calling
   */
  LocalDate getUpfrontFeePaymentDate();

  /**
   * coupon used to calc fee payments
   */
  double getCoupon();

  /**
   * notional amount used to calc fee payments
   */
  double getNotional();

  /**
   * currency fees are paid in
   */
  Currency getCurrency();
}
