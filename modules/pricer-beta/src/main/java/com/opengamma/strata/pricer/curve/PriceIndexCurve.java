/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.time.YearMonth;
import java.util.List;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.value.ValueAdjustment;

/**
 * Provides access to price related to a price index.
 * <p>
 * This provides historic and forward price for a single {@link PriceIndex}, such as 'US_CPI_U'.
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
   * @return the sensitivity array
   */
  public abstract double[] getPriceIndexParameterSensitivity(YearMonth month);

  /**
   * Gets the number of parameters defining the curve.
   * <p>
   * If the curve has no parameters, zero must be returned.
   * 
   * @return the number of parameters
   */
  public abstract int getParameterCount();

  /**
   * Returns a new curve for which each of the parameters has been adjusted.
   * <p>
   * The desired adjustment is specified using {@link ValueAdjustment}.
   * The size of the list of adjustments is expected to match the number of parameters.
   * If there are too many adjustments, no error will occur and the excess will be ignored.
   * If there are too few adjustments, no error will occur and the remaining points will not be adjusted.
   * 
   * @param adjustments  the adjustments to make
   * @return the new curve
   */
  public abstract PriceIndexCurve shiftedBy(List<ValueAdjustment> adjustments);

}
