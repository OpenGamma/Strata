/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

/**
 * A function interface that allows a single parameter to be perturbed.
 * <p>
 * This interface is used by {@link ParameterizedData} to allow parameters to be
 * efficiently perturbed (altered). The method is invoked with the parameter index,
 * value and metadata, and must return the new value.
 */
@FunctionalInterface
public interface ParameterPerturbation {

  /**
   * Applies a perturbation to a single parameter.
   * <p>
   * This method receives three arguments describing a single parameter, the index,
   * current value and metadata. The result is the perturbed value.
   * 
   * @param index  the parameter index
   * @param value  the parameter value
   * @param metadata  the parameter metadata
   * @return the perturbed value
   */
  public abstract double perturbParameter(int index, double value, ParameterMetadata metadata);

}
