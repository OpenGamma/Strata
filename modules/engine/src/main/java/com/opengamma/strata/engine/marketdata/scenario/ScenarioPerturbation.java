/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.scenario;

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
  public static <T> ScenarioPerturbation<T> none() {
    // TODO Does this need to be a bean so it can be serialized?
    return new ScenarioPerturbation<T>() {
      @Override
      public MarketDataBox<T> applyTo(MarketDataBox<T> marketData) {
        return marketData;
      }

      @Override
      public int getScenarioCount() {
        // A box with one scenario can be used for any number of scenarios
        return 1;
      }
    };
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
