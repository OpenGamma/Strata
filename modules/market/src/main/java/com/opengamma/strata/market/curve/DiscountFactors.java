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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to discount factors for a single currency.
 * <p>
 * The discount factor represents the time value of money for the specified currency
 * when comparing the valuation date to the specified date.
 */
public interface DiscountFactors {

  /**
   * Gets the currency.
   * <p>
   * The currency that discount factors are provided for.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  /**
   * Gets the name of the underlying curve.
   * 
   * @return the underlying curve name
   */
  public abstract CurveName getCurveName();

  //-------------------------------------------------------------------------
  /**
   * Gets the discount factor.
   * <p>
   * The discount factor represents the time value of money for the specified currency
   * when comparing the valuation date to the specified date.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param date  the date to discount to
   * @return the discount factor
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double discountFactor(LocalDate date);

  /**
   * Gets the zero rate curve sensitivity for the discount factor.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity typically has the value {@code (-discountFactor * relativeTime)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * 
   * @param date  the date to discount to
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract PointSensitivityBuilder pointSensitivity(LocalDate date);

  /**
   * Returns the parameter sensitivity of the forward rate at the specified fixing date.
   * <p>
   * This returns the raw sensitivity for each parameter on the underlying curve.
   * If the fixing date is before the valuation date an exception is thrown.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * 
   * @param date  the fixing date to find the sensitivity for
   * @return the parameter sensitivity
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double[] parameterSensitivity(LocalDate date);

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
  DiscountFactors shiftedBy(DoubleBinaryOperator operator);

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
  DiscountFactors shiftedBy(List<ValueAdjustment> adjustments);

}
