/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

/**
 * Describes a perturbation applied to a single piece of data as part of a scenario.
 * <p>
 * A perturbation is used to change market data in some way.
 * It applies to a single piece of data, such as a discount curve or volatility surface.
 * For example, a 5 basis point parallel shift of a curve, or a 10% increase in the quoted price of a security.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 *
 * @param <T>  the type of the market data handled by the perturbation
 */
@FunctionalInterface
public interface Perturbation<T> {

  /**
   * Returns a perturbation that returns its input unchanged.
   * <p>
   * This is useful for creating base scenarios where none of the market data is perturbed.
   *
   * @return a perturbation that returns its input unchanged
   */
  public static <T> Perturbation<T> none() {
    return marketData -> marketData;
  }

  //-------------------------------------------------------------------------
  /**
   * Applies this perturbation to the specified market data, returning a new, modified instance.
   * <p>
   * The original market data must not be altered.
   * Instead a perturbed copy must be returned.
   *
   * @param marketData  the single piece of market data to perturb
   * @return a new item of market data derived by applying the perturbation to the input data
   */
  public abstract T applyTo(T marketData);

}
