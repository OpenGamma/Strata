/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A cube that maps a {@code double} x-value, y-value, z-value to a {@code double} w-value.
 * <p>
 * Implementations of this interface provide the ability to find a w-value on the cube
 * from the x-value, y-value, z-value.
 * <p>
 * Each implementation will be backed by a number of <i>parameters</i>.
 * The meaning of the parameters is implementation dependent.
 * The sensitivity of the result to each of the parameters can also be obtained.
 *
 * @see InterpolatedNodalCube
 */
public interface Cube extends ParameterizedData {

  /**
   * Gets the cube metadata.
   * <p>
   * This method returns metadata about the cube and the cube parameters.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If parameter metadata is present, the size of the list will match the number of parameters of this cube.
   *
   * @return the metadata
   */
  public abstract CubeMetadata getMetadata();

  /**
   * Returns a new cube with the specified metadata.
   * <p>
   * This allows the metadata of the cube to be changed while retaining all other information.
   * If parameter metadata is present, the size of the list must match the number of parameters of this cube.
   *
   * @param metadata  the new metadata for the cube
   * @return the new cube
   */
  public abstract Cube withMetadata(CubeMetadata metadata);

  /**
   * Gets the cube name.
   *
   * @return the cube name
   */
  public default CubeName getName() {
    return getMetadata().getCubeName();
  }

  @Override
  public default ParameterMetadata getParameterMetadata(int parameterIndex) {
    return getMetadata().getParameterMetadata(parameterIndex);
  }

  @Override
  public default OptionalInt findParameterIndex(ParameterMetadata metadata) {
    return getMetadata().findParameterIndex(metadata);
  }

  @Override
  public abstract Cube withParameter(int parameterIndex, double newValue);

  @Override
  public default Cube withPerturbation(ParameterPerturbation perturbation) {
    return (Cube) ParameterizedData.super.withPerturbation(perturbation);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the w-value for the specified x-value, y-value, z-value.
   *
   * @param x  the x-value to find the w-value for
   * @param y  the y-value to find the w-value for
   * @param z  the z-value to find the w-value for
   * @return the value at the (x,y,z) point
   */
  public abstract double wValue(double x, double y, double z);

  /**
   * Computes the sensitivity of the w-value with respect to the cube parameters.
   * <p>
   * This returns an array with one element for each x-y-z parameter of the cube.
   * The array contains one a sensitivity value for each parameter used to create the cube.
   *
   * @param x  the x-value at which the parameter sensitivity is computed
   * @param y  the y-value at which the parameter sensitivity is computed
   * @param z  the z-value at which the parameter sensitivity is computed
   * @return the sensitivity at the x/y/z point
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract UnitParameterSensitivity wValueParameterSensitivity(double x, double y, double z);

  /**
   * Computes the partial derivatives of the cube.
   * <p>
   * The first derivatives are {@code dw/dx, dw/dy, dw/dz}.
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to x
   * <li>[1] derivative with respect to y
   * <li>[2] derivative with respect to z
   * </ul>
   *
   * @param x  the x-value at which the partial derivative is taken
   * @param y  the y-value at which the partial derivative is taken
   * @param z  the z-value at which the partial derivative is taken
   * @return the w-value and it's partial first derivatives
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract ValueDerivatives firstPartialDerivatives(double x, double y, double z);

  //-------------------------------------------------------------------------
  /**
   * Creates a parameter sensitivity instance for this cube when the sensitivity values are known.
   * <p>
   * It can be useful to create a {@link UnitParameterSensitivity} from pre-computed sensitivity values.
   *
   * @param sensitivities  the sensitivity values, which must match the parameter count of the cube
   * @return the sensitivity
   */
  public default UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    List<ParameterMetadata> paramMeta = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
    return UnitParameterSensitivity.of(getName(), paramMeta, sensitivities);
  }

  /**
   * Creates a parameter sensitivity instance for this cube when the sensitivity values are known.
   * <p>
   * It can be useful to create a {@link CurrencyParameterSensitivity} from pre-computed sensitivity values.
   *
   * @param currency  the currency
   * @param sensitivities  the sensitivity values, which must match the parameter count of the cube
   * @return the sensitivity
   */
  public default CurrencyParameterSensitivity createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    List<ParameterMetadata> paramMeta = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
    return CurrencyParameterSensitivity.of(getName(), paramMeta, currency, sensitivities);
  }

}
