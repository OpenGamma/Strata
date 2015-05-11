/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.time.YearMonth;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.basics.index.PriceIndex;

/**
 * Curve providing forward {@link PriceIndex} estimates.
 * <p>
 * Mainly used to price inflation related products.
 */
public interface PriceIndexCurve 
    extends ImmutableBean {
  
  /**
   * Returns the curve's name.
   * @return the name.
   */
  String getName();

  /**
   * Returns the estimated price index value for the given month.
   * @param month  the month
   * @return the price index value
   */
  double getPriceIndex(YearMonth month);

  /**
   * Returns the sensitivities of the price index to the curve parameters at a given month.
   * 
   * @param time The time
   * @return The sensitivities. If the time is less than 1e<sup>-6</sup>, the rate is
   * ill-defined and zero is returned.
   */
  Double[] getPriceIndexParameterSensitivity(YearMonth month);
  // TODO: should this be a double[], Double[] or something else.
  
  /**
   * Returns the number of parameters defining the curve.
   * @return The number of parameters
   */
  int getNumberOfParameters();  
  
  /**
   * Returns a new curve for which each of the parameters has been shift according to a vector of shifts.
   * @param shifts  the parameters shifts
   * @return the new curve
   */
  PriceIndexCurve shiftCurve(double[] shifts);
  
}
