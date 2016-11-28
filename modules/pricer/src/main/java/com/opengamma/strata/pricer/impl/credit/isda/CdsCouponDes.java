/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A date based description of a CDS accrual period 
 */
public class CdsCouponDes {

  private static final DayCount DEFAULT_ACCURAL_DCC = DayCounts.ACT_360;

  private final LocalDate accStart;
  private final LocalDate accEnd;
  private final LocalDate paymentDate;
  private final double yearFrac;

  /**
   * Make a set of CDSCouponDes.
   * 
   * @param leg  the schedule of the accrual periods
   * @return a set of CDSCouponDes
   */
  public static CdsCouponDes[] makeCoupons(IsdaPremiumLegSchedule leg) {
    return makeCoupons(leg, DEFAULT_ACCURAL_DCC);
  }

  /**
   * Make a set of CDSCouponDes.
   * 
   * @param leg  the schedule of the accrual periods
   * @param accrualDCC  the day count used for the accrual 
   * @return a set of CDSCouponDes
   */
  public static CdsCouponDes[] makeCoupons(IsdaPremiumLegSchedule leg, DayCount accrualDCC) {
    ArgChecker.notNull(leg, "leg");
    int n = leg.getNumPayments();
    CdsCouponDes[] coupons = new CdsCouponDes[n];
    for (int i = 0; i < n; i++) {
      coupons[i] = new CdsCouponDes(leg.getAccStartDate(i), leg.getAccEndDate(i), leg.getPaymentDate(i), accrualDCC);
    }
    return coupons;
  }

  //-------------------------------------------------------------------------
  /**
   * A date based description of a CDS accrual period.
   * The day count used for the accrual is ACT/360.
   * 
   * @param accStart  the start date of the period 
   * @param accEnd  the end date of the period 
   * @param paymentDate  the payment date for the period 
   */
  public CdsCouponDes(LocalDate accStart, LocalDate accEnd, LocalDate paymentDate) {
    this(accStart, accEnd, paymentDate, DEFAULT_ACCURAL_DCC);
  }

  /**
   * A date based description of a CDS accrual period.
   * 
   * @param accStart  the start date of the period 
   * @param accEnd  the end date of the period 
   * @param paymentDate  the payment date for the period 
   * @param accrualDCC  the day count used for the accrual 
   */
  public CdsCouponDes(LocalDate accStart, LocalDate accEnd, LocalDate paymentDate, DayCount accrualDCC) {
    ArgChecker.notNull(accStart, "accStart");
    ArgChecker.notNull(accEnd, "accEnd");
    ArgChecker.notNull(paymentDate, "paymentDate");
    ArgChecker.isTrue(accEnd.isAfter(accStart), "accEnd must be after accStart");
    ArgChecker.notNull(accrualDCC, "accrualDCC");
    this.accStart = accStart;
    this.accEnd = accEnd;
    this.paymentDate = paymentDate;
    this.yearFrac = accrualDCC.yearFraction(accStart, accEnd);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accStart.
   * @return the accStart
   */
  public LocalDate getAccStart() {
    return accStart;
  }

  /**
   * Gets the accEnd.
   * @return the accEnd
   */
  public LocalDate getAccEnd() {
    return accEnd;
  }

  /**
   * Gets the paymentDate.
   * @return the paymentDate
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  /**
   * Gets the yearFrac.
   * @return the yearFrac
   */
  public double getYearFrac() {
    return yearFrac;
  }

}
