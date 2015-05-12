/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.time.YearMonth;

import com.opengamma.strata.basics.index.PriceIndex;

/**
 * A curve providing forward estimates for a price index.
 * <p>
 * This provides a forward curve for a single {@link PriceIndex}.
 * Mainly used to price inflation related products.
 */
public interface PriceIndexCurve {

  /**
   * Gets the name of the curve.
   * 
   * @return the curve name
   */
  public abstract String getName();

  /**
   * Gets the price index value for the given month.
   * <p>
   * Values of the price index in the future are estimates.
   * 
   * @param month  the month to query the index for
   * @return the price index value
   */
  public abstract double getPriceIndex(YearMonth month);

  /**
   * Returns the sensitivities of the price index to the curve parameters at a given month.
   * 
   * @param month  the month to query the sensitivity for
   * @return the sensitivity array, if the time is less than 1e<sup>-6</sup>, the rate is
   *   ill-defined and zero is returned.
   */
  public abstract Double[] getPriceIndexParameterSensitivity(YearMonth month);
  // TODO: should this be a double[], Double[] or something else.

  /**
   * Gets the number of parameters defining the curve.
   * 
   * @return the number of parameters
   */
  public abstract int getNumberOfParameters();

  /**
   * Returns a new curve for which each of the parameters has been shifted according to a vector of shifts.
   * 
   * @param shifts  the parameters shifts
   * @return the new curve
   */
  public abstract PriceIndexCurve shiftedBy(double[] shifts);

}
