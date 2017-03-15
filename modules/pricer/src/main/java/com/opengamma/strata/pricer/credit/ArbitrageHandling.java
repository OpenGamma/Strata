/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The formula for accrual on default.
 * <p>
 * This specifies which formula is used in {@code IsdaCdsProductPricer} for computing the accrued payment on default. 
 * The formula is 'original ISDA', 'Markit fix' or 'correct'.
 */
public enum ArbitrageHandling {

  /**
   * Ignore.
   * <p>
   * If the market data has arbitrage, the curve will still build. 
   * The survival probability will not be monotonically decreasing 
   * (equivalently, some forward hazard rates will be negative). 
   */
  IGNORE,
  /**
   * Fail.
   * <p>
   * An exception is thrown if an arbitrage is found. 
   */
  FAIL,
  /**
   * Zero hazard rate.
   * <p>
   * If a particular spread implies a negative forward hazard rate, 
   * the hazard rate is set to zero, and the calibration continues. 
   * The resultant curve will not exactly reprice the input CDSs, but will find new spreads that just avoid arbitrage.   
   */
  ZERO_HAZARD_RATE;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static ArbitrageHandling of(String uniqueName) {
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
