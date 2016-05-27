/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * A curve based on {@code double} nodal points.
 * <p>
 * This provides access to a curve mapping a {@code double} x-value to a {@code double} y-value.
 * <p>
 * The parameters of an x-y curve are the x-y values.
 * The values themselves are returned by {@link #getXValues()} and {@link #getYValues()}.
 * The metadata is returned by {@link #getMetadata()}.
 * 
 * @see InterpolatedNodalCurve
 */
public interface NodalCurve
    extends Curve {

  /**
   * Returns a new curve with the specified metadata.
   * <p>
   * This allows the metadata of the curve to be changed while retaining all other information.
   * If parameter metadata is present, the size of the list must match the number of parameters of this curve.
   * 
   * @param metadata  the new metadata for the curve
   * @return the new curve
   */
  @Override
  public abstract NodalCurve withMetadata(CurveMetadata metadata);

  /**
   * Gets the known x-values of the curve.
   * <p>
   * This method returns the fixed x-values used to define the curve.
   * This will be of the same size as the y-values.
   * 
   * @return the x-values
   */
  public abstract DoubleArray getXValues();

  /**
   * Gets the known y-values of the curve.
   * <p>
   * This method returns the fixed y-values used to define the curve.
   * This will be of the same size as the x-values.
   * 
   * @return the y-values
   */
  public abstract DoubleArray getYValues();

  /**
   * Returns a new curve with the specified values.
   * <p>
   * This allows the y-values of the curve to be changed while retaining the same x-values.
   * 
   * @param values  the new y-values for the curve
   * @return the new curve
   */
  public abstract NodalCurve withYValues(DoubleArray values);

  //-------------------------------------------------------------------------
  @Override
  abstract NodalCurve withParameter(int parameterIndex, double newValue);

  @Override
  default NodalCurve withPerturbation(ParameterPerturbation perturbation) {
    return (NodalCurve) Curve.super.withPerturbation(perturbation);
  }

}
