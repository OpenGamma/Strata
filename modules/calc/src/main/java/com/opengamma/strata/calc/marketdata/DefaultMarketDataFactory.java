/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.not;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * The default market data factory.
 * <p>
 * This uses two providers, one for observable data and one for time-series.
 */
final class DefaultMarketDataFactory implements MarketDataFactory {

  /** Builds observable market data. */
  private final ObservableDataProvider observableDataProvider;

  /** Provides time-series of observable market data values. */
  private final TimeSeriesProvider timeSeriesProvider;

  /** Market data functions, keyed by the type of the market data ID they can handle. */
  private final Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance of the factory based on providers of market data and time-series.
   * <p>
   * The market data functions are used to build the market data.
   *
   * @param observableDataProvider  the provider observable market data
   * @param timeSeriesProvider  the provider time-series
   * @param functions  the functions that create the market data
   */
  @SuppressWarnings("unchecked")
  DefaultMarketDataFactory(
      ObservableDataProvider observableDataProvider,
      TimeSeriesProvider timeSeriesProvider,
      List<MarketDataFunction<?, ?>> functions) {

    this.observableDataProvider = observableDataProvider;
    this.timeSeriesProvider = timeSeriesProvider;

    // Use a HashMap instead of an ImmutableMap.Builder so values can be overwritten.
    // If the functions argument includes a missing mapping builder it can overwrite the one inserted below
    Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> builderMap = new HashMap<>();

    functions.stream().forEach(builder -> builderMap.put(builder.getMarketDataIdType(), builder));
    this.functions = ImmutableMap.copyOf(builderMap);
  }

  //-------------------------------------------------------------------------
  @Override
  public BuiltMarketData create(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      MarketData suppliedData,
      ReferenceData refData) {

    ScenarioMarketData md = ScenarioMarketData.of(1, suppliedData);
    BuiltScenarioMarketData smd = createMultiScenario(requirements, marketDataConfig, md, refData, ScenarioDefinition.empty());
    return new BuiltMarketData(smd);
  }

  @Override
  public BuiltScenarioMarketData createMultiScenario(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      MarketData suppliedData,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition) {

    ScenarioMarketData md = ScenarioMarketData.of(1, suppliedData);
    return createMultiScenario(requirements, marketDataConfig, md, refData, scenarioDefinition);
  }

  @Override
  public BuiltScenarioMarketData createMultiScenario(
      MarketDataRequirements requirements,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData suppliedData,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition) {

    BuiltScenarioMarketDataBuilder dataBuilder = BuiltScenarioMarketData.builder(suppliedData.getValuationDate());
    BuiltScenarioMarketData builtData = dataBuilder.build();

    // Build a tree of the market data dependencies. The root of the tree represents the calculations.
    // The children of the root represent the market data directly used in the calculations. The children
    // of those nodes represent the market data required to build that data, and so on
    MarketDataNode root = MarketDataNode.buildDependencyTree(requirements, suppliedData, marketDataConfig, functions);

    // The leaf nodes of the dependency tree represent market data with no missing requirements for market data.
    // This includes:
    //   * Market data that is already available
    //   * Observable data whose value can be obtained from a market data provider
    //   * Market data that can be built from data that is already available
    //
    // Therefore the market data represented by the leaf nodes can be built immediately.
    //
    // Market data building proceeds in multiple steps. The operations in each step are:
    //   1) Build the market data represented by the leaf nodes of the dependency tree
    //   2) Create a copy of the dependency tree without the leaf nodes
    //   3) If the root of new dependency tree has children, go to step 1 with the new tree
    //
    // When the tree has no children it indicates all dependencies have been built and the market data
    // needed for the calculations is available.
    //
    // The result of this method also contains details of the problems for market data can't be built or found.

    while (!root.isLeaf()) {
      // Effectively final reference to buildData which can be used in a lambda expression
      BuiltScenarioMarketData marketData = builtData;

      // The leaves of the dependency tree represent market data with no dependencies that can be built immediately
      Pair<MarketDataNode, MarketDataRequirements> pair = root.withLeavesRemoved();

      // The requirements contained in the leaf nodes
      MarketDataRequirements leafRequirements = pair.getSecond();

      // Time series of observable data ------------------------------------------------------------

      // Build any time series that are required but not available
      leafRequirements.getTimeSeries().stream()
          .filter(id -> marketData.getTimeSeries(id).isEmpty())
          .filter(id -> suppliedData.getTimeSeries(id).isEmpty())
          .forEach(id -> dataBuilder.addTimeSeriesResult(id, timeSeriesProvider.provideTimeSeries(id)));

      // Copy supplied time series to the scenario data
      leafRequirements.getTimeSeries().stream()
          .filter(id -> !suppliedData.getTimeSeries(id).isEmpty())
          .forEach(id -> dataBuilder.addTimeSeries(id, suppliedData.getTimeSeries(id)));

      // Single values of observable data -----------------------------------------------------------

      // Filter out IDs for the data that is already available
      Set<ObservableId> observableIds = leafRequirements.getObservables().stream()
          .filter(not(marketData::containsValue))
          .filter(not(suppliedData::containsValue))
          .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      if (!observableIds.isEmpty()) {
        Map<ObservableId, Result<Double>> observableResults = observableDataProvider.provideObservableData(observableIds);
        MapStream.of(observableResults)
            .forEach((id, res) -> addObservableResult(id, res, refData, scenarioDefinition, dataBuilder));
      }

      // Copy observable data from the supplied data to the builder, applying any matching perturbations
      leafRequirements.getObservables().stream()
          .filter(suppliedData::containsValue)
          .forEach(id -> addValue(id, suppliedData.getValue(id), refData, scenarioDefinition, dataBuilder));

      // Non-observable data -----------------------------------------------------------------------

      // Filter out IDs for the data that is already available and build the rest
      Set<MarketDataId<?>> nonObservableIds = leafRequirements.getNonObservables().stream()
          .filter(not(marketData::containsValue))
          .filter(not(suppliedData::containsValue))
          .collect(toImmutableSet());

      Map<MarketDataId<?>, Result<MarketDataBox<?>>> nonObservableResults =
          buildNonObservableData(nonObservableIds, marketDataConfig, marketData, refData);

      MapStream.of(nonObservableResults)
          .forEach((id, result) -> addResult(id, result, refData, scenarioDefinition, dataBuilder));

      // Copy supplied data to the scenario data after applying perturbations
      leafRequirements.getNonObservables().stream()
          .filter(suppliedData::containsValue)
          .forEach(id -> addValue(id, suppliedData.getValue(id), refData, scenarioDefinition, dataBuilder));

      // --------------------------------------------------------------------------------------------

      // Put the data built so far into an object that will be used in the next phase of building data
      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return builtData;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds items of non-observable market data using a market data function.
   *
   * @param id  ID of the market data that should be built
   * @param marketDataConfig  configuration specifying how the market data should be built
   * @param suppliedData  existing set of market data that contains any data required to build the values
   * @param refData  the reference data, used to resolve trades
   * @return a result containing the market data or details of why it wasn't built
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Result<MarketDataBox<?>> buildNonObservableData(
      MarketDataId id,
      MarketDataConfig marketDataConfig,
      BuiltScenarioMarketData suppliedData,
      ReferenceData refData) {

    // The raw types in this method are an unfortunate necessity. The type parameters on MarketDataBuilder
    // are mainly a useful guide for implementors as they constrain the method type signatures.
    // In this class a mixture of functions with different types are stored in a map. This loses the type
    // parameter information. When the functions are extracted from the map and used it's impossible to
    // convince the compiler the operations are safe, although the logic guarantees it.

    // This cast removes a spurious warning
    Class<? extends MarketDataId<?>> idClass = (Class<? extends MarketDataId<?>>) id.getClass();
    MarketDataFunction marketDataFunction = functions.get(idClass);

    if (marketDataFunction == null) {
      throw new IllegalStateException("No market data function available for market data ID of type " + idClass.getName());
    }
    return Result.of(() -> marketDataFunction.build(id, marketDataConfig, suppliedData, refData));
  }

  @SuppressWarnings("unchecked")
  private Map<MarketDataId<?>, Result<MarketDataBox<?>>> buildNonObservableData(
      Set<? extends MarketDataId<?>> ids,
      MarketDataConfig marketDataConfig,
      BuiltScenarioMarketData marketData,
      ReferenceData refData) {

    return ids.stream()
        .collect(toImmutableMap(id -> id, id -> buildNonObservableData(id, marketDataConfig, marketData, refData)));
  }

  /**
   * Adds an item of market data to a builder.
   * <p>
   * If the result is a failure it is added to the list of failures.
   * <p>
   * If the result is a success it is passed to {@link #addValue} where the scenario definition is
   * applied and the data is added to the builder.
   *
   * @param id  ID of the market data value
   * @param valueResult  a result containing the market data value or details of why it couldn't be built
   * @param scenarioDefinition  definition of a set of scenarios
   * @param builder  the value or failure details are added to this builder
   */
  private void addResult(
      MarketDataId<?> id,
      Result<MarketDataBox<?>> valueResult,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition,
      BuiltScenarioMarketDataBuilder builder) {

    if (valueResult.isFailure()) {
      builder.addResult(id, valueResult);
    } else {
      addValue(id, valueResult.getValue(), refData, scenarioDefinition, builder);
    }
  }

  /**
   * Adds an item of observable market data to a builder.
   * <p>
   * If the result is a failure it is added to the list of failures.
   * <p>
   * If the result is a success it is passed to {@link #addValue} where the scenario definition is
   * applied and the data is added to the builder.
   *
   * @param id  ID of the market data value
   * @param valueResult  a result containing the market data value or details of why it couldn't be built
   * @param scenarioDefinition  definition of a set of scenarios
   * @param builder  the value or failure details are added to this builder
   */
  private void addObservableResult(
      ObservableId id,
      Result<Double> valueResult,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition,
      BuiltScenarioMarketDataBuilder builder) {

    if (valueResult.isFailure()) {
      builder.addResult(id, Result.failure(valueResult));
    } else {
      addValue(id, MarketDataBox.ofSingleValue(valueResult.getValue()), refData, scenarioDefinition, builder);
    }
  }

  /**
   * Adds an item of market data to a builder.
   * <p>
   * The mappings from the scenario definition is applied to the value. If any of the mappings match the value
   * is perturbed and the perturbed values are added to the market data.
   *
   * @param id  ID of the market data value
   * @param value  the market data value
   * @param scenarioDefinition  definition of a set of scenarios
   * @param builder  the market data is added to this builder
   */
  @SuppressWarnings("unchecked")
  private void addValue(
      MarketDataId<?> id,
      MarketDataBox<?> value,
      ReferenceData refData,
      ScenarioDefinition scenarioDefinition,
      BuiltScenarioMarketDataBuilder builder) {

    Optional<PerturbationMapping<?>> optionalMapping = scenarioDefinition.getMappings().stream()
        .filter(m -> m.matches(id, value, refData))
        .findFirst();

    if (optionalMapping.isPresent()) {
      // This is definitely safe because the filter matched the value and the types of the filter and perturbation
      // are compatible
      PerturbationMapping<Object> mapping = (PerturbationMapping<Object>) optionalMapping.get();
      MarketDataBox<Object> objectValue = ((MarketDataBox<Object>) value);
      // Result.of() catches any exceptions thrown by the mapping and wraps them in a failure
      Result<MarketDataBox<?>> result = Result.of(() -> mapping.applyPerturbation(objectValue, refData));
      builder.addResult(id, result);
    } else {
      builder.addBox(id, value);
    }
  }

}
