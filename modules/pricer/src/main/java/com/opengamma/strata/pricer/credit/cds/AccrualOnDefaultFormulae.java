package com.opengamma.strata.pricer.credit.cds;

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
