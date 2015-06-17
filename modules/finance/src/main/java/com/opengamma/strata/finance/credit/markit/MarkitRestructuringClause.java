/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.markit;

import com.opengamma.strata.finance.credit.RestructuringClause;

/**
 * Specifies the form of the restructuring credit event that is applicable to the credit default swap.
 * Also called DocClause
 */
public enum MarkitRestructuringClause {

  /**
   * Mod-Mod Restructuring
   */

  MM,
  MM14,

  /**
   * Modified Restructuring
   */

  MR,
  MR14,

  /**
   * Cum-Restructuring or Old Restructuring
   */

  CR,
  CR14,

  /**
   * No restructuring. (2003 or 2014).
   * Ex-Restructuring
   */

  XR,
  XR14;


  public static MarkitRestructuringClause from(RestructuringClause restructuringClause) {
    switch (restructuringClause) {
      case ModModRestructuring2003:
        return MM;
      case ModModRestructuring2014:
        return MM14;
      case ModifiedRestructuring2003:
        return MR;
      case ModifiedRestructuring2014:
        return MR14;
      case CumRestructuring2003:
        return CR;
      case CumRestructuring2014:
        return CR14;
      case NoRestructuring2003:
        return XR;
      case NoRestructuring2014:
        return XR14;
      default:
        throw new UnsupportedOperationException("Unknown restructuring clause. Do not have mapping for " + restructuringClause);

    }
  }
}
