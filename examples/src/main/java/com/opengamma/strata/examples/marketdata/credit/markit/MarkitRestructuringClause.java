/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.credit.markit;

import com.opengamma.strata.finance.credit.RestructuringClause;

/**
 * Specifies the form of the restructuring credit event that is applicable to the credit default swap.
 * <p>
 * This is also known as the <i>DocClause</i>.
 */
public enum MarkitRestructuringClause {

  /**
   * Modified-Modified Restructuring 2003.
   */
  MM,
  /**
   * Modified-Modified Restructuring 2014.
   */
  MM14,
  /**
   * Modified Restructuring 2003.
   */
  MR,
  /**
   * Modified Restructuring 2014.
   */
  MR14,
  /**
   * Cum/Old/Full Restructuring 2003.
   */
  CR,
  /**
   * Cum/Old/Full Restructuring 2014.
   */
  CR14,
  /**
   * Ex/No restructuring 2003.
   */
  XR,
  /**
   * Ex/No restructuring 2014.
   */
  XR14;

  //-------------------------------------------------------------------------
  /**
   * Converts Markit code to standard restructuring clause.
   * 
   * @return the converted clause
   */
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
   * Converts restructuring clause to Markit equivalent.
   * 
   * @param restructuringClause  the clause to convert
   * @return the converted clause
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
