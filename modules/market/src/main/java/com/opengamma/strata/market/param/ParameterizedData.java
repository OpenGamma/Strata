/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import org.joda.beans.JodaBeanUtils;

/**
 * An abstraction of market data in terms of a number of arbitrary {@code double} parameters.
 * <p>
 * This interface provides an abstraction over many different kinds of market data,
 * including curves, surfaces and cubes. This abstraction allows an API to be structured
 * in such a way that it does not directly expose the underlying data.
 * For example, swaption volatilities might be based on a surface or a cube.
 * <p>
 * Implementations must be immutable and thread-safe.
 */
public interface ParameterizedData {

  /**
   * Gets the number of parameters.
   * <p>
   * This returns the number of parameters, which can be used to create a loop
   * to access the other methods on this interface.
   * 
   * @return the number of parameters
   */
  public abstract int getParameterCount();

  /**
   * Gets the value of the parameter at the specified index.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the value of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract double getParameter(int parameterIndex);

  /**
   * Gets the metadata of the parameter at the specified index.
   * <p>
   * If there is no specific parameter metadata, an empty instance will be returned.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract ParameterMetadata getParameterMetadata(int parameterIndex);

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of the data with the value at the specified index altered.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @param newValue  the new value for the specified parameter
   * @return a parameterized data instance based on this with the specified parameter altered
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract ParameterizedData withParameter(int parameterIndex, double newValue);

  /**
   * Returns a perturbed copy of the data.
   * <p>
   * The perturbation instance will be invoked once for each parameter in this instance,
   * returning the perturbed value for that parameter. The result of this method is a
   * new instance that is based on those perturbed values.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param perturbation  the perturbation to apply
   * @return a parameterized data instance based on this with the specified perturbation applied
   */
  public default ParameterizedData withPerturbation(ParameterPerturbation perturbation) {
    ParameterizedData result = this;
    for (int i = 0; i < getParameterCount(); i++) {
      double currentValue = getParameter(i);
      double perturbedValue = perturbation.perturbParameter(i, currentValue, getParameterMetadata(i));
      // compare using Double.doubleToLongBits()
      result = JodaBeanUtils.equal(currentValue, perturbedValue) ? result : result.withParameter(i, perturbedValue);
    }
    return result;
  }

}
