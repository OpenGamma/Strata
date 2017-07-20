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
public enum ArbitrageHandling implements NamedEnum {

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

  // helper for name conversions
  private static final EnumNames<ArbitrageHandling> NAMES = EnumNames.of(ArbitrageHandling.class);

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
  public static ArbitrageHandling of(String name) {
    return NAMES.parse(name);
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
    return NAMES.format(this);
  }

}
