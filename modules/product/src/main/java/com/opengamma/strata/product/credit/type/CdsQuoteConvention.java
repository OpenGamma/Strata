/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

/**
 * Market quote conventions for credit default swaps.
 */
public enum CdsQuoteConvention {

  /**
   * Par spread.
   * <p>
   * Par spread is the old (i.e. pre-April 2009) way of quoting CDSs. 
   * A CDS would be constructed to have an initial fair value of zero; 
   * the par-spread is the value of the coupon (premium) on the premium leg that makes this so. 
   * <p>
   * A zero hazard curve (or equivalent, e.g. the survival probability curve) can be implied from a set of par spread quotes 
   * (on the same name at different maturities) by finding the curve that gives all the CDSs a PV of zero  
   * (the curve is not unique and will depend on other modeling choices). 
   */
  PAR_SPREAD,

  /**
   * Points upfront.
   * <p>
   * Points upfront (PUF) is the current (as of April 2009) way of quoting CDSs. A CDS has a fixed coupon (premium). 
   * <p>
   * An up front fee is payable by the buyer of protection (i.e. the payer of the premiums) - this fee can be negative 
   * (i.e. an amount is received by the protection buyer). PUF is quoted as a percentage of the notional. 
   * <p>
   * A zero hazard curve (or equivalent, e.g. the survival probability curve) can be implied from a set of PUF quotes
   * (on the same name at different maturities) by finding the curve that gives all the CDSs a clean present value 
   * equal to their {@code PUF * notional}  (the curve is not unique and will depend on other modeling choices). 
   */
  POINTS_UPFRONT,

  /**
   * Quoted spread.
   * <p>
   * Quoted spread (sometimes misleadingly called flat spread) is an alternative to quoting PUF 
   * where people wish to see a spread like number. 
   * It is numerically close in value to the equivalent par spread but is not exactly the same.
   * <p>
   * To find the quoted spread of a CDS from its PUF (and premium) one first finds the unique flat hazard rate 
   * that will give the CDS a clean present value equal to its {@code PUF * notional}; one then finds 
   * the par spread (the coupon that makes the CDS have zero clean PV) of the CDS from this flat hazard curve - 
   * this is the quoted spread (and the reason for the confusing name, flat spread).
   * To go from a quoted spread to PUF, one does the reverse of the above.
   * <p>
   * A zero hazard curve (or equivalent, e.g. the survival probability curve) cannot be directly implied from 
   * a set of quoted spreads - one must first convert to PUF.
   */
  QUOTED_SPREAD;

}
