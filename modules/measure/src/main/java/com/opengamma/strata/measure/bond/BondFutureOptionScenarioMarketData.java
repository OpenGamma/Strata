/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Market data for bond future options, used for calculation across multiple scenarios.
 * <p>
 * This interface exposes the market data necessary for pricing bond future options.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface BondFutureOptionScenarioMarketData {

  /**
   * Gets the lookup that provides access to bond future volatilities.
   * 
   * @return the bond future options lookup
   */
  public abstract BondFutureOptionMarketDataLookup getLookup();

  /**
   * Gets the market data.
   * 
   * @return the market data
   */
  public abstract ScenarioMarketData getMarketData();

  /**
   * Returns a copy of this instance with the specified market data.
   * 
   * @param marketData  the market data to use
   * @return a market view based on the specified data
   */
  public abstract BondFutureOptionScenarioMarketData withMarketData(ScenarioMarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the number of scenarios.
   * 
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Returns market data for a single scenario.
   * <p>
   * This returns a view of the market data for the specified scenario.
   * 
   * @param scenarioIndex  the scenario index
   * @return the market data for the specified scenario
   * @throws IndexOutOfBoundsException if the scenario index is invalid
   */
  public abstract BondFutureOptionMarketData scenario(int scenarioIndex);

}
