/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * Which of the three accrual on default formulae to use.
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
