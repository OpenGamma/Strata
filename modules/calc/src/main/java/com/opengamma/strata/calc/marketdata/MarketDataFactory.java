/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Component that provides the ability to source and calibrate market data.
 * <p>
 * This component is used to create market data within Strata.
 * Each method receives a set of requirements defining the market data that is required.
 * This is typically obtained from {@link CalculationTasks#requirements(ReferenceData)}.
 * <p>
 * Given the requirements, the factory will determine whether any raw market data is needed.
 * This may use the {@link MarketDataConfig} to provide additional information.
 * <p>
 * If raw market data is required, the {@link ObservableDataProvider} and {@link TimeSeriesProvider}
 * will be invoked to supply it. Applications can implement these to supply data from an external source.
 * Alternatively, the raw market data can be passed into each method using the {@code suppliedData} parameter.
 * <p>
 * Once the raw data is obtained, the factory will determine whether it needs to be calibrated,
 * which may also involve additional information from the {@link MarketDataConfig}.
 * <p>
 * Two types of output can be built.
 * The {@code create} method is used to obtain and calibrate a single set of market data.
 * By contrast, the {@code createMultiScenario} methods are used to create data with multiple
 * scenarios based on a {@link ScenarioDefinition}.
 */
public interface MarketDataFactory {

  /**
   * Obtains an instance of the factory based on providers of market data and time-series.
   * <p>
   * The market data functions are used to build the market data.
   *
   * @param observableDataProvider  the provider of observable market data
   * @param timeSeriesProvider  the provider of time-series
   * @param functions  the functions that create the market data
   * @return the market data factory
   */
  public static MarketDataFactory of(
      ObservableDataProvider observableDataProvider,
      TimeSeriesProvider timeSeriesProvider,
      MarketDataFunction<?, ?>... functions) {

    return new DefaultMarketDataFactory(observableDataProvider, timeSeriesProvider, ImmutableList.copyOf(functions));
  }

  /**
   * Obtains an instance of the factory based on providers of market data and time-series.
   * <p>
   * The market data functions are used to build the market data.
   *
   * @param observableDataProvider  the provider of observable market data
   * @param timeSeriesProvider  the provider of time-series
   * @param functions  the functions that create the market data
   * @return the market data factory
   */
  @SuppressWarnings("unchecked")
  public static DefaultMarketDataFactory of(
      ObservableDataProvider observableDataProvider,
      TimeSeriesProvider timeSeriesProvider,
      List<MarketDataFunction<?, ?>> functions) {

    return new DefaultMarketDataFactory(observableDataProvider, timeSeriesProvider, functions);
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a set of market data.
   * <p>
   * This builds market data based on the specified requirements and configuration.
   * If some market data is known, it can be supplied using the {@link MarketData} interface.
   * Only data not already present in the {@code suppliedData} will be built.
   *
   * @param requirements  the market data required for the calculations
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @param suppliedData  market data supplied by the user
   * @param refData  the reference data
   * @return the market data required by the calculations plus details of any data that could not be built
   */
  public abstract BuiltMarketData create(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      MarketData suppliedData,
      ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Builds the market data required for performing calculations for a set of scenarios.
   * <p>
   * This builds market data based on the specified requirements and configuration.
   * If some market data is known, it can be supplied using the {@link MarketData} interface.
   * Only data not already present in the {@code suppliedData} will be built.
   * The scenario definition will be applied, potentially generating multiple sets of market data.
   * <p>
   * If the scenario definition contains perturbations that apply to the inputs used to build market data,
   * the data must be built by this method, not provided in {@code suppliedData}.
   * <p>
   * For example, if a perturbation is defined that shocks the par rates used to build a curve, the curve
   * must not be provided in {@code suppliedData}. The factory will only build the curve using the par rates
   * if it is not found in {@code suppliedData}.
   *
   * @param requirements  the market data required for the calculations
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @param suppliedData  the base market data used to derive the data for each scenario
   * @param refData  the reference data
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   * @return the market data required by the calculations
   */
  public abstract BuiltScenarioMarketData createMultiScenario(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      MarketData suppliedData,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition);

  /**
   * Builds the market data required for performing calculations for a set of scenarios.
   * <p>
   * This builds market data based on the specified requirements and configuration.
   * If some market data is known, it can be supplied using the {@link ScenarioMarketData} interface.
   * Only data not already present in the {@code suppliedData} will be built.
   * The scenario definition will be applied, potentially generating multiple sets of market data.
   * The number of scenarios in the supplied data must match that of the scenario definition.
   * <p>
   * If the scenario definition contains perturbations that apply to the inputs used to build market data,
   * the data must be built by this method, not provided in {@code suppliedData}.
   * <p>
   * For example, if a perturbation is defined that shocks the par rates used to build a curve, the curve
   * must not be provided in {@code suppliedData}. The factory will only build the curve using the par rates
   * if it is not found in {@code suppliedData}.
   *
   * @param requirements  the market data required for the calculations
   * @param marketDataConfig  configuration needed to build non-observable market data, for example curves or surfaces
   * @param suppliedData  the base market data used to derive the data for each scenario
   * @param refData  the reference data
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   * @return the market data required by the calculations
   */
  public abstract BuiltScenarioMarketData createMultiScenario(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData suppliedData,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition);

}
