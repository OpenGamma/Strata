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
 * A convention defining how to handle a negative interest rate.
 * <p>
 * When calculating a floating rate, the result may be negative.
 * This convention defines whether to allow the negative value or round to zero.
 */
public enum NegativeRateMethod {

  /**
   * The "Negative Interest Rate Method", that allows the rate to be negative.
   * <p>
   * When calculating a payment, negative rates are allowed and result in a payment
   * in the opposite direction to that normally expected.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4b and 6.4c.
   */
  ALLOW_NEGATIVE {
    @Override
    public double adjust(double rate) {
      return rate;
    }
  },
  /**
   * The "Zero Rate Method", that prevents the rate from going below zero.
   * <p>
   * When calculating a payment, or other amount during compounding, the rate is
   * not allowed to go below zero.
   * <p>
   * Defined by the 2006 ISDA definitions article 6.4d and 6.4e.
   */
  NOT_NEGATIVE {
    @Override
    public double adjust(double rate) {
      return Math.max(rate, 0);
    }
  };

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static NegativeRateMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-----------------------------------------------------------------------
  /**
   * Adjusts the specified rate according to the rate method rule.
   * <p>
   * Given a rate, the result will be either the same rate, or zero,
   * depending on the enum constant the method is called on.
   * 
   * @param rate  the rate to adjust
   * @return the adjusted result
   */
  public abstract double adjust(double rate);

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
