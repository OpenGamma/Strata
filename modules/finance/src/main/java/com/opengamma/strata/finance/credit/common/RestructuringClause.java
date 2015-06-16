/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.common;

/**
 * http://www.fpml.org/coding-scheme/restructuring
 * <p>
 * Specifies the form of the restructuring credit event that is applicable to the credit default swap.
 * Also called DocClause
 */
public enum RestructuringClause {

  /**
   * Restructuring (Section 4.7) and Modified Restructuring Maturity Limitation and
   * Conditionally Transferable Obligation
   * (2014 Definitions: Section 3.31, 2003 Definitions: 2.32) apply.
   * Mod-Mod Restructuring
   */

  ModModR("MM"),
  ModModR14("MM14"),

  /**
   * Restructuring (Section 4.7) and Restructuring Maturity Limitation and Fully
   * Transferable Obligation (2014 Definitions: Section 3.31, 2003 Definitions: 2.32) apply.
   * Modified Restructuring
   */

  ModR("MR"),
  ModR14("MR14"),

  /**
   * Restructuring as defined in the applicable ISDA Credit Derivatives Definitions. (2003 or 2014).
   * Cum-Restructuring or Old Restructuring
   */

  R("CR"),
  R14("CR14"),

  /**
   * No restructuring. (2003 or 2014).
   * Ex-Restructuring
   */

  XR("XR"),
  XR14("XR14");

  private String markitNotation;

  RestructuringClause(String markitNotation) {
    this.markitNotation = markitNotation;
  }

  /**
   * @return common Markit/RED notation
   */
  public String getMarkitNotation() {
    return markitNotation;
  }
}
