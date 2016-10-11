/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

/**
 * The formula for accrual on default.
 * <p>
 * This specifies which formula is used in {@code IsdaCdsProductPricer} for computing the accrued payment on default. 
 * The formula is 'original ISDA', 'Markit fix' or 'correct'.
 */
public enum AccrualOnDefaultFormulae {

  /**
   * The formula in v1.8.1 and below.
   */
  OriginalISDA,

  /**
   * The correction proposed by Markit (v 1.8.2).
   */
  MarkitFix,

  /**
   * The mathematically correct formula .
   */
  Correct;

  //-------------------------------------------------------------------------
  /**
   * Gets the omega value. 
   * <p>
   * The omega value is used in {@link IsdaCdsProductPricer}.
   * 
   * @return the omega value
   */
  public double getOmega() {
    if (this == OriginalISDA) {
      return 1d / 730d;
    } else {
      return 0d;
    }
  }
}
