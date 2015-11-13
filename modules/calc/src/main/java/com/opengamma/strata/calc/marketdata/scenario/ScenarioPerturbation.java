/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.scenario;

/**
 * Describes a perturbation applied to a market data box to create market data for use in one or more scenarios.
 * <p>
 * A perturbation is used to change market data in some way.
 * It applies to a single piece of data, such as a discount curve or volatility surface.
 * For example, a 5 basis point parallel shift of a curve, or a 10% increase in the quoted price of a security.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 *
 * @param <T>  the type of the market data handled by the perturbation
 */
public interface ScenarioPerturbation<T> {

  /**
   * Returns a perturbation that returns its input unchanged.
   * <p>
   * This is useful for creating base scenarios where none of the market data is perturbed.
   *
   * @return a perturbation that returns its input unchanged
   */
  @SuppressWarnings("unchecked")
  public static <T> ScenarioPerturbation<T> none() {
    // It is safe to cast this to any type because it returns it input with no changes
    return (ScenarioPerturbation<T>) NoOpScenarioPerturbation.INSTANCE;
  }

  //-------------------------------------------------------------------------
  /**
   * Applies this perturbation to the market data in a box, returning a box containing new, modified data.
   * <p>
   * The original market data must not be altered.
   * Instead a perturbed copy must be returned.
   *
   * @param marketData  the market data to perturb
   * @return new market data derived by applying the perturbation to the input data
   */
  public abstract MarketDataBox<T> applyTo(MarketDataBox<T> marketData);

  /**
   * Returns the number of scenarios for which this perturbation generates data.
   *
   * @return the number of scenarios for which this perturbation generates data
   */
  public abstract int getScenarioCount();
}
