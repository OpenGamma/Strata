/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.ArgChecker;

/**
 * This represents one payment period on the premium leg of a CDS
 */
public class CdsCoupon {

  private static final DayCount ACT_365 = DayCounts.ACT_365F;
  private static final DayCount ACT_360 = DayCounts.ACT_360;
  private static final boolean PROTECTION_FROM_START = true;

  private final double effStart;
  private final double effEnd;
  private final double paymentTime;
  private final double yearFrac;
  private final double ycRatio;

  /**
   * Make a set of CDSCoupon used by {@link CdsAnalytic} given a trade date and the schedule of the accrual periods.
   * 
   * @param tradeDate The trade date 
   * @param leg schedule of the accrual periods
   * @param protectionFromStartOfDay If true the protection is from the start of day and the effective accrual 
   *  start and end dates are one day less. The exception is the final accrual end date which has one day 
   *  added (if  protectionFromStartOfDay = true) in ISDAPremiumLegSchedule to compensate for this, so the
   *  accrual end date is just the CDS maturity.
   *  The effect of having protectionFromStartOfDay = true is to add an extra day of protection.
   * @param accrualDCC The day count used to compute accrual periods 
   * @param curveDCC  Day count used on curve (NOTE ISDA uses ACT/365 (fixed) and it is not recommended to change this)
   * @see CdsAnalytic
   * @return A set of CDSCoupon
   */
  public static CdsCoupon[] makeCoupons(
      LocalDate tradeDate,
      IsdaPremiumLegSchedule leg,
      boolean protectionFromStartOfDay,
      DayCount accrualDCC,
      DayCount curveDCC) {

    ArgChecker.notNull(leg, "leg");
    int n = leg.getNumPayments();
    CdsCoupon[] res = new CdsCoupon[n];
    for (int i = 0; i < n; i++) {
      LocalDate[] dates = leg.getAccPaymentDateTriplet(i);
      res[i] = new CdsCoupon(tradeDate, dates[0], dates[1], dates[2], protectionFromStartOfDay, accrualDCC, curveDCC);
    }
    return res;
  }

  /**
   * Make a set of CDSCoupon used by {@link CdsAnalytic} given a trade date and a set of {@link CdsCouponDes}.
   * 
   * @param tradeDate The trade date 
   * @param couponsDes Description of CDS accrual periods with LocalDate 
   * @param protectionFromStartOfDay If true the protection is from the start of day and the effective accrual
   *  start and end dates are one day less. The exception is the accrual end date which should have one day
   *  added (if  protectionFromStartOfDay = true) in the CDSCouponDes to compensate for this, so the 
   *  accrual end date is just the CDS maturity.
   *  The effect of having protectionFromStartOfDay = true is to add an extra day of protection.
   * @param curveDCC Day count used on curve (NOTE ISDA uses ACT/365 (fixed) and it is not recommended to change this)
   * @return A set of CDSCoupon
   */
  public static CdsCoupon[] makeCoupons(
      LocalDate tradeDate,
      CdsCouponDes[] couponsDes,
      boolean protectionFromStartOfDay,
      DayCount curveDCC) {

    ArgChecker.noNulls(couponsDes, "couponsDes");
    int n = couponsDes.length;
    ArgChecker.isTrue(couponsDes[n - 1].getPaymentDate().isAfter(tradeDate), "all coupons have expired");
    int count = 0;
    while (tradeDate.isAfter(couponsDes[count].getPaymentDate())) {
      count++;
    }
    int nCoupons = n - count;
    CdsCoupon[] coupons = new CdsCoupon[nCoupons];
    for (int i = 0; i < nCoupons; i++) {
      coupons[i] = new CdsCoupon(tradeDate, couponsDes[i + count], protectionFromStartOfDay, curveDCC);
    }
    return coupons;
  }

  /**
   * Turn a date based description of a CDS accrual period ({@link CdsCouponDes}) into an analytic description
   * ({@link CdsCoupon}). This has protection from  start of day and uses ACT/360 for the accrual day count.
   * 
   * @param tradeDate The trade date 
   * @param coupon A date based description of a CDS accrual period 
   */
  public CdsCoupon(LocalDate tradeDate, CdsCouponDes coupon) {
    this(tradeDate, coupon, PROTECTION_FROM_START, ACT_360);
  }

  /**
   * Turn a date based description of a CDS accrual period ({@link CdsCouponDes}) into an analytic description
   * ({@link CdsCoupon}). This has protection from  start of day and uses ACT/360 for the accrual day count.
   * 
   * @param tradeDate The trade date 
   * @param coupon A date based description of a CDS accrual period 
   * @param curveDCC Day count used on curve (NOTE ISDA uses ACT/365 (fixed) and it is not recommended to change this)
   */
  public CdsCoupon(LocalDate tradeDate, CdsCouponDes coupon, DayCount curveDCC) {
    this(tradeDate, coupon, PROTECTION_FROM_START, curveDCC);
  }

  /**
  * Turn a date based description of a CDS accrual period ({@link CdsCouponDes}) into an analytic description
  * ({@link CdsCoupon}). This uses ACT/360 for the accrual day count.
  * 
   * @param tradeDate The trade date 
   * @param coupon A date based description of a CDS accrual period 
   * @param protectionFromStartOfDay If true the protection is from the start of day and the effective accrual
   *  start and end dates are one day less. The exception is the accrual end date which should have one day
   *  added (if  protectionFromStartOfDay = true) in the CDSCouponDes to compensate for this, so the 
   *  accrual end date is just the CDS maturity.
   *  The effect of having protectionFromStartOfDay = true is to add an extra day of protection.
   */
  public CdsCoupon(LocalDate tradeDate, CdsCouponDes coupon, boolean protectionFromStartOfDay) {
    this(tradeDate, coupon, protectionFromStartOfDay, ACT_360);
  }

  /**
   * Turn a date based description of a CDS accrual period ({@link CdsCouponDes}) into an analytic description
   * ({@link CdsCoupon}). This uses ACT/360 for the accrual day count.
   * 
   * @param tradeDate The trade date 
   * @param coupon A date based description of a CDS accrual period 
   * @param protectionFromStartOfDay If true the protection is from the start of day and the effective accrual
   *  start and end dates are one day less. The exception is the accrual end date which should have one day
   *  added (if  protectionFromStartOfDay = true) in the CDSCouponDes to compensate for this, so the
   *  accrual end date is just the CDS maturity.
   *  The effect of having protectionFromStartOfDay = true is to add an extra day of protection.
   * @param curveDCC Day count used on curve (NOTE ISDA uses ACT/365 (fixed) and it is not recommended to change this)
   */
  public CdsCoupon(LocalDate tradeDate, CdsCouponDes coupon, boolean protectionFromStartOfDay, DayCount curveDCC) {
    ArgChecker.notNull(coupon, "coupon");
    ArgChecker.notNull(curveDCC, "curveDCC");
    ArgChecker.isFalse(tradeDate.isAfter(coupon.getPaymentDate()), "coupon payment is in the past");

    LocalDate effStart = protectionFromStartOfDay ? coupon.getAccStart().minusDays(1) : coupon.getAccStart();
    LocalDate effEnd = protectionFromStartOfDay ? coupon.getAccEnd().minusDays(1) : coupon.getAccEnd();

    this.effStart = effStart.isBefore(tradeDate) ?
        -curveDCC.yearFraction(effStart, tradeDate) :
        curveDCC.yearFraction(tradeDate, effStart);
    this.effEnd = curveDCC.yearFraction(tradeDate, effEnd);
    this.paymentTime = curveDCC.yearFraction(tradeDate, coupon.getPaymentDate());
    this.yearFrac = coupon.getYearFrac();
    this.ycRatio = yearFrac / curveDCC.yearFraction(coupon.getAccStart(), coupon.getAccEnd());
  }

  /**
   * Setup a analytic description (i.e. involving only doubles) of a single CDS premium payment period
   * seen from a particular trade date. Protection is taken from start of day; ACT/360 is used for the accrual
   * DCC and ACT/365F for the curve DCC.
   * 
   * @param tradeDate The trade date (this is the base date that discount factors and survival probabilities are measured from)
   * @param premiumDateTriplet The three dates: start and end of the accrual period and the payment time 
   */
  public CdsCoupon(LocalDate tradeDate, LocalDate... premiumDateTriplet) {
    this(toDoubles(tradeDate, PROTECTION_FROM_START, ACT_360, ACT_365, premiumDateTriplet));
  }

  /**
   *
   * Setup a analytic description (i.e. involving only doubles) of a single CDS premium payment period
   * seen from a particular trade date.  ACT/360 is used for the accrual DCC and ACT/365F for the curve DCC.
   * 
   * @param tradeDate The trade date (this is the base date that discount factors and survival probabilities are measured from)
   * @param accStart The start of the accrual period 
   * @param accEnd The end of the accrual period 
   * @param paymentDate The date of the premium payment 
   * @param protectionFromStartOfDay true if protection is from the start of day (true for standard CDS) 
   */
  public CdsCoupon(
      LocalDate tradeDate,
      LocalDate accStart,
      LocalDate accEnd,
      LocalDate paymentDate,
      boolean protectionFromStartOfDay) {

    this(toDoubles(tradeDate, protectionFromStartOfDay, ACT_360, ACT_365, accStart, accEnd, paymentDate));
  }

  /**
   * Setup a analytic description (i.e. involving only doubles) of a single CDS premium payment period
   * seen from a particular trade date.
   * 
   * @param tradeDate The trade date (this is the base date that discount factors and survival probabilities are measured from)
   * @param premiumDateTriplet  The three dates: start and end of the accrual period and the payment time 
   * @param protectionFromStartOfDay true if protection is from the start of day (true for standard CDS) 
   * @param accrualDCC The day-count-convention used for calculation the accrual period (ACT/360 for standard CDS) 
   * @param curveDCC The day-count-convention used for converting dates to time intervals along curves - this should be ACT/365F 
   */
  public CdsCoupon(
      LocalDate tradeDate,
      LocalDate[] premiumDateTriplet,
      boolean protectionFromStartOfDay,
      DayCount accrualDCC,
      DayCount curveDCC) {

    this(toDoubles(tradeDate, protectionFromStartOfDay, accrualDCC, curveDCC, premiumDateTriplet));
  }

  /**
   * Setup a analytic description (i.e. involving only doubles) of a single CDS premium payment period
   * seen from a particular trade date.
   * 
   * @param tradeDate The trade date (this is the base date that discount factors and survival probabilities are measured from)
   * @param accStart The start of the accrual period 
   * @param accEnd The end of the accrual period 
   * @param paymentDate The date of the premium payment 
   * @param protectionFromStartOfDay true if protection is from the start of day (true for standard CDS) 
   * @param accrualDCC The day-count-convention used for calculation the accrual period (ACT/360 for standard CDS) 
   * @param curveDCC The day-count-convention used for converting dates to time intervals along curves - this should be ACT/365F 
   */
  public CdsCoupon(
      LocalDate tradeDate,
      LocalDate accStart,
      LocalDate accEnd,
      LocalDate paymentDate,
      boolean protectionFromStartOfDay,
      DayCount accrualDCC,
      DayCount curveDCC) {

    this(toDoubles(tradeDate, protectionFromStartOfDay, accrualDCC, curveDCC, accStart, accEnd, paymentDate));
  }

  private CdsCoupon(double... data) {
    this.effStart = data[0];
    this.effEnd = data[1];
    this.paymentTime = data[2];
    this.yearFrac = data[3];
    this.ycRatio = data[4];
  }

  @SuppressWarnings("unused")
  private CdsCoupon(CdsCoupon other) {
    ArgChecker.notNull(other, "other");
    this.paymentTime = other.paymentTime;
    this.yearFrac = other.yearFrac;
    this.effStart = other.effStart;
    this.effEnd = other.effEnd;
    this.ycRatio = other.ycRatio;
  }

  private static double[] toDoubles(
      LocalDate tradeDate,
      boolean protectionFromStartOfDay,
      DayCount accrualDCC,
      DayCount curveDCC,
      LocalDate... premDates) {

    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.noNulls(premDates, "premDates");
    ArgChecker.isTrue(3 == premDates.length, "premDates must be length 3");
    ArgChecker.notNull(accrualDCC, "accrualDCC");
    ArgChecker.notNull(curveDCC, "curveDCC");
    LocalDate accStart = premDates[0];
    LocalDate accEnd = premDates[1];
    LocalDate paymentDate = premDates[2];
    ArgChecker.isTrue(accEnd.isAfter(accStart), "require accEnd after accStart");
    ArgChecker.isFalse(tradeDate.isAfter(paymentDate), "coupon payment is in the past");

    LocalDate effStart = protectionFromStartOfDay ? accStart.minusDays(1) : accStart;
    LocalDate effEnd = protectionFromStartOfDay ? accEnd.minusDays(1) : accEnd;

    double[] res = new double[5];
    res[0] = effStart.isBefore(tradeDate) ?
        -curveDCC.yearFraction(effStart, tradeDate) :
        curveDCC.yearFraction(tradeDate, effStart);
    res[1] = curveDCC.yearFraction(tradeDate, effEnd);
    res[2] = curveDCC.yearFraction(tradeDate, paymentDate);
    res[3] = accrualDCC.yearFraction(accStart, accEnd);
    res[4] = res[3] / curveDCC.yearFraction(accStart, accEnd);
    return res;
  }

  /**
   * Gets the paymentTime.
   * @return the paymentTime
   */
  public double getPaymentTime() {
    return paymentTime;
  }

  /**
   * Gets the yearFrac.
   * @return the yearFrac
   */
  public double getYearFrac() {
    return yearFrac;
  }

  /**
   * Gets the effStart.
   * @return the effStart
   */
  public double getEffStart() {
    return effStart;
  }

  /**
   * Gets the effEnd.
   * @return the effEnd
   */
  public double getEffEnd() {
    return effEnd;
  }

  /**
   * Gets the ratio of the accrual period year fraction calculated using the accrual DCC to that calculated
   * using the curve DCC. This is used in accrual on default calculations.
   * 
   * @return the year fraction ratio
   */
  public double getYFRatio() {
    return ycRatio;
  }

  /**
   * Produce a coupon with payments and accrual start/end offset by a given amount.
   * For example if an offset of 0.5 was applied to a coupon with effStart, effEnd and payment
   * time of 0, 0.25 and 0.25,  the new coupon would have 0.5, 0.75, 0.75 (effStart, effEnd, payment time).
   * 
   * @param offset amount of offset (in years)
   * @return offset coupon 
   */
  public CdsCoupon withOffset(double offset) {
    return new CdsCoupon(effStart + offset, effEnd + offset, paymentTime + offset, yearFrac, ycRatio);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(effEnd);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(effStart);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(paymentTime);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(ycRatio);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(yearFrac);
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
    CdsCoupon other = (CdsCoupon) obj;
    if (Double.doubleToLongBits(effEnd) != Double.doubleToLongBits(other.effEnd)) {
      return false;
    }
    if (Double.doubleToLongBits(effStart) != Double.doubleToLongBits(other.effStart)) {
      return false;
    }
    if (Double.doubleToLongBits(paymentTime) != Double.doubleToLongBits(other.paymentTime)) {
      return false;
    }
    if (Double.doubleToLongBits(ycRatio) != Double.doubleToLongBits(other.ycRatio)) {
      return false;
    }
    if (Double.doubleToLongBits(yearFrac) != Double.doubleToLongBits(other.yearFrac)) {
      return false;
    }
    return true;
  }

}
