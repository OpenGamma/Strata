/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The types of curve node date.
 * <p>
 * This is used to identify how the date of a node should be calculated.
 */
public enum CurveNodeDateType {

  /**
   * Defines a fixed date that is externally provided.
   */
  FIXED,
  /**
   * Defines the end date of the trade.
   * This will typically be the last accrual date, but may be any suitable
   * date at the end of the trade.
   */
  END,
  /**
   * Defines the last fixing date referenced in the trade.
   * Used only for instruments referencing an Ibor index.
   */
  LAST_FIXING;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CurveNodeDateType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
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
