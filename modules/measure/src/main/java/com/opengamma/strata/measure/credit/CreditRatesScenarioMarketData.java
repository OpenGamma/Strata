package com.opengamma.strata.measure.credit;

import com.opengamma.strata.data.scenario.ScenarioMarketData;

public interface CreditRatesScenarioMarketData {

  /**
   * Gets the lookup that provides access to swaption volatilities.
   * 
   * @return the swaption lookup
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
