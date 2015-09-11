/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.scenario.ScenarioDefinition;

/**
 * A market data factory build market data. It can source observable data from a data provider
 * and it can also build higher level market data, for example calibrated curves or volatility
 * surfaces.
 */
public interface MarketDataFactory {

  /**
   * Builds a set of market data.
   * <p>
   * If the requirements specify any data not provided in the {@code suppliedData} it is built by the
   * engine.
   * <p>
   * A market environment contains the basic market data values that are of interest to users. For example, a market
   * environment might contain market quotes, calibrated curves and volatility surfaces.
   * <p>
   * It is anticipated that {@link MarketEnvironment} will be exposed directly to users.
   * <p>
   * The market data used in calculations is provided by {@link CalculationEnvironment} or
   * {@link ScenarioCalculationEnvironment}. These contains the same data as {@link MarketEnvironment} plus
   * additional derived values used by the calculations and scenario framework.
   * <p>
   * {@link CalculationEnvironment} and {@link ScenarioCalculationEnvironment} can be built from a
   * {@link MarketEnvironment} using {@link #buildCalculationEnvironment} and
   * {@link #buildScenarioCalculationEnvironment}.
   *
   * @param requirements  the market data required
   * @param suppliedData  the market data supplied by the caller
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @param includeIntermediateValues  if this flag is true all market data values are returned including intermediate
   *   values used to build other values. If it is false the returned data will only include the values
   *   specified in tne requirements. This is intended to be used when debugging problems building market data
   * @return the requested market data plus details of any data that could not be built
   */
  public abstract MarketEnvironmentResult buildMarketEnvironment(
      MarketDataRequirements requirements,
      MarketEnvironment suppliedData,
      MarketDataConfig marketDataConfig,
      boolean includeIntermediateValues);

  /**
   * Builds the market data required for performing calculations over a portfolio.
   * <p>
   * If the calculations require any data not provided in the {@code suppliedData} it is built by the
   * engine.
   * <p>
   * {@link CalculationEnvironment} contains the same data as {@link MarketEnvironment} plus
   * additional derived values used by the calculations and scenario framework.
   *
   * @param requirements  the market data required for the calculations
   * @param suppliedData  market data supplied by the user
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @return the market data required by the calculations plus details of any data that could not be built
   */
  public abstract CalculationEnvironment buildCalculationEnvironment(
      CalculationRequirements requirements,
      MarketEnvironment suppliedData,
      MarketDataConfig marketDataConfig);

  /**
   * Builds the market data required for performing calculations over a portfolio for a set of scenarios.
   * <p>
   * If the calculations require any data not provided in the {@code suppliedData} it is built by the
   * engine before applying the scenario definition.
   * <p>
   * {@link ScenarioCalculationEnvironment} contains the same data as {@link MarketEnvironment} plus
   * additional derived values used by the calculations and scenario framework.
   * <p>
   * If the scenario definition contains perturbations that apply to the inputs used to build market data,
   * the data must be built by this method, not provided in {@code suppliedData}.
   * <p>
   * For example, if a perturbation is defined that shocks the par rates used to build a curve, the curve
   * must not be provided in {@code suppliedData}. The engine will only build the curve using the par rates
   * if it is not found in {@code suppliedData}.
   *
   * @param requirements  the market data required for the calculations
   * @param suppliedData  the base market data used to derive the data for each scenario
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @return the market data required by the calculations
   */
  public abstract ScenarioCalculationEnvironment buildScenarioCalculationEnvironment(
      CalculationRequirements requirements,
      MarketEnvironment suppliedData,
      ScenarioDefinition scenarioDefinition,
      MarketDataConfig marketDataConfig);
}
