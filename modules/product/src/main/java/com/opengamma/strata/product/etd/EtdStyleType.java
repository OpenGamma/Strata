/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The type of an Exchange Traded Derivative (ETD) product, either a future or an option.
 */
public enum EtdStyleType {

  /**
   * Standard contract, where the ETD expires monthly.
   */
  MONTHLY,
  /**
   * Standard contract, where the ETD expires weekly.
   */
  WEEKLY,
  /**
   * Standard contract, where the ETD expires on a specific day-of-month.
   */
  DAILY,
  /**
   * Flexible contract, where the ETD expires on a specific day-of-month
   * and the settlement type and option type can be controlled.
   */
  FLEX;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static EtdStyleType of(String uniqueName) {
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
