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
  ORIGINAL_ISDA,
  /**
   * The correction proposed by Markit (v 1.8.2).
   */
  MARKIT_FIX,
  /**
   * The mathematically correct formula .
   */
  CORRECT;

}
