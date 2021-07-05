/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * A surface that maps a {@code double} x-value and y-value to a {@code double} z-value.
 * <p>
 * Implementations of this interface provide the ability to find a z-value on the surface
 * from the x-value and y-value.
 * <p>
 * Each implementation will be backed by a number of <i>parameters</i>.
 * The meaning of the parameters is implementation dependent.
 * The sensitivity of the result to each of the parameters can also be obtained.
 * 
 * @see InterpolatedNodalSurface
 */
public interface Surface extends ParameterizedData {

  /**
   * Gets the surface metadata.
   * <p>
   * This method returns metadata about the surface and the surface parameters.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If parameter metadata is present, the size of the list will match the number of parameters of this surface.
   * 
   * @return the metadata
   */
  public abstract SurfaceMetadata getMetadata();

  /**
   * Returns a new surface with the specified metadata.
   * <p>
   * This allows the metadata of the surface to be changed while retaining all other information.
   * If parameter metadata is present, the size of the list must match the number of parameters of this surface.
   * 
   * @param metadata  the new metadata for the surface
   * @return the new surface
   */
  public abstract Surface withMetadata(SurfaceMetadata metadata);

  /**
   * Gets the surface name.
   * 
   * @return the surface name
   */
  public default SurfaceName getName() {
    return getMetadata().getSurfaceName();
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
  public abstract Surface withParameter(int parameterIndex, double newValue);

  @Override
  public default Surface withPerturbation(ParameterPerturbation perturbation) {
    return (Surface) ParameterizedData.super.withPerturbation(perturbation);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the z-value for the specified x-value and y-value.
   * 
   * @param x  the x-value to find the z-value for
   * @param y  the y-value to find the z-value for
   * @return the value at the x/y point
   */
  public abstract double zValue(double x, double y);

  /**
   * Computes the z-value for the specified pair of x-value and y-value.
   * 
   * @param xyPair  the pair of x-value and y-value to find the z-value for
   * @return the value at the x/y point
   */
  public default double zValue(DoublesPair xyPair) {
    return zValue(xyPair.getFirst(), xyPair.getSecond());
  }

  /**
   * Computes the sensitivity of the z-value with respect to the surface parameters.
   * <p>
   * This returns an array with one element for each x-y parameter of the surface.
   * The array contains one a sensitivity value for each parameter used to create the surface.
   * 
   * @param x  the x-value at which the parameter sensitivity is computed
   * @param y  the y-value at which the parameter sensitivity is computed
   * @return the sensitivity at the x/y/ point
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract UnitParameterSensitivity zValueParameterSensitivity(double x, double y);

  /**
   * Computes the sensitivity of the z-value with respect to the surface parameters.
   * <p>
   * This returns an array with one element for each x-y parameter of the surface.
   * The array contains one sensitivity value for each parameter used to create the surface.
   * 
   * @param xyPair  the pair of x-value and y-value at which the parameter sensitivity is computed
   * @return the sensitivity at the x/y/ point
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public default UnitParameterSensitivity zValueParameterSensitivity(DoublesPair xyPair) {
    return zValueParameterSensitivity(xyPair.getFirst(), xyPair.getSecond());
  }

  /**
   * Computes the partial derivatives of the surface.
   * <p>
   * The first derivatives are {@code dz/dx and dz/dy}.
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to x
   * <li>[1] derivative with respect to y
   * </ul>
   *
   * @param x  the x-value at which the partial derivative is taken
   * @param y  the y-value at which the partial derivative is taken
   * @return the z-value and it's partial first derivatives
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract ValueDerivatives firstPartialDerivatives(double x, double y);

  //-------------------------------------------------------------------------
  /**
   * Creates a parameter sensitivity instance for this surface when the sensitivity values are known.
   * <p>
   * In most cases, {@link #zValueParameterSensitivity(double, double)} should be used and manipulated.
   * However, it can be useful to create a {@link UnitParameterSensitivity} from pre-computed sensitivity values.
   * 
   * @param sensitivities  the sensitivity values, which must match the parameter count of the surface
   * @return the sensitivity
   */
  public default UnitParameterSensitivity createParameterSensitivity(DoubleArray sensitivities) {
    List<ParameterMetadata> paramMeta = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
    return UnitParameterSensitivity.of(getName(), paramMeta, sensitivities);
  }

  /**
   * Creates a parameter sensitivity instance for this surface when the sensitivity values are known.
   * <p>
   * In most cases, {@link #zValueParameterSensitivity(double, double)} should be used and manipulated.
   * However, it can be useful to create a {@link CurrencyParameterSensitivity} from pre-computed sensitivity values.
   * 
   * @param currency  the currency
   * @param sensitivities  the sensitivity values, which must match the parameter count of the surface
   * @return the sensitivity
   */
  public default CurrencyParameterSensitivity createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    List<ParameterMetadata> paramMeta = IntStream.range(0, getParameterCount())
        .mapToObj(i -> getParameterMetadata(i))
        .collect(toImmutableList());
    return CurrencyParameterSensitivity.of(getName(), paramMeta, currency, sensitivities);
  }

}
