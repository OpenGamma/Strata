package com.opengamma.strata.product.credit.cds;

public enum PaymentOnDefault {

  ACCRUED_INTEREST,

  NONE;

  public boolean isAccruedInterest() {
    return this == ACCRUED_INTEREST;
  }

}
