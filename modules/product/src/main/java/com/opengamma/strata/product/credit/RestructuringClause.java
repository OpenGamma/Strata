/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

/**
 * Specifies the form of the restructuring credit event that is applicable to the credit default swap.
 * Also called DocClause
 */
public enum RestructuringClause {

  /**
   * Restructuring (Section 4.7) and Modified Restructuring Maturity Limitation and
   * Conditionally Transferable Obligation (2003 Definitions: 2.32) apply.
   * Mod-Mod Restructuring
   */
  MOD_MOD_RESTRUCTURING_2003,
  /**
   * Restructuring (Section 4.7) and Modified Restructuring Maturity Limitation and
   * Conditionally Transferable Obligation (2014 Definitions: Section 3.31) apply.
   * Mod-Mod Restructuring
   */
  MOD_MOD_RESTRUCTURING_2014,
  /**
   * Restructuring (Section 4.7) and Restructuring Maturity Limitation and Fully
   * Transferable Obligation (2003 Definitions: 2.32) apply.
   * Modified Restructuring
   */
  MODIFIED_RESTRUCTURING_2003,
  /**
   * Restructuring (Section 4.7) and Restructuring Maturity Limitation and Fully
   * Transferable Obligation (2014 Definitions: Section 3.31) apply.
   * Modified Restructuring
   */
  MODIFIED_RESTRUCTURING_2014,
  /**
   * Restructuring as defined in the applicable ISDA Credit Derivatives Definitions. (2003).
   * Cum-Restructuring or Old Restructuring
   */
  CUM_RESTRUCTURING_2003,
  /**
   * Restructuring as defined in the applicable ISDA Credit Derivatives Definitions. (2014).
   * Cum-Restructuring or Old Restructuring
   */
  CUM_RESTRUCTURING_2014,
  /**
   * No restructuring. (2003).
   * Ex-Restructuring
   */
  NO_RESTRUCTURING_2003,
  /**
   * No restructuring. (2014).
   * Ex-Restructuring
   */
  NO_RESTRUCTURING_2014

}
