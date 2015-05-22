/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.YearMonth;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to the values of a price index.
 * <p>
 * This provides historic and forward values for a single {@link PriceIndex}, such as 'US-CPI-U'.
 * This is typically used in inflation products.
 */
public interface PriceIndexValues {

  /**
   * Gets the Price index.
   * <p>
   * The index that the rates are for.
   * 
   * @return the Price index
   */
  public abstract PriceIndex getIndex();

  /**
   * Gets the valuation month.
   * <p>
   * The raw data in this provider is calibrated for this month.
   * 
   * @return the valuation month
   */
  public abstract YearMonth getValuationMonth();

  /**
   * Gets the time-series of fixings for the index.
   * <p>
   * The time-series contains historic fixings of the index.
   * It may be empty if the data is not available.
   * 
   * @return the time-series fixings
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries();

  /**
   * Gets the name of the underlying curve.
   * 
   * @return the underlying curve name
   */
  public abstract CurveName getCurveName();

  /**
   * Gets the number of parameters defining the curve.
   * <p>
   * If the curve has no parameters, zero must be returned.
   * 
   * @return the number of parameters
   */
  public abstract int getParameterCount();

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing month.
   * <p>
   * The rate of the Price index, such as 'US-CPI-U', varies over time.
   * This method obtains the actual or estimated rate for the month.
   * <p>
   * This retrieves the actual rate if the fixing month is before the valuation month,
   * or the estimated rate if the fixing month is after the valuation month.
   * If the month equals the valuation month, then the best available value is returned.
   * 
   * @param fixingMonth  the fixing month to query the rate for
   * @return the value of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double value(YearMonth fixingMonth);

  /**
   * Gets the point sensitivity of the historic or forward value at the specified fixing month.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward value.
   * If a time-series was used, then there is no sensitivity.
   * Otherwise, the sensitivity has the value 1.
   * The sensitivity refers to the result of {@link #value(YearMonth)}.
   * 
   * @param fixingMonth  the fixing month to find the sensitivity for
   * @return the point sensitivity of the value
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract PointSensitivityBuilder pointSensitivity(YearMonth fixingMonth);

  /**
   * Returns the sensitivities of the price index to the curve parameters at a given month.
   * 
   * @param month  the month to query the sensitivity for
   * @return the sensitivity array
   */
  public abstract double[] parameterSensitivity(YearMonth month);

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance for which each of the parameters in the curve has been shifted.
   * <p>
   * The desired adjustment is specified using {@link DoubleUnaryOperator}.
   * <p>
   * The operator will be called once for each point on the curve.
   * The input will be the x and y values of the point.
   * The output will be the new y value.
   * 
   * @param operator  the operator that provides the change
   * @return the new curve
   */
  public abstract PriceIndexValues shiftedBy(DoubleBinaryOperator operator);

  /**
   * Returns a new instance for which each of the parameters in the curve has been shifted.
   * <p>
   * The desired adjustment is specified using {@link ValueAdjustment}.
   * The size of the list of adjustments is expected to match the number of parameters.
   * If there are too many adjustments, no error will occur and the excess will be ignored.
   * If there are too few adjustments, no error will occur and the remaining points will not be adjusted.
   * 
   * @param adjustments  the adjustments to make
   * @return the new curve
   */
  public abstract PriceIndexValues shiftedBy(List<ValueAdjustment> adjustments);

}
