/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;
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
   * Gets the metadata of the parameter at the specified index.
   * <p>
   * If there is no specific parameter metadata, {@link SimpleCurveParameterMetadata} will be created.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  @Override
  public default ParameterMetadata getParameterMetadata(int parameterIndex) {
    return getMetadata().getParameterMetadata().map(pm -> pm.get(parameterIndex))
        .orElse(SimpleCurveParameterMetadata.of(getMetadata().getXValueType(), getXValues().get(parameterIndex)));
  }

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

  /**
   * Returns a new curve with the specified x-values and y-values.
   * <p>
   * This allows the x values and y-values of the curve to be changed.
   * 
   * @param xValues  the new x-values for the curve
   * @param yValues  the new y-values for the curve
   * @return the new curve
   */
  public abstract NodalCurve withValues(DoubleArray xValues, DoubleArray yValues);

  //-------------------------------------------------------------------------
  @Override
  abstract NodalCurve withParameter(int parameterIndex, double newValue);

  @Override
  default NodalCurve withPerturbation(ParameterPerturbation perturbation) {
    return (NodalCurve) Curve.super.withPerturbation(perturbation);
  }

  /**
   * Returns a new curve with an additional node, specifying the parameter metadata.
   * <p>
   * The result will contain the specified node.
   * If the x-value equals an existing x-value, the y-value will be changed.
   * If the x-value does not equal an existing x-value, the node will be added.
   * <p>
   * The result will only contain the specified parameter metadata if this curve also has parameter meta-data.
   * 
   * @param x  the new x-value
   * @param y  the new y-value
   * @param paramMetadata  the new parameter metadata
   * @return the updated curve
   */
  public abstract NodalCurve withNode(double x, double y, ParameterMetadata paramMetadata);

}
