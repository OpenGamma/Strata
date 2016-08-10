package com.opengamma.strata.pricer.credit.cds;

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
  public boolean isCleanPrice() {
    return this == CLEAN;
  }
}
