/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.JulianFields;
import java.util.Arrays;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * This converts and stores all the date logic as doubles for CDS pricing on a particular date.<p>
 * For convenient ways to generate sets of CDSs
 * @see CdsAnalyticFactory
 */
public class CdsAnalytic {

  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;
  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;

  /** Curve daycount generally fixed to Act/365 in ISDA */
  private static final DayCount ACT_365 = DayCounts.ACT_365F;
  private static final DayCount ACT_360 = DayCounts.ACT_360;

  private final double _lgd;
  private final boolean _payAccOnDefault;
  private final CdsCoupon[] _coupons;

  //important time measures for the curve zero time ('now') using the curve DCC
  private final double _accStart; //the start of the first accrual period (usually < 0) 
  private final double _effectiveProtectionStart; //when protection starts (usually zero unless forward starting CDS)
  private final double _cashSettlementTime; //Time when CDS is cash settled (valuation time defaults to this)
  private final double _protectionEnd; //when the CDS ends 

  private final double _accrued;
  private final int _accruedDays;

  /**
   * Generates an analytic description of a CDS trade on a particular date.
   * This can then be passed to a analytic CDS pricer.
   * <p>
   * This using a weekend only calendar with a following convention. ACT/360 is used for accrual
   * and  ACT/365 to convert payment dates to year-fractions (doubles).
   * 
   * @param tradeDate  the trade date, this is the date other times are measured from (i.e. t = 0)
   * @param stepinDate  (aka Protection Effective date or assignment date). Date when party assumes ownership.
   *  This is usually T+1. This is when protection (and risk) starts in terms of the model. Note, this is
   *  sometimes just called the Effective Date, however this can cause confusion with the legal effective
   *  date which is T-60 or T-90.
   * @param valueDate  the valuation date. The date that values are PVed to.
   *  Is is normally today + 3 business days.  Aka cash-settle date.
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments. i.e. the number
   *  of days in the first  period (and thus the amount of the first premium payment) is counted from this date.
   * @param endDate  (aka maturity date) This is when the contract expires and protection ends -
   *  any default after this date does not trigger a payment. (the protection ends at end of day)
   * @param payAccOnDefault  is the accrued premium paid in the event of a default
   * @param paymentInterval  the nominal step between premium payments (e.g. 3 months, 6 months).
   * @param stubType  the stub convention
   * @param protectStart  if protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   * @param recoveryRate  the recovery rate
   */
  public CdsAnalytic(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate accStartDate,
      LocalDate endDate,
      boolean payAccOnDefault,
      Period paymentInterval,
      StubConvention stubType,
      boolean protectStart,
      double recoveryRate) {

    this(tradeDate, stepinDate, valueDate, accStartDate, endDate, payAccOnDefault,
        paymentInterval, stubType, protectStart, recoveryRate, FOLLOWING, DEFAULT_CALENDAR, ACT_360, ACT_365);
  }

  /**
   * Generates an analytic description of a CDS trade on a particular date.
   * This can then be passed to a analytic CDS pricer.
   * <p>
   * This using a weekend only calendar with a following convention. ACT/360 is used for accrual
   * and  ACT/365 to convert payment dates to year-fractions (doubles).
   * <p>
   * Note this uses a curve daycount of ACT/365 to match the ISDA methodology.
   *
   * @param tradeDate  the trade date, this is the date other times are measured from (i.e. t = 0)
   * @param stepinDate  (aka Protection Effective date or assignment date). Date when party assumes ownership.
   *  This is usually T+1. This is when protection (and risk) starts in terms of the model. Note, this is
   *  sometimes just called the Effective Date, however this can cause confusion with the legal effective
   *  date which is T-60 or T-90.
   * @param valueDate  the valuation date. The date that values are PVed to.
   *  Is is normally today + 3 business days.  Aka cash-settle date.
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments. i.e. the number
   *  of days in the first  period (and thus the amount of the first premium payment) is counted from this date.
   * @param endDate  (aka maturity date) This is when the contract expires and protection ends -
   *  any default after this date does not trigger a payment. (the protection ends at end of day)
   * @param payAccOnDefault  is the accrued premium paid in the event of a default
   * @param paymentInterval  the nominal step between premium payments (e.g. 3 months, 6 months).
   * @param stubType  the stub convention
   * @param protectStart  if protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   * @param recoveryRate  the recovery rate
   * @param businessdayAdjustmentConvention  how are adjustments for non-business days made
   * @param calendar  the holiday calendar defining what is a non-business day
   * @param accrualDayCount  the day count used for accrual
   */
  public CdsAnalytic(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate accStartDate,
      LocalDate endDate,
      boolean payAccOnDefault,
      Period paymentInterval,
      StubConvention stubType,
      boolean protectStart,
      double recoveryRate,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calendar,
      DayCount accrualDayCount) {

    this(tradeDate, stepinDate, valueDate, accStartDate, endDate, payAccOnDefault, paymentInterval,
        stubType, protectStart, recoveryRate, businessdayAdjustmentConvention, calendar, accrualDayCount, ACT_365);
  }

  /**
   * Generates an analytic description of a CDS trade on a particular date.
   * This can then be passed to a analytic CDS pricer.
   * 
   * @param tradeDate  the trade date, this is the date other times are measured from (i.e. t = 0)
   * @param stepinDate  (aka Protection Effective date or assignment date). Date when party assumes ownership.
   *  This is usually T+1. This is when protection (and risk) starts in terms of the model. Note, this is
   *  sometimes just called the Effective Date, however this can cause confusion with the legal effective
   *  date which is T-60 or T-90.
   * @param valueDate  the valuation date. The date that values are PVed to.
   *  Is is normally today + 3 business days.  Aka cash-settle date.
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments. i.e. the number
   *  of days in the first  period (and thus the amount of the first premium payment) is counted from this date.
   * @param endDate  (aka maturity date) This is when the contract expires and protection ends -
   *  any default after this date does not trigger a payment. (the protection ends at end of day)
   * @param payAccOnDefault  is the accrued premium paid in the event of a default
   * @param paymentInterval  the nominal step between premium payments (e.g. 3 months, 6 months).
   * @param stubType  the stub convention
   * @param protectStart  if protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   * @param recoveryRate  the recovery rate
   * @param businessdayAdjustmentConvention How are adjustments for non-business days made
   * @param calendar HolidayCalendar defining what is a non-business day
   * @param accrualDayCount Day count used for accrual
   * @param curveDayCount Day count used on curve (NOTE ISDA uses ACT/365 (fixed) and it is not recommended to change this)
   */
  public CdsAnalytic(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate accStartDate,
      LocalDate endDate,
      boolean payAccOnDefault,
      Period paymentInterval,
      StubConvention stubType,
      boolean protectStart,
      double recoveryRate,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calendar,
      DayCount accrualDayCount,
      DayCount curveDayCount) {

    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.notNull(stepinDate, "stepinDate");
    ArgChecker.notNull(valueDate, "valueDate");
    ArgChecker.notNull(accStartDate, "accStartDate");
    ArgChecker.notNull(endDate, "endDate");
    ArgChecker.notNull(paymentInterval, "tenor");
    ArgChecker.notNull(stubType, "stubType");
    ArgChecker.notNull(businessdayAdjustmentConvention, "businessdayAdjustmentConvention");
    ArgChecker.notNull(accrualDayCount, "accuralDayCount");
    ArgChecker.notNull(curveDayCount, "curveDayCount");
    ArgChecker.inRangeInclusive(recoveryRate, 0d, 1d, "recoveryRate");
    ArgChecker.isFalse(valueDate.isBefore(tradeDate), "Require valueDate >= today");
    ArgChecker.isFalse(stepinDate.isBefore(tradeDate), "Require stepin >= today");
    //TODO should not allow the accrual start to be after the stepin (protection start),
    // since this is 'free' protection. Currently some tests have this and need to be changed 
    ArgChecker.isFalse(tradeDate.isAfter(endDate), "CDS has expired");

    _payAccOnDefault = payAccOnDefault;

    LocalDate startDate = stepinDate.isAfter(accStartDate) ? stepinDate : accStartDate;
    LocalDate effectiveStartDate = protectStart ? startDate.minusDays(1) : startDate;

    _accStart = accStartDate.isBefore(tradeDate) ?
        -curveDayCount.yearFraction(accStartDate, tradeDate) :
        curveDayCount.yearFraction(tradeDate, accStartDate);
    _cashSettlementTime = curveDayCount.yearFraction(tradeDate, valueDate);
    _effectiveProtectionStart = effectiveStartDate.isBefore(tradeDate) ?
        -curveDayCount.yearFraction(effectiveStartDate, tradeDate) :
        curveDayCount.yearFraction(tradeDate, effectiveStartDate);
    _protectionEnd = curveDayCount.yearFraction(tradeDate, endDate);
    _lgd = 1 - recoveryRate;

    IsdaPremiumLegSchedule fullPaymentSchedule = new IsdaPremiumLegSchedule(
        accStartDate, endDate, paymentInterval, stubType, businessdayAdjustmentConvention, calendar, protectStart);
    IsdaPremiumLegSchedule paymentSchedule = IsdaPremiumLegSchedule.truncateSchedule(stepinDate, fullPaymentSchedule);
    _coupons = CdsCoupon.makeCoupons(tradeDate, paymentSchedule, protectStart, accrualDayCount, curveDayCount);

    LocalDate accStart = paymentSchedule.getAccStartDate(0);
    long firstJulianDate = accStart.getLong(JulianFields.MODIFIED_JULIAN_DAY);
    long secondJulianDate = stepinDate.getLong(JulianFields.MODIFIED_JULIAN_DAY);
    _accruedDays = secondJulianDate > firstJulianDate ? (int) (secondJulianDate - firstJulianDate) : 0;
    _accrued = accStart.isBefore(stepinDate) ? accrualDayCount.yearFraction(accStart, stepinDate) : 0.0;
  }

  public int getNumPayments() {
    return _coupons.length;
  }

  /**
   * Gets the payAccOnDefault.
   * @return the payAccOnDefault
   */
  public boolean isPayAccOnDefault() {
    return _payAccOnDefault;
  }

  /**
   * Gets the protectionFromStartOfDay.
   * @return the protectionFromStartOfDay
   */

  /**
   * The loss-given-default. This is 1 - recovery rate
   * @return the LGD
   */
  public double getLGD() {
    return _lgd;
  }

  /**
   * Gets year fraction (according to curve DCC) between the trade date and the cash-settle date 
   * @return the CashSettleTime
   */
  public double getCashSettleTime() {
    return _cashSettlementTime;
  }

  /**
   * Year fraction (according to curve DCC) from trade date to accrual start date.
   * This will be negative for spot starting CDS, but will be positive for forward starting CDS.
   * @return accrual start year-fraction.
   */
  public double getAccStart() {
    return _accStart;
  }

  /**
   * Year fraction (according to curve DCC) from trade date to effective protection start date.
   * The effective protection start date is the greater of the accrual start date
   * and the step-in date;  if protection is from start of day, this is  adjusted back one
   * day - so for a standard CDS it is the trade date.
   * @return the effectiveProtectionStart
   */
  public double getEffectiveProtectionStart() {
    return _effectiveProtectionStart;
  }

  /**
   *  Year fraction (according to curve DCC) from trade date to the maturity of the CDS.
   * @return the protectionEnd
   */
  public double getProtectionEnd() {
    return _protectionEnd;
  }

  /**
   * Get all the coupons on the premium leg.
   * @return the coupons.
   */
  public CdsCoupon[] getCoupons() {
    return _coupons;
  }

  /**
   * get a coupon at a particular index (zero based).
   * @param index the index
   * @return a coupon 
   */
  public CdsCoupon getCoupon(int index) {
    return _coupons[index];
  }

  /**
   * Gets the accrued premium per unit of (fractional) spread - i.e. if the quoted spread
   * (coupon)  was 500bps the actual accrued premium paid would be this times 0.05
   * @return the accrued premium per unit of (fractional) spread (and unit of notional)
   */
  public double getAccruedYearFraction() {
    return _accrued;
  }

  /**
   * Gets the accrued premium per unit of notional
   * @param fractionalSpread The <b>fraction</b> spread
   * @return the accrued premium
   */
  public double getAccruedPremium(double fractionalSpread) {
    return _accrued * fractionalSpread;
  }

  /**
   * Get the number of days of accrued premium.
   * @return Accrued days
   */
  public int getAccuredDays() {
    return _accruedDays;
  }

  private CdsAnalytic(
      double lgd,
      CdsCoupon[] coupons,
      double start,
      double effectiveProtectionStart,
      double protectionEnd,
      double valuationTime,
      boolean payAccOnDefault,
      double accrued,
      int accruedDays) {

    ArgChecker.noNulls(coupons, "coupons");

    _accStart = start;
    _effectiveProtectionStart = effectiveProtectionStart;
    _protectionEnd = protectionEnd;
    _cashSettlementTime = valuationTime;

    _lgd = lgd;
    _coupons = coupons;
    _payAccOnDefault = payAccOnDefault;
    _accrued = accrued;
    _accruedDays = accruedDays;
  }

  /**
   * produce a copy of the CDS with a new recovery rate
   * @param recoveryRate The recovery rate
   * @return a new CDS
   */
  public CdsAnalytic withRecoveryRate(double recoveryRate) {
    ArgChecker.inRangeInclusive(recoveryRate, 0d, 1d, "recoveryRate");
    return new CdsAnalytic(
        1 - recoveryRate, _coupons, _accStart, _effectiveProtectionStart, _protectionEnd,
        _cashSettlementTime, _payAccOnDefault, _accrued, _accruedDays);
  }

  /**
   * Generate a CDS with a time offset. The main use is to produce a forward starting CDS from a forward CDS.
   * A forward CDS is a CDS with a future trade date viewed from that date (i.e. it is a spot CDS view
   * from the future trade date). A forward starting CDS is a CDS (seen today) that starts on some future date.
   * The effect of this operation is to shift all time-to based numbers by offset.
   * 
   * @param offset The offset (in years) - must be positive 
   * @return an offset (i.e. forward starting) CDS 
   */
  public CdsAnalytic withOffset(double offset) {
    ArgChecker.isTrue(offset >= 0, "offset must be positive");
    if (offset == 0.0) {
      return this;
    }
    int n = getNumPayments();
    CdsCoupon[] coupons = new CdsCoupon[n];
    for (int i = 0; i < n; i++) {
      coupons[i] = _coupons[i].withOffset(offset);
    }
    return new CdsAnalytic(
        _lgd, coupons, _accStart + offset, _effectiveProtectionStart + offset,
        _protectionEnd + offset, _cashSettlementTime + offset, _payAccOnDefault, _accrued, _accruedDays);
  }

  //-------------------------------------------------------------------------
  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_accStart);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_accrued);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + _accruedDays;
    temp = Double.doubleToLongBits(_cashSettlementTime);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_coupons);
    temp = Double.doubleToLongBits(_effectiveProtectionStart);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_lgd);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + (_payAccOnDefault ? 1231 : 1237);
    temp = Double.doubleToLongBits(_protectionEnd);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CdsAnalytic other = (CdsAnalytic) obj;
    if (Double.doubleToLongBits(_accStart) != Double.doubleToLongBits(other._accStart)) {
      return false;
    }
    if (Double.doubleToLongBits(_accrued) != Double.doubleToLongBits(other._accrued)) {
      return false;
    }
    if (_accruedDays != other._accruedDays) {
      return false;
    }
    if (Double.doubleToLongBits(_cashSettlementTime) != Double.doubleToLongBits(other._cashSettlementTime)) {
      return false;
    }
    if (!Arrays.equals(_coupons, other._coupons)) {
      return false;
    }
    if (Double.doubleToLongBits(_effectiveProtectionStart) != Double.doubleToLongBits(other._effectiveProtectionStart)) {
      return false;
    }
    if (Double.doubleToLongBits(_lgd) != Double.doubleToLongBits(other._lgd)) {
      return false;
    }
    if (_payAccOnDefault != other._payAccOnDefault) {
      return false;
    }
    if (Double.doubleToLongBits(_protectionEnd) != Double.doubleToLongBits(other._protectionEnd)) {
      return false;
    }
    return true;
  }

}
