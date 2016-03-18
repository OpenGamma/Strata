/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.Perturbation;

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
public interface Surface {

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
   * Gets the surface name.
   * 
   * @return the surface name
   */
  public default SurfaceName getName() {
    return getMetadata().getSurfaceName();
  }

  /**
   * Gets the number of parameters in the surface.
   * <p>
   * This returns the number of parameters that are used to define the surface.
   * 
   * @return the number of parameters
   */
  public abstract int getParameterCount();

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
  public abstract SurfaceUnitParameterSensitivity zValueParameterSensitivity(double x, double y);

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
  public default SurfaceUnitParameterSensitivity zValueParameterSensitivity(DoublesPair xyPair) {
    return zValueParameterSensitivity(xyPair.getFirst(), xyPair.getSecond());
  }

  /**
   * Applies the perturbation to this surface.
   * <p>
   * This returns a surface that has been changed by the {@link Perturbation} instance.
   * 
   * @param perturbation  the perturbation to apply
   * @return the perturbed surface
   * @throws RuntimeException if the perturbation cannot be applied
   */
  public default Surface applyPerturbation(Perturbation<Surface> perturbation) {
    return perturbation.applyTo(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Concerts this surface to a nodal surface.
   * <p>
   * A nodal surface is based on specific x-y-z values, typically with interpolation.
   * See {@link InterpolatedNodalSurface} for more details.
   * 
   * @return the equivalent nodal surface
   * @throws UnsupportedOperationException if the surface cannot be converted
   */
  public default NodalSurface toNodalSurface() {
    throw new UnsupportedOperationException(Messages.format(
        "Unable to convert surface '{}' to NodalSurface, type was: {}", getName(), getClass().getName()));
  }

}
