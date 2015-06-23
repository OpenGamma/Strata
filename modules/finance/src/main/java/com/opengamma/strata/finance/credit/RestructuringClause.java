/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

/**
 * Specifies the form of the restructuring credit event that is applicable to the credit default swap.
 * Also called DocClause
 */
public enum RestructuringClause {
  // TODO: constants should be UPPER_CASE
  // TODO: constants all need Javadoc

  /**
   * Restructuring (Section 4.7) and Modified Restructuring Maturity Limitation and
   * Conditionally Transferable Obligation
   * (2014 Definitions: Section 3.31, 2003 Definitions: 2.32) apply.
   * Mod-Mod Restructuring
   */

  ModModRestructuring2003,
  ModModRestructuring2014,

  /**
   * Restructuring (Section 4.7) and Restructuring Maturity Limitation and Fully
   * Transferable Obligation (2014 Definitions: Section 3.31, 2003 Definitions: 2.32) apply.
   * Modified Restructuring
   */

  ModifiedRestructuring2003,
  ModifiedRestructuring2014,

  /**
   * Restructuring as defined in the applicable ISDA Credit Derivatives Definitions. (2003 or 2014).
   * Cum-Restructuring or Old Restructuring
   */

  CumRestructuring2003,
  CumRestructuring2014,

  /**
   * No restructuring. (2003 or 2014).
   * Ex-Restructuring
   */

  NoRestructuring2003,
  NoRestructuring2014

}
