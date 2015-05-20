/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.LocalDate;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for an Overnight index.
 * <p>
 * This provides historic and forward rates for a single {@link OvernightIndex}, such as 'EUR-EONIA'.
 */
public interface OvernightIndexRates {

  /**
   * Gets the Overnight index.
   * <p>
   * The index that the rates are for.
   * 
   * @return the Overnight index
   */
  public abstract OvernightIndex getIndex();

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

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

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing date.
   * <p>
   * The rate of the overnight index, such as 'EUR-EONIA', varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * The reference period for the underlying deposit is computed from the index conventions.
   * 
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(LocalDate fixingDate);

  /**
   * Gets the point sensitivity of the historic or forward rate at the specified fixing date.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * Otherwise, the sensitivity has the value 1.
   * The sensitivity refers to the result of {@link #rate(LocalDate)}.
   * 
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public PointSensitivityBuilder pointSensitivity(LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing period.
   * <p>
   * The start date should be on or after the valuation date. The end date should be after the start date.
   * <p>
   * This computes the forward rate in the simple simply compounded convention of the index between two given date.
   * This is used mainly to speed-up computation by computing the rate on a longer period instead of each individual 
   * overnight rate. When data related to the overnight index rate are stored based on the fixing date and not
   * the start and end date of the period, the call may return an {@code IllegalArgumentException}.
   * 
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the simply compounded rate associated to the period for the index
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double periodRate(LocalDate startDate, LocalDate endDate);

  /**
   * Gets the point sensitivity of the historic or forward rate at the specified fixing period.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * The sensitivity will have the value 1.
   * The sensitivity refers to the result of {@link #periodRate(LocalDate, LocalDate)}.
   * 
   * @param startDate  the start or effective date of the period on which the rate is computed
   * @param endDate  the end or maturity date of the period on which the rate is computed
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public PointSensitivityBuilder periodRatePointSensitivity(LocalDate startDate, LocalDate endDate);

  //-------------------------------------------------------------------------
  /**
   * Returns the parameter sensitivity of the forward rate at the specified fixing date.
   * <p>
   * This returns the raw sensitivity for each parameter on the underlying curve.
   * If the fixing date is before the valuation date an exception is thrown.
   * The sensitivity refers to the result of {@link #rate(LocalDate)}.
   * 
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the parameter sensitivity
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double[] parameterSensitivity(LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Gets the number of parameters defining the curve.
   * <p>
   * If the curve has no parameters, zero must be returned.
   * 
   * @return the number of parameters
   */
  int getParameterCount();

  /**
   * Returns a new curve for which each of the parameters has been shifted.
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
  OvernightIndexRates shiftedBy(DoubleBinaryOperator operator);

  /**
   * Returns a new curve for which each of the parameters has been shifted.
   * <p>
   * The desired adjustment is specified using {@link ValueAdjustment}.
   * The size of the list of adjustments is expected to match the number of parameters.
   * If there are too many adjustments, no error will occur and the excess will be ignored.
   * If there are too few adjustments, no error will occur and the remaining points will not be adjusted.
   * 
   * @param adjustments  the adjustments to make
   * @return the new curve
   */
  OvernightIndexRates shiftedBy(List<ValueAdjustment> adjustments);

}
