/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Analytic CDS pricer with cash flows truncated.  
 * <p>
 * For example, a cash flow amount of 2,555,555.5555556 is truncated to  2,555,555.0. 
 */
public class AnalyticTruncatedCdsPricer {

  private static final AnalyticCdsPricer BASE_PRICER = new AnalyticCdsPricer(); // acc formula not used
  
  private static final double EPS = 1.0e-13;

  /**
   * Computes dirty present value of CDS. 
   * 
   * @param cds  the CDS to price
   * @param yieldCurve  the yield curve
   * @param creditCurve  the credit curve
   * @param fractionalSpread  the coupon 
   * @param notional  the notional, must be positive
   * @return the present value
   */
  public double dirtyPvRounded(
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double fractionalSpread,
      double notional) {

    ArgChecker.notNull(cds, "cds");
    if (notional == 0d || cds.getProtectionEnd() <= 0.0) { //short cut already expired CDSs
      return 0d;
    }
    ArgChecker.isTrue(notional > 0d, "notional must be positive");
    double dirtyPremLegRounded = dirtyPremiumLegRounded(cds, yieldCurve, creditCurve, fractionalSpread, notional);
    double proLeg = BASE_PRICER.protectionLeg(cds, yieldCurve, creditCurve);
    return proLeg * notional - dirtyPremLegRounded;
  }

  /**
   * Computes dirty present of CDS value with par spread. 
   * 
   * @param cds  the CDS to price
   * @param yieldCurve  the yield curve
   * @param creditCurve  the credit curve
   * @param fractionalSpread  the coupon
   * @param parSpread  the par spread
   * @param notional  the notional
   * @return the present value
   */
  public double dirtyPvRounded(
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double fractionalSpread,
      double parSpread,
      double notional) {

    ArgChecker.notNull(cds, "cds");
    if (notional == 0d || cds.getProtectionEnd() <= 0.0) { //short cut already expired CDSs
      return 0d;
    }
    ArgChecker.isTrue(notional > 0d, "notional must be positive");
    double dirtyPremLegRounded = dirtyPremiumLegRounded(cds, yieldCurve, creditCurve, fractionalSpread, notional);
    return (parSpread - fractionalSpread) * dirtyPremLegRounded / fractionalSpread;
  }

  /**
   * Computes dirty present value of premium leg.
   * 
   * @param cds  the CDS to price
   * @param yieldCurve  the yield curve
   * @param creditCurve  the credit curve
   * @param fractionalSpread  the coupon
   * @param notional  the notional
   * @return the present value
   */
  public double dirtyPremiumLegRounded(
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double fractionalSpread,
      double notional) {
    ArgChecker.notNull(cds, "null cds");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.notNull(creditCurve, "null creditCurve");
    if (notional == 0d || cds.getProtectionEnd() <= 0.0) { //short cut already expired CDSs
      return 0d;
    }
    ArgChecker.isTrue(notional > 0d, "notional should be positive");
    ArgChecker.isTrue(cds.isPayAccOnDefault(), "payAccOnDefault should be true");

    double pv = 0d;
    double accPv = 0d;
    for (CdsCoupon coupon : cds.getCoupons()) {
      double cf = coupon.getYearFrac() * fractionalSpread * notional;
      double cfFloor = Math.floor(cf);
      double cfCeil = Math.ceil(cf);
      double cfTrc = Math.abs(cfCeil - cf) < EPS * notional ? cfCeil : cfFloor;
      double q1 = creditCurve.getDiscountFactor(coupon.getEffStart());
      double q2 = creditCurve.getDiscountFactor(coupon.getEffEnd());
      double p = yieldCurve.getDiscountFactor(coupon.getPaymentTime());
      pv += cfTrc * p * q2;
      accPv += 0.5 * cfTrc * p * (q1 - q2);
    }
    return pv + accPv;
  }

}
