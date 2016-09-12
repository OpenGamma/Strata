/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The type of a swap leg.
 * <p>
 * This provides a high-level categorization of a swap leg.
 * This is useful when it is necessary to find a specific leg.
 */
public enum SwapLegType {

  /**
   * A fixed rate swap leg.
   * All periods in this leg must have a fixed rate.
   */
  FIXED,
  /**
   * A floating rate swap leg based on an Ibor index.
   * <p>
   * This kind of leg may include some fixed periods, such as in a stub or
   * where the first rate is specified in the contract.
   */
  IBOR,
  /**
   * A floating rate swap leg based on an Overnight index.
   * <p>
   * This kind of leg may include some fixed periods, such as in a stub or
   * where the first rate is specified in the contract.
   */
  OVERNIGHT,
  /**
   * A floating rate swap leg based on an price index.
   * <p>
   * This kind of leg may include some reference dates 
   * where the index rate is specified.
   */
  INFLATION,
  /**
   * A swap leg that is not based on a Fixed, Ibor, Overnight or Inflation rate.
   */
  OTHER;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SwapLegType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Fixed'.
   * 
   * @return true if fixed, false otherwise
   */
  public boolean isFixed() {
    return this == FIXED;
  }

  /**
   * Checks if the type is floating, defined as 'Ibor', 'Overnight' or 'Inflation'.
   * 
   * @return true if floating, false otherwise
   */
  public boolean isFloat() {
    return this == IBOR || this == OVERNIGHT || this == INFLATION;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
