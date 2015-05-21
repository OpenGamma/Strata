package com.opengamma.strata.finance.credit.general;

/**
 * The purpose of this element is to disambiguate whether the buyer of the product effectively buys
 * protection or whether he buys risk (and, hence, sells protection) in the case, such as high yields
 * instruments, where no firm standard appears to exist at the execution level.
 */
public enum BuyerConvention {
  PROTECTION,
  RISK
}
