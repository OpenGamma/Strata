/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
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
   * 
   * @return the x-values
   */
  public abstract DoubleArray getXValues();

  /**
   * Gets the known y-values of the surface.
   * <p>
   * This method returns the fixed y-values used to define the surface.
   * This will be of the same size as the x-values and z-values.
   * 
   * @return the y-values
   */
  public abstract DoubleArray getYValues();

  /**
   * Gets the known z-values of the surface.
   * <p>
   * This method returns the fixed z-values used to define the surface.
   * This will be of the same size as the x-values and y-values.
   * 
   * @return the z-values
   */
  public abstract DoubleArray getZValues();

  /**
   * Returns a new surface with the specified values.
   * <p>
   * This allows the z-values of the surface to be changed while retaining the
   * same x-values and y-values.
   * 
   * @param values  the new y-values for the surface
   * @return the new surface
   */
  public abstract NodalSurface withZValues(DoubleArray values);

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
    DoubleArray xValues = getXValues();
    DoubleArray yValues = getYValues();
    DoubleArray zValues = getZValues();
    DoubleArray shifted = zValues.mapWithIndex((i, v) -> operator.applyAsDouble(xValues.get(i), yValues.get(i), v));
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
    DoubleArray zValues = getZValues();
    return withZValues(zValues.mapWithIndex((i, v) -> i < adjustments.size() ? adjustments.get(i).adjust(v) : v));
  }

  //-------------------------------------------------------------------------
  @Override
  public default NodalSurface toNodalSurface() {
    return this;
  }

}
