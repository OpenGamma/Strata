/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

/**
 * Enumerates the types of PV that can be returned. 
 * <p>
 * The price is usually clean or dirty.
 */
public enum PriceType {

  /**
   * Clean price
   */
  CLEAN,
  /**
   * Dirty price
   */
  DIRTY;

  //-------------------------------------------------------------------------
  /**
   * Check if the price type is 'Clean'.
   * 
   * @return true if clean, false if dirty
   */
  public boolean isCleanPrice() {
    return this == CLEAN;
  }

}
