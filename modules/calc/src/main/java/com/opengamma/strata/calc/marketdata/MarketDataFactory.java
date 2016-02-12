/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;

/**
 * A market data factory build market data. It can source observable data from a data provider
 * and it can also build higher level market data, for example calibrated curves or volatility
 * surfaces.
 */
public interface MarketDataFactory {

  /**
   * Builds a set of market data.
   * <p>
   * This builds market data based on the specified requirements and configuration.
   * Only data not already present in the {@code suppliedData} will be built.
   *
   * @param requirements  the market data required for the calculations
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @param suppliedData  market data supplied by the user
   * @param refData  the reference data
   * @return the market data required by the calculations plus details of any data that could not be built
   */
  public abstract MarketEnvironment buildMarketData(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      CalculationEnvironment suppliedData,
      ReferenceData refData);

  /**
   * Builds the market data required for performing calculations for a set of scenarios.
   * <p>
   * This builds market data based on the specified requirements and configuration.
   * Only data not already present in the {@code suppliedData} will be built.
   * The scenario definition will be applied, potentially generating multiple sets of market data.
   * <p>
   * If the scenario definition contains perturbations that apply to the inputs used to build market data,
   * the data must be built by this method, not provided in {@code suppliedData}.
   * <p>
   * For example, if a perturbation is defined that shocks the par rates used to build a curve, the curve
   * must not be provided in {@code suppliedData}. The engine will only build the curve using the par rates
   * if it is not found in {@code suppliedData}.
   *
   * @param requirements  the market data required for the calculations
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @param suppliedData  the base market data used to derive the data for each scenario
   * @param refData  the reference data
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   * @return the market data required by the calculations
   */
  public abstract MarketEnvironment buildMarketData(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      CalculationEnvironment suppliedData,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition);

}
