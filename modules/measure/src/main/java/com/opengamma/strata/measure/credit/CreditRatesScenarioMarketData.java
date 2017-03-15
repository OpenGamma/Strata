/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Market data for products based on credit, discount and recovery rate curves, used for calculation across multiple scenarios.
 * <p>
 * This interface exposes the market data necessary for pricing credit products, such as CDS and CDS index.
 * It uses a {@link CreditRatesMarketDataLookup} to provide a view on {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface CreditRatesScenarioMarketData {

  /**
   * Gets the lookup that provides access to credit, discount and recovery rate curves.
   * 
   * @return the credit rates lookup
   */
  public abstract CreditRatesMarketDataLookup getLookup();

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
  public abstract CreditRatesScenarioMarketData withMarketData(ScenarioMarketData marketData);

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
  public abstract CreditRatesMarketData scenario(int scenarioIndex);

}
