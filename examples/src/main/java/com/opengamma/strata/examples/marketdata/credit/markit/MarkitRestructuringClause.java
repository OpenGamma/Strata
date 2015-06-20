/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.credit.markit;

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


  public RestructuringClause translate() {
    switch (this) {
      case MM:
        return RestructuringClause.ModModRestructuring2003;
      case MM14:
        return RestructuringClause.ModModRestructuring2014;
      case MR:
        return RestructuringClause.ModifiedRestructuring2003;
      case MR14:
        return RestructuringClause.ModifiedRestructuring2014;
      case CR:
        return RestructuringClause.CumRestructuring2003;
      case CR14:
        return RestructuringClause.CumRestructuring2014;
      case XR:
        return RestructuringClause.NoRestructuring2003;
      case XR14:
        return RestructuringClause.NoRestructuring2014;
      default:
        throw new IllegalStateException("Unmapped restructuring clause. Do not have mapping for " + this);

    }
  }

  /**
   * Map from Strata enum to
   */
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
