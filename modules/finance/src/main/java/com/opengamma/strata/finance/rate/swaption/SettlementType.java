/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swaption;

/**
 * Settlement types for {@code Swaption}. 
 */
public enum SettlementType {

  /**
   * Cash settlement
   * <p>
   * Cash amount is paid (by the short party to the long party) at the exercise date (or more exactly 
   * at the spot lag after the exercise) and the actual swap is not entered into.
   */
  CASH,
  /**
   * Physical delivery. 
   * <p>
   * The two parties enter into actual interest rate swap (the underlying swap) at the expiry date of the option. 
   */
  PHYSICAL,

}
