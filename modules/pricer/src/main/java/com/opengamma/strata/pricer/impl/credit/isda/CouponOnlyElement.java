/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * 
 */
public class CouponOnlyElement {

  private final double riskLessValue;
  private final double effEnd;
  private final int creditCurveKnot;

  public CouponOnlyElement(CdsCoupon coupon, IsdaCompliantYieldCurve yieldCurve, int creditCurveKnot) {
    this.riskLessValue = coupon.getYearFrac() * yieldCurve.getDiscountFactor(coupon.getPaymentTime());
    this.effEnd = coupon.getEffEnd();
    this.creditCurveKnot = creditCurveKnot;
  }

  //-------------------------------------------------------------------------
  public double pv(IsdaCompliantCreditCurve creditCurve) {
    return riskLessValue * creditCurve.getDiscountFactor(effEnd);
  }

  public double[] pvAndSense(IsdaCompliantCreditCurve creditCurve) {
    double pv = riskLessValue * creditCurve.getDiscountFactor(effEnd);
    double pvSense = -pv * creditCurve.getSingleNodeRTSensitivity(effEnd, creditCurveKnot);
    return new double[] {pv, pvSense};
  }

  //-------------------------------------------------------------------------
  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + creditCurveKnot;
    long temp;
    temp = Double.doubleToLongBits(effEnd);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(riskLessValue);
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
    CouponOnlyElement other = (CouponOnlyElement) obj;
    if (creditCurveKnot != other.creditCurveKnot) {
      return false;
    }
    if (Double.doubleToLongBits(effEnd) != Double.doubleToLongBits(other.effEnd)) {
      return false;
    }
    if (Double.doubleToLongBits(riskLessValue) != Double.doubleToLongBits(other.riskLessValue)) {
      return false;
    }
    return true;
  }

}
