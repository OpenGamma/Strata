/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.Period;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;

/**
 * A curve that maps a {@code double} x-value to a {@code double} y-value.
 * <p>
 * Implementations of this interface provide the ability to find a y-value on the curve from the x-value.
 * <p>
 * Each implementation will be backed by a number of <i>parameters</i>.
 * The meaning of the parameters is implementation dependent.
 * The sensitivity of the result to each of the parameters can also be obtained.
 * 
 * @see InterpolatedNodalCurve
 */
public interface Curve {

  /**
   * Gets the curve metadata.
   * <p>
   * This method returns metadata about the curve and the curve parameters.
   * <p>
   * For example, a curve may be defined based on financial instruments.
   * The parameters might represent 1 day, 1 week, 1 month, 3 months, 6 months and 12 months.
   * The metadata could be used to describe each parameter in terms of a {@link Period}.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If parameter metadata is present, the size of the list will match the number of parameters of this curve.
   * 
   * @return the metadata
   */
  public abstract CurveMetadata getMetadata();

  /**
   * Gets the curve name.
   * 
   * @return the curve name
   */
  public default CurveName getName() {
    return getMetadata().getCurveName();
  }

  /**
   * Gets the number of parameters in the curve.
   * <p>
   * This returns the number of parameters that are used to define the curve.
   * 
   * @return the number of parameters
   */
  public abstract int getParameterCount();

  //-------------------------------------------------------------------------
  /**
   * Computes the y-value for the specified x-value.
   * 
   * @param x  the x-value to find the y-value for
   * @return the value at the x-value
   */
  public abstract double yValue(double x);

  /**
   * Computes the sensitivity of the y-value with respect to the curve parameters.
   * <p>
   * This returns an array with one element for each parameter of the curve.
   * The array contains the sensitivity of the y-value at the specified x-value to each parameter.
   * 
   * @param x  the x-value at which the parameter sensitivity is computed
   * @return the sensitivity
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract CurveUnitParameterSensitivity yValueParameterSensitivity(double x);

  /**
   * Computes the first derivative of the curve.
   * <p>
   * The first derivative is {@code dy/dx}.
   * 
   * @param x  the x-value at which the derivative is taken
   * @return the first derivative
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract double firstDerivative(double x);

  /**
   * Applies the perturbation to this curve.
   * <p>
   * This returns a curve that has been changed by the {@link Perturbation} instance.
   * 
   * @param perturbation  the perturbation to apply
   * @return the perturbed curve
   * @throws RuntimeException if the perturbation cannot be applied
   */
  public default Curve applyPerturbation(Perturbation<Curve> perturbation) {
    return perturbation.applyTo(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this curve to a nodal curve.
   * <p>
   * A nodal curve is based on specific x-y values, typically with interpolation.
   * See {@link InterpolatedNodalCurve} for more details.
   * 
   * @return the equivalent nodal curve
   * @throws UnsupportedOperationException if the curve cannot be converted
   */
  public default NodalCurve toNodalCurve() {
    throw new UnsupportedOperationException(Messages.format(
        "Unable to convert curve '{}' to NodalCurve, type was: {}", getName(), getClass().getName()));
  }

}
