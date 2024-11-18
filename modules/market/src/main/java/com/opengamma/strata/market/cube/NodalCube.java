/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * A cube based on {@code double} nodal points.
 * <p>
 * This provides access to a cube mapping a {@code double} x-value,
 * {@code double} y-value, {@code double} z-value to a {@code double} w-value.
 * <p>
 * The parameters of an x-y-z cube are the x-y-z values.
 * The values themselves are returned by {@link #getXValues()}, {@link #getYValues()},
 * {@link #getZValues()}, {@link #getWValues()}.
 * The metadata is returned by {@link #getMetadata()}.
 *
 * @see InterpolatedNodalCube
 */
public interface NodalCube
    extends Cube {

  /**
   * Returns a new cube with the specified metadata.
   * <p>
   * This allows the metadata of the cube to be changed while retaining all other information.
   * If parameter metadata is present, the size of the list must match the number of parameters of this cube.
   *
   * @param metadata the new metadata for the cube
   * @return the new cube
   */
  @Override
  public abstract NodalCube withMetadata(CubeMetadata metadata);

  /**
   * Gets the metadata of the parameter at the specified index.
   * <p>
   * If there is no specific parameter metadata, {@link SimpleCubeParameterMetadata} will be created.
   *
   * @param parameterIndex the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  @Override
  public default ParameterMetadata getParameterMetadata(int parameterIndex) {
    return getMetadata().getParameterMetadata().map(pm -> pm.get(parameterIndex))
        .orElse(SimpleCubeParameterMetadata.of(
            getMetadata().getXValueType(),
            getXValues().get(parameterIndex),
            getMetadata().getYValueType(),
            getYValues().get(parameterIndex),
            getMetadata().getZValueType(),
            getZValues().get(parameterIndex)));
  }

  /**
   * Gets the known x-values of the cube.
   * <p>
   * This method returns the fixed x-values used to define the cube.
   * This will be of the same size as the y-values, z-values and w-values.
   *
   * @return the x-values
   */
  public abstract DoubleArray getXValues();

  /**
   * Gets the known y-values of the cube.
   * <p>
   * This method returns the fixed y-values used to define the cube.
   * This will be of the same size as the y-values, z-values and w-values.
   *
   * @return the y-values
   */
  public abstract DoubleArray getYValues();

  /**
   * Gets the known z-values of the cube.
   * <p>
   * This method returns the fixed z-values used to define the cube.
   * This will be of the same size as the x-values, y-values and w-values.
   *
   * @return the z-values
   */
  public abstract DoubleArray getZValues();

  /**
   * Gets the known w-values of the cube.
   * <p>
   * This method returns the fixed w-values used to define the cube.
   * This will be of the same size as the x-values, y-values and z-values.
   *
   * @return the w-values
   */
  public abstract DoubleArray getWValues();

  /**
   * Returns a new cube with the specified values.
   * <p>
   * This allows the w-values of the cube to be changed while retaining the
   * same x-values, y-values, z-values.
   *
   * @param values the new w-values for the cube
   * @return the new cube
   */
  public abstract NodalCube withWValues(DoubleArray values);

  //-------------------------------------------------------------------------
  @Override
  public abstract NodalCube withParameter(int parameterIndex, double newValue);

  @Override
  public default NodalCube withPerturbation(ParameterPerturbation perturbation) {
    return (NodalCube) Cube.super.withPerturbation(perturbation);
  }

}
