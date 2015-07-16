/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * Points up-front (PUF) is the current (as of April 2009) way of quoting CDSs.
 * A CDS has a standardised coupon (premium) - which is either 100 or 500 bps in North America
 * (depending on the credit quality of the reference entity). An up front fee is then payable
 * by the buyer of protection (i.e. the payer of the premiums) - this fee can be negative
 * (i.e. an amount is received by the protection buyer). PUF is quoted as a percentage of the notional.
 * <p>
 * A zero hazard curve (or equivalent, e.g. the survival probability curve) can be implied from
 * a set of PUF quotes (on the same name at different maturities) by finding the curve that gives
 * all the CDSs a clean present value equal to their PUF*Notional  (the curve is not unique
 * and will depend on other modeling choices). 
 */
public class PointsUpFront implements CdsQuoteConvention {

  private final double _coupon;
  private final double _puf;

  public PointsUpFront(double coupon, double puf) {
    _coupon = coupon;
    _puf = puf;
  }

  @Override
  public double getCoupon() {
    return _coupon;
  }

  public double getPointsUpFront() {
    return _puf;
  }

}
