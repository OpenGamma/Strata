/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A compounded rate type.
 * <p>
 * Compounded rate is continuously compounded rate or periodically compounded rate.
 * The main application of this is z-spread computation under a specific way of compounding.
 * See, for example, {@link DiscountFactors}.
 */
public enum CompoundedRateType {

  /**
   * Periodic compounding.
   * <p>
   * The rate is periodically compounded.
   * In this case the number of periods par year should be specified in addition.
   */
  PERIODIC,
  /**
   * Continuous compounding.
   * <p>
   * The rate is continuously compounded.
   */
  CONTINUOUS;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CompoundedRateType of(String uniqueName) {
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
