package com.opengamma.strata.finance.credit.protection;

/**
 * http://www.fpml.org/coding-scheme/restructuring
 * <p>
 * Specifies the form of the restructuring credit event that is applicable to the credit default swap.
 */
public enum RestructuringClause {

  /**
   * Restructuring (Section 4.7) and Modified Restructuring Maturity Limitation and
   * Conditionally Transferable Obligation
   * (2014 Definitions: Section 3.31, 2003 Definitions: 2.32) apply.
   * Mod-Mod Restructuring
   */

  ModModR("MM"),

  /**
   * Restructuring (Section 4.7) and Restructuring Maturity Limitation and Fully
   * Transferable Obligation (2014 Definitions: Section 3.31, 2003 Definitions: 2.32) apply.
   * Modified Restructuring
   */

  ModR("MR"),

  /**
   * Restructuring as defined in the applicable ISDA Credit Derivatives Definitions. (2003 or 2014).
   * Cum-Restructuring or Old Restructuring
   */

  R("CR"),

  /**
   * Ex-Restructuring
   */

  XR("XR");

  private String markitNotation;

  RestructuringClause(String markitNotation) {
    this.markitNotation = markitNotation;
  }

  public String getMarkitNotation() {
    return markitNotation;
  }
}
