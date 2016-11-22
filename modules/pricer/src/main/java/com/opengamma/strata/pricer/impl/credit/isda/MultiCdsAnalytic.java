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
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class MultiCdsAnalytic {

  private final double _lgd;
  private final boolean _payAccOnDefault;
  private final CdsCoupon[] _standardCoupons; //these will be common across many CDSs
  private final CdsCoupon[] _terminalCoupons; //these are the final coupons for each CDS 

  private final double _accStart;
  private final double _effectiveProtectionStart;
  private final double _cashSettlementTime;
  private final double[] _protectionEnd;

  private final double[] _accrued;
  private final int[] _accruedDays;

  private final int _totalPayments;
  private final int _nMaturities;
  private final int[] _matIndexToPayments;

  /**
   * Set up a strip of increasing maturity CDSs that have some coupons in common.  The trade date, step-in date and valuation date and
   * accrual start date are all common, as is the payment frequency. The maturities are expressed as integer multiples of the
   * payment interval from a reference date (the next IMM date after the trade date for standard CDSs) - this guarantees that premiums 
   * will be the same across several CDSs.
   * @param tradeDate The trade date
   * @param stepinDate (aka Protection Effective sate or assignment date). Date when party assumes ownership. This is usually T+1. This is when protection
   * (and risk) starts in terms of the model. Note, this is sometimes just called the Effective Date, however this can cause
   * confusion with the legal effective date which is T-60 or T-90.
   * @param cashSettlementDate The cash settlement date. The date that values are PVed to. Is is normally today + 3 business days.
   * @param accStartDate  Accrual Start Date. This is when the CDS nominally starts in terms of premium payments.  i.e. the number 
   * of days in the first period (and thus the amount of the first premium payment) is counted from this date.
   * @param maturityReferanceDate A reference date that maturities are measured from. For standard CDSSs, this is the next IMM  date after
   * the trade date, so the actually maturities will be some fixed periods after this.
   * @param maturityIndexes The maturities are fixed integer multiples of the payment interval, so for 6M, 1Y and 2Y tenors with a 3M 
   * payment interval, would require 2, 4, and 8 as the indices    
   * @param payAccOnDefault Is the accrued premium paid in the event of a default
   * @param paymentInterval The nominal step between premium payments (e.g. 3 months, 6 months).
   * @param stubType the stub convention
   * @param protectStart If protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   * @param recoveryRate The recovery rate
   * @param businessdayAdjustmentConvention How are adjustments for non-business days made
   * @param calendar HolidayCalendar defining what is a non-business day
   * @param accrualDayCount Day count used for accrual
   * @param curveDayCount Day count used on curve (NOTE ISDA uses ACT/365 and it is not recommended to change this)
   */
  public MultiCdsAnalytic(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate cashSettlementDate,
      LocalDate accStartDate,
      LocalDate maturityReferanceDate,
      int[] maturityIndexes,
      boolean payAccOnDefault,
      Tenor paymentInterval,
      StubConvention stubType,
      boolean protectStart,
      double recoveryRate,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calendar,
      DayCount accrualDayCount,
      DayCount curveDayCount) {

    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.notNull(stepinDate, "stepinDate");
    ArgChecker.notNull(cashSettlementDate, "cashSettlementDate");
    ArgChecker.notNull(accStartDate, "accStartDate");
    ArgChecker.notNull(maturityReferanceDate, "maturityReferanceDate");
    ArgChecker.notNull(paymentInterval, "tenor");
    ArgChecker.notNull(stubType, "stubType");
    ArgChecker.notNull(businessdayAdjustmentConvention, "businessdayAdjustmentConvention");
    ArgChecker.notNull(accrualDayCount, "accuralDayCount");
    ArgChecker.notNull(curveDayCount, "curveDayCount");
    ArgChecker.isFalse(cashSettlementDate.isBefore(tradeDate), "Require valueDate >= today");
    ArgChecker.isFalse(stepinDate.isBefore(tradeDate), "Require stepin >= today");
    ArgChecker.notEmpty(maturityIndexes, "maturityIndexes");

    _nMaturities = maturityIndexes.length;
    ArgChecker.isTrue(maturityIndexes[0] >= 0, "first maturity index < 0");
    for (int i = 1; i < _nMaturities; i++) {
      ArgChecker.isTrue(maturityIndexes[i] > maturityIndexes[i - 1], "maturityIndexes not ascending");
    }
    _payAccOnDefault = payAccOnDefault;

    _accStart = accStartDate.isBefore(tradeDate) ?
        -curveDayCount.yearFraction(accStartDate, tradeDate) :
        curveDayCount.yearFraction(tradeDate, accStartDate);
    LocalDate temp = stepinDate.isAfter(accStartDate) ? stepinDate : accStartDate;
    LocalDate effectiveStartDate = protectStart ? temp.minusDays(1) : temp;

    _cashSettlementTime = curveDayCount.yearFraction(tradeDate, cashSettlementDate);
    _effectiveProtectionStart = curveDayCount.yearFraction(tradeDate, effectiveStartDate);
    _lgd = 1 - recoveryRate;

    LocalDate[] maturities = new LocalDate[_nMaturities];
    _protectionEnd = new double[_nMaturities];
    Period period = paymentInterval.getPeriod();
    for (int i = 0; i < _nMaturities; i++) {
      Period tStep = period.multipliedBy(maturityIndexes[i]);
      maturities[i] = maturityReferanceDate.plus(tStep);
      _protectionEnd[i] = curveDayCount.yearFraction(tradeDate, maturities[i]);
    }

    IsdaPremiumLegSchedule fullPaymentSchedule = new IsdaPremiumLegSchedule(accStartDate, maturities[_nMaturities - 1], period,
        stubType, businessdayAdjustmentConvention, calendar, protectStart);
    //remove already expired coupons
    IsdaPremiumLegSchedule paymentSchedule = fullPaymentSchedule.truncateSchedule(stepinDate);
    int couponOffset = fullPaymentSchedule.getNumPayments() - paymentSchedule.getNumPayments();

    _totalPayments = paymentSchedule.getNumPayments();
    _standardCoupons = new CdsCoupon[_totalPayments - 1];
    for (int i = 0; i < (_totalPayments - 1); i++) { //The last coupon is actually a terminal coupon, so not included here
      _standardCoupons[i] = new CdsCoupon(
          tradeDate, paymentSchedule.getAccPaymentDateTriplet(i), protectStart, accrualDayCount, curveDayCount);
    }

    //find the terminal coupons 
    _terminalCoupons = new CdsCoupon[_nMaturities];
    _matIndexToPayments = new int[_nMaturities];
    _accruedDays = new int[_nMaturities];
    _accrued = new double[_nMaturities];
    long secondJulianDate = stepinDate.getLong(JulianFields.MODIFIED_JULIAN_DAY);
    for (int i = 0; i < _nMaturities; i++) {
      int index = fullPaymentSchedule.getNominalPaymentDateIndex(maturities[i]);
      if (index < 0) {
        throw new IllegalStateException("should never see this. There is a bug in the code.");
      }
      //maturity is unadjusted, but if protectionStart=true (i.e. standard CDS) there is effectively an extra day of accrued interest
      LocalDate accEnd = protectStart ? maturities[i].plusDays(1) : maturities[i];
      _terminalCoupons[i] = new CdsCoupon(
          tradeDate, fullPaymentSchedule.getAccStartDate(index), accEnd,
          fullPaymentSchedule.getPaymentDate(index), protectStart, accrualDayCount, curveDayCount);
      _matIndexToPayments[i] = index - couponOffset;
      //This will only matter for the edge case when the trade date is 1 day before maturity      
      LocalDate tDate2 = _matIndexToPayments[i] < 0 ?
          fullPaymentSchedule.getAccStartDate(couponOffset - 1) :
          paymentSchedule.getAccStartDate(0);
      long firstJulianDate = tDate2.getLong(JulianFields.MODIFIED_JULIAN_DAY);
      _accruedDays[i] = secondJulianDate > firstJulianDate ? (int) (secondJulianDate - firstJulianDate) : 0;
      _accrued[i] = tDate2.isBefore(stepinDate) ? accrualDayCount.yearFraction(tDate2, stepinDate) : 0.0;
    }
  }

  private MultiCdsAnalytic(
      double lgd,
      boolean payAccOnDefault,
      CdsCoupon[] standardCoupons,
      CdsCoupon[] terminalCoupons,
      double accStart,
      double effectiveProtectionStart,
      double valuationTime,
      double[] protectionEnd,
      double[] accrued,
      int[] accruedDays,
      int totalPayments,
      int nMaturities,
      int[] matIndexToPayments) {

    _lgd = lgd;
    _payAccOnDefault = payAccOnDefault;
    _standardCoupons = standardCoupons;
    _terminalCoupons = terminalCoupons;
    _accStart = accStart;
    _effectiveProtectionStart = effectiveProtectionStart;
    _cashSettlementTime = valuationTime;
    _protectionEnd = protectionEnd;
    _accrued = accrued;
    _accruedDays = accruedDays;
    _totalPayments = totalPayments;
    _nMaturities = nMaturities;
    _matIndexToPayments = matIndexToPayments;
  }

  public int getNumMaturities() {
    return _nMaturities;
  }

  /**
   * This is the number of payments for the largest maturity CDS 
   * @return totalPayments 
   */
  public int getTotalPayments() {
    return _totalPayments;
  }

  /**
   * get payment index for a particular maturity index. The standard coupon is one less than this
   * @param matIndex maturity index (0 for first maturity, etc)
   * @return payment index 
   */
  public int getPaymentIndexForMaturity(int matIndex) {
    return _matIndexToPayments[matIndex];
  }

  /**
   * Gets the payAccOnDefault.
   * @return the payAccOnDefault
   */
  public boolean isPayAccOnDefault() {
    return _payAccOnDefault;
  }

  /**
   * The loss-given-default. This is 1 - recovery rate
   * @return the LGD
   */
  public double getLGD() {
    return _lgd;
  }

  public MultiCdsAnalytic withRecoveryRate(double recovery) {
    return new MultiCdsAnalytic(
        1 - recovery, _payAccOnDefault, _standardCoupons, _terminalCoupons, _accStart,
        _effectiveProtectionStart, _cashSettlementTime, _protectionEnd, _accrued, _accruedDays,
        _totalPayments, _nMaturities, _matIndexToPayments);
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
   * and the step-in date;  if protection is from start of day, this is  adjusted back one day - 
   * so for a standard CDS it is the trade date.
   * @return the effectiveProtectionStart
   */
  public double getEffectiveProtectionStart() {
    return _effectiveProtectionStart;
  }

  /**
   *  Year fraction (according to curve DCC) from trade date to the maturity of the CDS at the given index (zero based). 
   *  @param matIndex the index 
   * @return the protectionEnd
   */
  public double getProtectionEnd(int matIndex) {
    return _protectionEnd[matIndex];
  }

  /**
   * Get the coupon for the CDS at the given index (zero based). 
   * @param matIndex the index 
   * @return A coupon 
   */
  public CdsCoupon getTerminalCoupon(int matIndex) {
    return _terminalCoupons[matIndex];
  }

  /** Get the standard (i.e. not the or terminal coupon of a CDS) at the given index
   * @param index the index
   * @return a coupon 
   */
  public CdsCoupon getStandardCoupon(int index) {
    return _standardCoupons[index];
  }

  public CdsCoupon[] getStandardCoupons() {
    return _standardCoupons;
  }

  /**
   * Gets the accrued premium per unit of (fractional) spread (i.e. if the quoted spread (coupon)  was 500bps the actual
   * accrued premium paid would be this times 0.05) for the CDS at the given index (zero based). 
  * @param matIndex the index 
   * @return the accrued premium per unit of (fractional) spread (and unit of notional)
   */
  public double getAccruedPremiumPerUnitSpread(int matIndex) {
    return _accrued[matIndex];
  }

  /**
   * Gets the accrued premium per unit of notional for the CDS at the given index (zero based). 
   * @param matIndex the index 
   * @param fractionalSpread The <b>fraction</b> spread
   * @return the accrued premium
   */
  public double getAccruedPremium(int matIndex, double fractionalSpread) {
    return _accrued[matIndex] * fractionalSpread;
  }

  /**
   * Get the number of days of accrued premium for the CDS at the given index (zero based)
  * @param matIndex the index 
   * @return Accrued days
   */
  public int getAccuredDays(int matIndex) {
    return _accruedDays[matIndex];
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_accStart);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_accrued);
    result = prime * result + Arrays.hashCode(_accruedDays);
    temp = Double.doubleToLongBits(_effectiveProtectionStart);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(_lgd);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + Arrays.hashCode(_matIndexToPayments);
    result = prime * result + _nMaturities;
    result = prime * result + (_payAccOnDefault ? 1231 : 1237);
    result = prime * result + Arrays.hashCode(_protectionEnd);
    result = prime * result + Arrays.hashCode(_standardCoupons);
    result = prime * result + Arrays.hashCode(_terminalCoupons);
    result = prime * result + _totalPayments;
    temp = Double.doubleToLongBits(_cashSettlementTime);
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
    MultiCdsAnalytic other = (MultiCdsAnalytic) obj;
    if (Double.doubleToLongBits(_accStart) != Double.doubleToLongBits(other._accStart)) {
      return false;
    }
    if (!Arrays.equals(_accrued, other._accrued)) {
      return false;
    }
    if (!Arrays.equals(_accruedDays, other._accruedDays)) {
      return false;
    }
    if (Double.doubleToLongBits(_effectiveProtectionStart) != Double.doubleToLongBits(other._effectiveProtectionStart)) {
      return false;
    }
    if (Double.doubleToLongBits(_lgd) != Double.doubleToLongBits(other._lgd)) {
      return false;
    }
    if (!Arrays.equals(_matIndexToPayments, other._matIndexToPayments)) {
      return false;
    }
    if (_nMaturities != other._nMaturities) {
      return false;
    }
    if (_payAccOnDefault != other._payAccOnDefault) {
      return false;
    }
    if (!Arrays.equals(_protectionEnd, other._protectionEnd)) {
      return false;
    }
    if (!Arrays.equals(_standardCoupons, other._standardCoupons)) {
      return false;
    }
    if (!Arrays.equals(_terminalCoupons, other._terminalCoupons)) {
      return false;
    }
    if (_totalPayments != other._totalPayments) {
      return false;
    }
    if (Double.doubleToLongBits(_cashSettlementTime) != Double.doubleToLongBits(other._cashSettlementTime)) {
      return false;
    }
    return true;
  }

}
