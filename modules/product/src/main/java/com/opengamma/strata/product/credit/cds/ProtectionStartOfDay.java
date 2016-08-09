/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.cds;

/**
 * The protection start of the day.
 * <p>
 * When the protection starts on the start date.
 */
public enum ProtectionStartOfDay {

  /**
   * Beginning of the start day. 
   * <p>
   * The protection starts at the beginning of the day. 
   */
  BEGINNING,

  /**
   * None.
   * <p>
   * The protection start is not specified. 
   * The CDS is priced based on the default date logic.
   */
  NONE;

  //-------------------------------------------------------------------------
  /**
   * Check if the type is 'Beginning'.
   * 
   * @return true if beginning, false otherwise
   */
  public boolean isBeginning() {
    return this == BEGINNING;
  }

}
