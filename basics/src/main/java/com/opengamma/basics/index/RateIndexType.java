/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * The type of a rate index.
 * <p>
 * A rate index represents an agreed opinion on the rate for a certain type of deposit.
 */
public enum RateIndexType {

  /**
   * An index representing the interest rate for an overnight deposit.
   * <p>
   * This type includes indices that are strictly defined as "Tomorrow/Next" rather than "Overnight".
   */
  OVERNIGHT,
  /**
   * An index representing the interest rate for a deposit based on a tenor.
   * <p>
   * In many cases, there is a lag of 2 business days between the fixing date and the settlement date.
   * The tenor of the deposit starts on the settlement date.
   */
  TENOR;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static RateIndexType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

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
