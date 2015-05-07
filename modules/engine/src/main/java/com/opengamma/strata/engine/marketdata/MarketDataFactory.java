/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;

/**
 * A market data factory supplies the market data used in calculations. It can source observable data from
 * a data provider, and it can also build higher level market data, for example calibrated curves or volatility
 * surfaces.
 */
public interface MarketDataFactory {

  /**
   * Builds the market data required for performing calculations over a portfolio.
   * <p>
   * If the calculations require any data not provided in the {@code suppliedData} it is built by the
   * engine.
   *
   * @param requirements  the market data required for the calculations
   * @param suppliedData  market data supplied by the caller
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @return the market data required by the calculations plus details of any data that could not be built
   */
  public abstract BaseMarketDataResult buildBaseMarketData(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData,
      MarketDataConfig marketDataConfig);

  /**
   * Builds the market data required for performing calculations over a portfolio for a set of scenarios.
   * <p>
   * If the calculations require any data not provided in the {@code suppliedData} it is built by the
   * engine before applying the scenario definition.
   * <p>
   * If the scenario definition contains perturbations that apply to the inputs used to build market data,
   * the data must be built by this method, not provided in {@code suppliedData}.
   * <p>
   * For example, if a perturbation is defined that shocks the market quotes used to build a curve, the curve
   * must not be provided in {@code suppliedData}. The engine will only build the curve using the market quotes
   * if it is not found in {@code suppliedData}.
   *
   * @param requirements  the market data required for the calculations
   * @param suppliedData  the base market data used to derive the data for each scenario
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @return the market data required by the calculations
   */
  public abstract ScenarioMarketDataResult buildScenarioMarketData(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData,
      ScenarioDefinition scenarioDefinition,
      MarketDataConfig marketDataConfig);
}
