/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;
import java.util.function.DoubleBinaryOperator;

import com.opengamma.strata.basics.value.ValueAdjustment;

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
   * Gets the known x-values of the curve.
   * <p>
   * This method returns the fixed x-values used to define the curve.
   * This will be of the same size as the y-values.
   * <p>
   * The implementation will clone any internal data, thus the result may be mutated.
   * 
   * @return the x-values
   */
  public abstract double[] getXValues();

  /**
   * Gets the known y-values of the curve.
   * <p>
   * This method returns the fixed y-values used to define the curve.
   * This will be of the same size as the x-values.
   * <p>
   * The implementation will clone any internal data, thus the result may be mutated.
   * 
   * @return the y-values
   */
  public abstract double[] getYValues();

  /**
   * Returns a new curve with the specified values.
   * <p>
   * This allows the y-values of the curve to be changed while retaining the same x-values.
   * <p>
   * The implementation will clone the input array.
   * 
   * @param values  the new y-values for the curve
   * @return the new curve
   */
  public abstract NodalCurve withYValues(double[] values);

  //-------------------------------------------------------------------------
  /**
   * Returns a new curve for which each of the parameters has been shifted.
   * <p>
   * The desired adjustment is specified using {@link DoubleBinaryOperator}.
   * <p>
   * The operator will be called once for each parameter of the curve.
   * The input will be the x and y values of the parameter.
   * The output will be the new y-value.
   * 
   * @param operator  the operator that provides the change
   * @return the new curve
   */
  public default NodalCurve shiftedBy(DoubleBinaryOperator operator) {
    double[] xValues = getXValues();
    double[] yValues = getYValues();
    double[] shifted = new double[yValues.length];
    for (int i = 0; i < yValues.length; i++) {
      shifted[i] = operator.applyAsDouble(xValues[i], yValues[i]);
    }
    return withYValues(shifted);
  }

  /**
   * Returns a new curve for which each of the parameters has been shifted.
   * <p>
   * The desired adjustment is specified using {@link ValueAdjustment}.
   * The size of the list of adjustments will typically match the number of parameters.
   * If there are too many adjustments, no error will occur and the excess will be ignored.
   * If there are too few adjustments, no error will occur and the remaining points will not be adjusted.
   * 
   * @param adjustments  the adjustments to make
   * @return the new curve
   */
  public default NodalCurve shiftedBy(List<ValueAdjustment> adjustments) {
    double[] shifted = getYValues();
    int minSize = Math.min(shifted.length, adjustments.size());
    for (int i = 0; i < minSize; i++) {
      shifted[i] = adjustments.get(i).adjust(shifted[i]);
    }
    return withYValues(shifted);
  }

  //-------------------------------------------------------------------------
  @Override
  public default NodalCurve toNodalCurve() {
    return this;
  }

}
