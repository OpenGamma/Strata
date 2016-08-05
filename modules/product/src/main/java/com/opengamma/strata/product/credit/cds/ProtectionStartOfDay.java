package com.opengamma.strata.product.credit.cds;

public enum ProtectionStartOfDay {

  BEGINNING,

  NONE;

  public boolean isBeginning() {
    return this == BEGINNING;
  }

}
