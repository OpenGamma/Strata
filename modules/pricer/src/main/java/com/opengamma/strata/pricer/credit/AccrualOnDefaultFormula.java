/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The formula for accrual on default.
 * <p>
 * This specifies which formula is used in {@code IsdaCdsProductPricer} for computing the accrued payment on default. 
 * The formula is 'original ISDA', 'Markit fix' or 'correct'.
 */
public enum AccrualOnDefaultFormula implements NamedEnum {

  /**
   * The formula in v1.8.1 and below.
   */
  ORIGINAL_ISDA("OriginalISDA"),

  /**
   * The correction proposed by Markit (v 1.8.2).
   */
  MARKIT_FIX("MarkitFix"),

  /**
   * The mathematically correct formula.
   */
  CORRECT("Correct");

  // helper for name conversions
  private static final EnumNames<AccrualOnDefaultFormula> NAMES = EnumNames.ofManualToString(AccrualOnDefaultFormula.class);

  // name
  private final String name;

  // create
  private AccrualOnDefaultFormula(String name) {
    this.name = name;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static AccrualOnDefaultFormula of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the omega value. 
   * <p>
   * The omega value is used in {@link IsdaCdsProductPricer}.
   * 
   * @return the omega value
   */
  public double getOmega() {
    if (this == ORIGINAL_ISDA) {
      return 1d / 730d;
    } else {
      return 0d;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return name;
  }

}
