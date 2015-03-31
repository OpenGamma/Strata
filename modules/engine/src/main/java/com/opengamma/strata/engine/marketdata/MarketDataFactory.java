/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;

/**
 * A market data factory supplies the market data used in calculations. It can source observable data from
 * a data provider, and it can also build higher level market data, for example calibrated curves or volatility
 * surfaces.
 */
public interface MarketDataFactory {

  /**
   * Builds the market data required for performing calculations over a portfolio.
   * If the calculations require any data not provided in the {@code suppliedData} it is built by the
   * engine.
   *
   * @param requirements  the market data required for the calculations
   * @param suppliedData  market data supplied by the caller
   * @return the market data required by the calculations plus details of any data that could not be built
   */
  public abstract MarketDataResult buildBaseMarketData(MarketDataRequirements requirements, BaseMarketData suppliedData);

  /**
   * Builds the market data required for performing calculations over a portfolio for a set of scenarios.
   * The scenario data is derived by applying the perturbations in the scenario definition to the base data.
   *
   * @param baseData  the base market data used to derive the data for each scenario
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   * @return the market data required by the calculations
   */
  public abstract ScenarioMarketData buildScenarioMarketData(
      BaseMarketData baseData,
      ScenarioDefinition scenarioDefinition);
}
