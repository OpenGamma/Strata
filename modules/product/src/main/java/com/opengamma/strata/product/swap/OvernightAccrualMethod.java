/*
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
 * The method of accruing interest based on an Overnight index.
 * <p>
 * Two methods of accrual are supported - compounded and averaged.
 * Averaging is primarily related to the 'USD-FED-FUND' index.
 */
public enum OvernightAccrualMethod {

  /**
   * The compounded method.
   * <p>
   * Interest is accrued by simple compounding of each rate published during the accrual period.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.2a(3C).
   */
  COMPOUNDED,
  /**
   * The averaged method.
   * <p>
   * Interest is accrued by taking the average of all the rates published on the
   * index during the accrual period.
   */
  AVERAGED;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static OvernightAccrualMethod of(String uniqueName) {
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
