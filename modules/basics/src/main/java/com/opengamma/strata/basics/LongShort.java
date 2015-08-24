/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Flag indicating whether a trade is "long" or "short".
 * <p>
 * A long position is one where a financial instrument is bought with the expectation
 * that its value will rise. A short position is the opposite where the expectation
 * is that its value will fall, usually applied to the sale of a borrowed asset.
 */
public enum LongShort {

  /**
   * Long.
   */
  LONG, /**
         * Short.
         */
  SHORT;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static LongShort of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Converts a boolean "is long" flag to the enum value.
   * 
   * @param isLong  the long flag, true for long, false for short
   * @return the equivalent enum
   */
  public static LongShort ofLong(boolean isLong) {
    return isLong ? LONG : SHORT;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Long'.
   * 
   * @return true if long, false if short
   */
  public boolean isLong() {
    return this == LONG;
  }

  /**
   * Checks if the type is 'Short'.
   * 
   * @return true if short, false if long
   */
  public boolean isShort() {
    return this == SHORT;
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
