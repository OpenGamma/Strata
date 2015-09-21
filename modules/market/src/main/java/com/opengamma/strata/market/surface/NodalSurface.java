/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.function.DoubleTenaryOperator;

/**
 * A surface based on {@code double} nodal points.
 * <p>
 * This provides access to a surface mapping a {@code double} x-value and
 * {@code double} y-value to a {@code double} z-value.
 * <p>
 * The parameters of an x-y surface are the x-y values.
 * The values themselves are returned by {@link #getXValues()} and {@link #getYValues()}.
 * The metadata is returned by {@link #getMetadata()}.
 * 
 * @see InterpolatedNodalSurface
 */
public interface NodalSurface
    extends Surface {

  /**
   * Gets the known x-values of the surface.
   * <p>
   * This method returns the fixed x-values used to define the surface.
   * This will be of the same size as the y-values and z-values.
   * <p>
   * The implementation will clone any internal data, thus the result may be mutated.
   * 
   * @return the x-values
   */
  public abstract double[] getXValues();

  /**
   * Gets the known y-values of the surface.
   * <p>
   * This method returns the fixed y-values used to define the surface.
   * This will be of the same size as the x-values and z-values.
   * <p>
   * The implementation will clone any internal data, thus the result may be mutated.
   * 
   * @return the y-values
   */
  public abstract double[] getYValues();

  /**
   * Gets the known z-values of the surface.
   * <p>
   * This method returns the fixed z-values used to define the surface.
   * This will be of the same size as the x-values and y-values.
   * <p>
   * The implementation will clone any internal data, thus the result may be mutated.
   * 
   * @return the z-values
   */
  public abstract double[] getZValues();

  /**
   * Returns a new surface with the specified values.
   * <p>
   * This allows the z-values of the surface to be changed while retaining the
   * same x-values and y-values.
   * <p>
   * The implementation will clone the input array.
   * 
   * @param values  the new y-values for the surface
   * @return the new surface
   */
  public abstract NodalSurface withZValues(double[] values);

  //-------------------------------------------------------------------------
  /**
   * Returns a new surface for which each of the parameters has been shifted.
   * <p>
   * The desired adjustment is specified using {@link DoubleUnaryOperator}.
   * <p>
   * The operator will be called once for each parameter of the curve.
   * The input will be the x, y and z values of the parameter.
   * The output will be the new z-value.
   * 
   * @param operator  the operator that provides the change
   * @return the new surface
   */
  public default NodalSurface shiftedBy(DoubleTenaryOperator operator) {
    double[] xValues = getXValues();
    double[] yValues = getYValues();
    double[] zValues = getZValues();
    double[] shifted = new double[zValues.length];
    for (int i = 0; i < yValues.length; i++) {
      shifted[i] = operator.applyAsDouble(xValues[i], yValues[i], zValues[i]);
    }
    return withZValues(shifted);
  }

  /**
   * Returns a new surface for which each of the parameters has been shifted.
   * <p>
   * The desired adjustment is specified using {@link ValueAdjustment}.
   * The size of the list of adjustments will typically match the number of parameters.
   * If there are too many adjustments, no error will occur and the excess will be ignored.
   * If there are too few adjustments, no error will occur and the remaining points will not be adjusted.
   * 
   * @param adjustments  the adjustments to make
   * @return the new surface
   */
  public default NodalSurface shiftedBy(List<ValueAdjustment> adjustments) {
    double[] shifted = getZValues();
    int minSize = Math.min(shifted.length, adjustments.size());
    for (int i = 0; i < minSize; i++) {
      shifted[i] = adjustments.get(i).adjust(shifted[i]);
    }
    return withZValues(shifted);
  }

  //-------------------------------------------------------------------------
  @Override
  public default NodalSurface toNodalSurface() {
    return this;
  }

}
