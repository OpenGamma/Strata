/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * Quoted spread (sometimes misleadingly called flat spread) is an alternative to quoting PUF
 * where people wish to see a spread like number. It is numerical close in value to the equivalent
 * par spread but is <b>absolutely not the same thing</b>.
 * <p>
 * To find the quoted spread of a CDS from its PUF (and premium) one first finds the unique flat
 * hazard rate that will give the CDS a clean present value equal to its PUF*Notional;
 * one then finds the par spread (the coupon that makes the CDS have zero clean PV) of the CDS
 * from this <b>flat hazard</b> curve - this is the quoted spread (and the reason for the confusing
 * name, flat spread).
 * <p>
 * To go from a quoted spread to PUF, one does the reverse of the above.
<p>
 * A zero hazard curve (or equivalent, e.g. the survival probability curve) cannot be directly
 * implied from a set of quoted spreads - one must first convert to PUF.
 */
public class CdsQuotedSpread implements CdsQuoteConvention {

  private final double coupon;
  private final double quotedSpread;

  public CdsQuotedSpread(double coupon, double quotedSpread) {
    this.coupon = coupon;
    this.quotedSpread = quotedSpread;
  }

  @Override
  public double getCoupon() {
    return coupon;
  }

  public double getQuotedSpread() {
    return quotedSpread;
  }

}
