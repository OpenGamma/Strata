/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;

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
   * Returns a new surface with the specified metadata.
   * <p>
   * This allows the metadata of the surface to be changed while retaining all other information.
   * If parameter metadata is present, the size of the list must match the number of parameters of this surface.
   * 
   * @param metadata  the new metadata for the surface
   * @return the new surface
   */
  @Override
  public abstract NodalSurface withMetadata(SurfaceMetadata metadata);

  /**
   * Gets the metadata of the parameter at the specified index.
   * <p>
   * If there is no specific parameter metadata, {@link SimpleSurfaceParameterMetadata} will be created.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  @Override
  public default ParameterMetadata getParameterMetadata(int parameterIndex) {
    return getMetadata().getParameterMetadata().map(pm -> pm.get(parameterIndex))
        .orElse(SimpleSurfaceParameterMetadata.of(
            getMetadata().getXValueType(),
            getXValues().get(parameterIndex),
            getMetadata().getYValueType(),
            getYValues().get(parameterIndex)));
  }

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
  @Override
  abstract NodalSurface withParameter(int parameterIndex, double newValue);

  @Override
  default NodalSurface withPerturbation(ParameterPerturbation perturbation) {
    return (NodalSurface) Surface.super.withPerturbation(perturbation);
  }

}
