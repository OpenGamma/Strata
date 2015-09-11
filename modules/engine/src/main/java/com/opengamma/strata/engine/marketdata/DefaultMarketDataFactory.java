/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.Guavate.entriesToImmutableMap;
import static com.opengamma.strata.collect.Guavate.not;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.Perturbation;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.function.MarketDataFunction;
import com.opengamma.strata.engine.marketdata.function.MissingDataAwareObservableFunction;
import com.opengamma.strata.engine.marketdata.function.MissingDataAwareTimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.function.MissingMappingMarketDataFunction;
import com.opengamma.strata.engine.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.engine.marketdata.mapping.MissingDataAwareFeedIdMapping;
import com.opengamma.strata.engine.marketdata.scenario.PerturbationMapping;
import com.opengamma.strata.engine.marketdata.scenario.ScenarioDefinition;

/**
 * Co-ordinates building of market data.
 */
public final class DefaultMarketDataFactory implements MarketDataFactory {

  /** Provides time series of observable market data values. */
  private final TimeSeriesProvider timeSeriesProvider;

  /** Builds observable market data. */
  private final ObservableMarketDataFunction observablesBuilder;

  /** Market data functions, keyed by the type of the market data ID they can handle. */
  private final Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> functions;

  /** For looking up IDs that are suitable for a particular market data feed. */
  private final FeedIdMapping feedIdMapping;

  /**
   * Creates a new factory.
   *
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param observablesBuilder  builder to create observable market data
   * @param feedIdMapping  for looking up IDs that are suitable for a particular market data feed
   * @param functions  functions that create the market data
   */
  public DefaultMarketDataFactory(
      TimeSeriesProvider timeSeriesProvider,
      ObservableMarketDataFunction observablesBuilder,
      FeedIdMapping feedIdMapping,
      MarketDataFunction<?, ?>... functions) {

    this(timeSeriesProvider, observablesBuilder, feedIdMapping, ImmutableList.copyOf(functions));
  }

  /**
   * Creates a new factory.
   *
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param observablesBuilder  builder to create observable market data
   * @param feedIdMapping  for looking up IDs that are suitable for a particular market data feed
   * @param functions  functions that create the market data
   */
  @SuppressWarnings("unchecked")
  public DefaultMarketDataFactory(
      TimeSeriesProvider timeSeriesProvider,
      ObservableMarketDataFunction observablesBuilder,
      FeedIdMapping feedIdMapping,
      List<MarketDataFunction<?, ?>> functions) {

    // Wrap these 3 to handle market data where there is missing data for the calculation
    this.feedIdMapping = new MissingDataAwareFeedIdMapping(feedIdMapping);
    this.observablesBuilder = new MissingDataAwareObservableFunction(observablesBuilder);
    this.timeSeriesProvider = new MissingDataAwareTimeSeriesProvider(timeSeriesProvider);

    // Use a HashMap instead of an ImmutableMap.Builder so values can be overwritten.
    // If the functions argument includes a missing mapping builder it can overwrite the one inserted below
    Map<Class<? extends MarketDataId<?>>, MarketDataFunction<?, ?>> builderMap = new HashMap<>();

    // Add a builder that adds failures with helpful error messages when there is no mapping configured for a key type
    builderMap.put(
        MissingMappingMarketDataFunction.INSTANCE.getMarketDataIdType(),
        MissingMappingMarketDataFunction.INSTANCE);

    // Add a builder that adds failures with helpful error messages when there is no market data rule for a calculation
    builderMap.put(
        NoMatchingRulesMarketDataFunction.INSTANCE.getMarketDataIdType(),
        NoMatchingRulesMarketDataFunction.INSTANCE);

    functions.stream().forEach(builder -> builderMap.put(builder.getMarketDataIdType(), builder));
    this.functions = ImmutableMap.copyOf(builderMap);
  }

  @Override
  public MarketEnvironmentResult buildMarketEnvironment(
      MarketDataRequirements requirements,
      MarketEnvironment suppliedData,
      MarketDataConfig marketDataConfig,
      boolean includeIntermediateValues) {

    CalculationRequirements calcRequirements = CalculationRequirements.of(requirements);
    CalculationEnvironment calcEnv = buildCalculationEnvironment(calcRequirements, suppliedData, marketDataConfig);
    Map<MarketDataId<?>, Object> values;
    Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries;

    if (includeIntermediateValues) {
      // If the intermediate values are required then all the data from the calculation environment is included
      values = calcEnv.getValues();
      timeSeries = calcEnv.getTimeSeries();
    } else {
      // If the intermediate values are not required then the results are filtered to only include the values
      // requested in the requirements
      values = calcEnv.getValues().entrySet().stream()
          .filter(
              tp -> requirements.getNonObservables().contains(tp.getKey()) ||
                  requirements.getObservables().contains(tp.getKey()))
          .collect(entriesToImmutableMap());
      timeSeries = calcEnv.getTimeSeries().entrySet().stream()
          .filter(tp -> requirements.getTimeSeries().contains(tp.getKey()))
          .collect(entriesToImmutableMap());
    }
    MarketEnvironment marketEnvironment = MarketEnvironment.builder(calcEnv.getValuationDate())
        .addAllValues(values)
        .addAllTimeSeries(timeSeries)
        .build();

    return MarketEnvironmentResult.builder()
        .marketEnvironment(marketEnvironment)
        .singleValueFailures(calcEnv.getSingleValueFailures())
        .timeSeriesFailures(calcEnv.getTimeSeriesFailures())
        .build();
  }

  @Override
  public CalculationEnvironment buildCalculationEnvironment(
      CalculationRequirements requirements,
      MarketEnvironment suppliedData,
      MarketDataConfig marketDataConfig) {

    CalculationEnvironment builtData = CalculationEnvironment.of(suppliedData);

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
      // The leaves of the dependency tree represent market data with no dependencies that can be built immediately
      Pair<MarketDataNode, MarketDataRequirements> pair = root.withLeavesRemoved();

      // The requirements contained in the leaf nodes
      MarketDataRequirements leafRequirements = pair.getSecond();

      CalculationEnvironmentBuilder dataBuilder = builtData.toBuilder();

      // Time series of observable data ------------------------------------------------------------

      // Build any time series that are required but not available in the built data
      leafRequirements.getTimeSeries().stream()
          .filter(not(builtData::containsTimeSeries))
          .forEach(id -> dataBuilder.addTimeSeriesResult(id, findTimeSeries(id)));

      // Single values of observable data -----------------------------------------------------------

      // Filter out IDs for the data that is already present in the built data
      Set<ObservableId> observableIds = leafRequirements.getObservables().stream()
          .filter(not(builtData::containsValue))
          .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      Map<ObservableId, Result<Double>> observableResults = buildObservableData(observableIds);
      dataBuilder.addResults(observableResults);

      // Non-observable data -----------------------------------------------------------------------

      // Need to copy to an effectively final var to satisfy the compiler
      CalculationEnvironment tmpData = builtData;

      // Filter out IDs for the data that is already present in builtData and build the rest
      leafRequirements.getNonObservables().stream()
          .filter(not(tmpData::containsValue))
          .forEach(id -> dataBuilder.addResultUnsafe(id, buildNonObservableData(id, tmpData, marketDataConfig)));

      // --------------------------------------------------------------------------------------------

      // Put the data built so far into a CalculationEnvironment that will be used in the next phase of building data
      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return builtData;
  }

  @Override
  public ScenarioCalculationEnvironment buildScenarioCalculationEnvironment(
      CalculationRequirements requirements,
      MarketEnvironment suppliedData,
      ScenarioDefinition scenarioDefinition,
      MarketDataConfig marketDataConfig) {

    ScenarioCalculationEnvironmentBuilder dataBuilder =
        ScenarioCalculationEnvironment.builder(scenarioDefinition.getScenarioCount(), suppliedData.getValuationDate());
    ScenarioCalculationEnvironment builtData = dataBuilder.build();

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

    // As we build the leaf nodes we need to know their dependencies are even though they are no longer in the tree.
    // This maps from the market data ID to the original node, including all its dependencies
    Map<MarketDataId<?>, MarketDataNode> nodeMap = root.nodeMap();

    while (!root.isLeaf()) {
      // Effectively final reference to buildData which can be used in a lambda expression
      ScenarioCalculationEnvironment marketData = builtData;

      // The leaves of the dependency tree represent market data with no dependencies that can be built immediately
      Pair<MarketDataNode, MarketDataRequirements> pair = root.withLeavesRemoved();

      // The requirements contained in the leaf nodes
      MarketDataRequirements leafRequirements = pair.getSecond();

      // Time series of observable data ------------------------------------------------------------

      // Build any time series that are required but not available
      leafRequirements.getTimeSeries().stream()
          .filter(not(marketData::containsTimeSeries))
          .filter(not(suppliedData::containsTimeSeries))
          .forEach(id -> dataBuilder.addTimeSeriesResult(id, this.findTimeSeries(id)));

      // Copy supplied time series to the scenario data
      leafRequirements.getTimeSeries().stream()
          .filter(suppliedData::containsTimeSeries)
          .forEach(id -> dataBuilder.addTimeSeries(id, suppliedData.getTimeSeries(id)));

      // Single values of observable data -----------------------------------------------------------

      // Filter out IDs for the data that is already available
      Set<ObservableId> observableIds = leafRequirements.getObservables().stream()
          .filter(not(marketData::containsValues))
          .filter(not(suppliedData::containsValue))
          .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      Map<ObservableId, Result<Double>> observableResults = buildObservableData(observableIds);
      observableResults.entrySet().stream()
          .forEach(tp -> addObservableResult(tp.getKey(), tp.getValue(), scenarioDefinition, dataBuilder));

      // Copy observable data from the supplied data to the builder, applying any matching perturbations
      leafRequirements.getObservables().stream()
          .filter(suppliedData::containsValue)
          .forEach(id -> addObservableValue(id, suppliedData.getValue(id), scenarioDefinition, dataBuilder));

      // Non-observable data -----------------------------------------------------------------------

      // Filter out IDs for the data that is already available and build the rest
      leafRequirements.getNonObservables().stream()
          .filter(not(marketData::containsValues))
          .filter(not(suppliedData::containsValue))
          .forEach(id -> addNonObservableValues(id, marketDataConfig, nodeMap, marketData, scenarioDefinition, dataBuilder));

      // Copy supplied data to the scenario data after applying perturbations
      leafRequirements.getNonObservables().stream()
          .filter(suppliedData::containsValue)
          .forEach(id -> addNonObservableValue(id, suppliedData.getValue(id), scenarioDefinition, dataBuilder));

      // --------------------------------------------------------------------------------------------

      // Put the data built so far into an object that will be used in the next phase of building data
      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return builtData;
  }

  /**
   * Builds a non-observable market data value and adds it to the data builder.
   * <p>
   * If any of the dependencies of the item are in the scenario data then multiple values are built for the item,
   * one for each scenario. After the values are built the perturbation mappings from the scenario definition
   * are applied.
   * <p>
   * If all dependencies of the item are in the base data then a single value is built.
   * Perturbation mappings from the scenario are applied. If none of the mappings match the value it is
   * put into the base data. If any mappings match, the value is perturbed and the perturbed values are put
   * into the scenario data.
   *
   * @param id  ID of the market data
   * @param marketDataConfig  configuration used when building market data
   * @param nodeMap  map of market data ID to the node in the dependency graph for the market data value
   * @param marketData  the set of market data containing any dependencies required to build the value
   * @param scenarioDefinition  definition of a scenario used to perturb the built value
   * @param dataBuilder  the built values are put into this builder
   */
  private void addNonObservableValues(
      MarketDataId<?> id,
      MarketDataConfig marketDataConfig,
      Map<MarketDataId<?>, MarketDataNode> nodeMap,
      ScenarioCalculationEnvironment marketData,
      ScenarioDefinition scenarioDefinition,
      ScenarioCalculationEnvironmentBuilder dataBuilder) {

    // Gets a copy of the current node including the child nodes representing the dependencies of the node's value
    MarketDataNode node = nodeMap.get(id);
    Set<MarketDataId<?>> dependencyIds = node.getDependencies().stream()
        .map(MarketDataNode::getId)
        .collect(toImmutableSet());
    // This flag is true if any of the dependencies are in the scenario data.
    // If this is true then multiple values must be built for the ID using the scenario data
    // If this is false a single value must be built using the base data
    boolean dependencyInScenario = dependencyIds.stream().anyMatch(marketData::containsScenarioValues);

    if (dependencyInScenario) {
      Map<MarketDataId<?>, Result<List<?>>> results =
          buildNonObservableScenarioData(id, marketData, marketDataConfig, scenarioDefinition);

      results.entrySet().stream().forEach(e -> dataBuilder.addResultUnsafe(e.getKey(), e.getValue()));
    } else {
      // Build single base value for the ID using the base data as input.
      Result<?> result = buildNonObservableData(id, marketData.getSharedData(), marketDataConfig);
      applyScenariosToBaseResult(id, result, scenarioDefinition, dataBuilder);
    }
  }

  /**
   * Adds an item of observable market data to a builder.
   * <p>
   * If the result is a failure it is added to the base data failures.
   * <p>
   * If the result is a success it is passed to {@link #addObservableValue} where the scenario definition is
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
      ScenarioDefinition scenarioDefinition,
      ScenarioCalculationEnvironmentBuilder builder) {

    if (valueResult.isFailure()) {
      builder.addSharedResult(id, valueResult);
    } else {
      addObservableValue(id, valueResult.getValue(), scenarioDefinition, builder);
    }
  }

  /**
   * Adds an item of observable market data to a builder.
   * <p>
   * The mappings from the scenario definition is applied to the value. If any of the mappings match the value
   * is perturbed and the perturbed values are added to the scenario data. If none of the mappings match
   * the input value is added to the base data.
   *
   * @param id  ID of the market data value
   * @param value  the market data value
   * @param scenarioDefinition  definition of a set of scenarios
   * @param builder  the market data is added to this builder
   */
  private void addObservableValue(
      ObservableId id,
      double value,
      ScenarioDefinition scenarioDefinition,
      ScenarioCalculationEnvironmentBuilder builder) {

    // Filters and perturbations can be user-supplied and we can't guarantee they won't throw exceptions
    try {
      Optional<PerturbationMapping<?>> optionalMapping = scenarioDefinition.getMappings().stream()
          .filter(m -> m.matches(id, value))
          .findFirst();

      if (optionalMapping.isPresent()) {
        // This is definitely safe because the filter matched the value and the types of the filter and perturbation
        // are compatible
        @SuppressWarnings("unchecked")
        PerturbationMapping<Double> mapping = (PerturbationMapping<Double>) optionalMapping.get();
        List<Double> values = mapping.applyPerturbations(value);
        builder.addValues(id, values);
      } else {
        builder.addSharedValue(id, value);
      }
    } catch (RuntimeException e) {
      builder.addResult(id, Result.failure(e));
    }
  }

  /**
   * Builds items of observable market data.
   *
   * @param ids  IDs of the market data that should be built
   */
  private Map<ObservableId, Result<Double>> buildObservableData(Set<ObservableId> ids) {
    // We need to convert between the input IDs from the requirements and the feed IDs
    // which are passed to the builder and used to request the data.
    Map<ObservableId, ObservableId> feedIdToRequirementId = new HashMap<>();
    // IDs that are in the requirements but have no mapping to an ID the data provider understands
    Set<ObservableId> unmappedIds = new HashSet<>();

    // TODO Mapping of IDs should probably go inside the ObservableMarketDataBuilder
    for (ObservableId id : ids) {
      Optional<ObservableId> feedId = feedIdMapping.idForFeed(id);

      if (feedId.isPresent()) {
        feedIdToRequirementId.put(feedId.get(), id);
      } else {
        unmappedIds.add(id);
      }
    }
    Map<ObservableId, Result<Double>> builtValues = observablesBuilder.build(feedIdToRequirementId.keySet());
    ImmutableMap.Builder<ObservableId, Result<Double>> builder = ImmutableMap.builder();
    // Put the built data into the results, mapping the feed ID to the ID that was passed in
    builtValues.keySet().stream().forEach(id -> builder.put(feedIdToRequirementId.get(id), builtValues.get(id)));
    // Add failures for IDs that don't have mappings to market data feed IDs
    unmappedIds.forEach(id -> builder.put(id, noMappingResult(id)));
    return builder.build();
  }

  /**
   * Returns a failure result for an observable ID that can't be mapped to an ID recognised by the market
   * data feed.
   *
   * @param id  an observable ID that can't be mapped to an ID recognised by the market data feed
   * @return a failure result for the ID
   */
  private <T> Result<T> noMappingResult(ObservableId id) {
    return Result.failure(FailureReason.MISSING_DATA, "No feed ID mapping found for ID {}", id);
  }

  /**
   * Builds items of non-observable market data using a market data function and adds them to the results.
   *
   * @param id  ID of the market data that should be built
   * @param suppliedData  existing set of market data that contains any data required to build the values
   * @param marketDataConfig  configuration specifying how the market data should be built
   */
  @SuppressWarnings("unchecked")
  private Result<?> buildNonObservableData(
      MarketDataId id,
      MarketDataLookup suppliedData,
      MarketDataConfig marketDataConfig) {

    // The raw types in this method are an unfortunate necessity. The type parameters on MarketDataBuilder
    // are mainly a useful guide for implementors as they constrain the method type signatures.
    // In this class a mixture of functions with different types are stored in a map. This loses the type
    // parameter information. When the functions are extracted from the map and used it's impossible to
    // convince the compiler the operations are safe, although the logic guarantees it.

    // This cast removes a spurious warning
    MarketDataFunction marketDataFunction = functions.get((Class<? extends MarketDataId<?>>) id.getClass());

    return marketDataFunction != null ?
        marketDataFunction.build(id, suppliedData, marketDataConfig) :
        failureForMissingBuilder(id);
  }

  /**
   * Builds multiple versions of an item of market data, one for each scenario.
   * <p>
   * The values are rebuilt for each scenario on the assumption that its input data or market data source might be
   * different for each scenario.
   * <p>
   * After a value is built the perturbations in the scenario definition are examined and any applicable
   * perturbation is applied to the value.
   *
   * @param id ID of the market data value
   * @param marketData  market data containing any dependencies of the values being built
   * @param marketDataConfig  configuration specifying how market data should be built
   * @param scenarioDefinition  definition of the scenarios
   */
  private Map<MarketDataId<?>, Result<List<?>>> buildNonObservableScenarioData(
      MarketDataId<?> id,
      ScenarioCalculationEnvironment marketData,
      MarketDataConfig marketDataConfig,
      ScenarioDefinition scenarioDefinition) {

    ImmutableMap.Builder<MarketDataId<?>, Result<List<?>>> resultMap = ImmutableMap.builder();

    List<Result<?>> results = IntStream.range(0, scenarioDefinition.getScenarioCount()).boxed()
        .map(scenarioIndex -> new ScenarioMarketDataLookup(marketData, scenarioIndex))
        .map(data -> buildNonObservableData(id, data, marketDataConfig))
        .collect(toImmutableList());

    if (Result.anyFailures(results)) {
      resultMap.put(id, Result.failure(results));
    } else {
      List<Result<?>> perturbedValues = IntStream.range(0, results.size())
          .mapToObj(index -> perturbValue(id, results.get(index).getValue(), scenarioDefinition, index))
          .collect(toImmutableList());

      if (Result.anyFailures(perturbedValues)) {
        resultMap.put(id, Result.failure(perturbedValues));
      } else {
        List<Object> values = perturbedValues.stream().map(Result::getValue).collect(toImmutableList());
        resultMap.put(id, Result.success(values));
      }
    }
    return resultMap.build();
  }

  /**
   * Applies a perturbation from a single scenario to an item of market data if there is one that applies.
   * <p>
   * If there is no applicable perturbation in the scenario the input data is returned unchanged.
   *
   * @param id  ID of the market data value
   * @param marketDataValue  the market data value
   * @param scenarioDefinition  the definition of the scenarios
   * @param scenarioIndex  the index of the scenario from which the perturbation should be taken
   * @return the item of data with any applicable perturbation applied
   */
  @SuppressWarnings("unchecked")
  private Result<Object> perturbValue(
      MarketDataId<?> id,
      Object marketDataValue,
      ScenarioDefinition scenarioDefinition,
      int scenarioIndex) {

    Optional<PerturbationMapping<?>> mapping = scenarioDefinition.getMappings().stream()
        .filter(m -> m.matches(id, marketDataValue))
        .findFirst();

    if (!mapping.isPresent()) {
      return Result.success(marketDataValue);
    }
    // The perturbation is definitely compatible with the market data because the filter matched above
    Perturbation<Object> perturbation = (Perturbation<Object>) mapping.get().getPerturbations().get(scenarioIndex);
    try {
      return Result.success(perturbation.applyTo(marketDataValue));
    } catch (Exception e) {
      return Result.failure(e);
    }
  }

  /**
   * Applies perturbations from a scenario definition to a base market data value and puts the result into a builder.
   * <p>
   * If the result is a failure it is added to the base data failures. If it is a success the value is passed
   * to {@link #addNonObservableValue} and put into the builder after applying any applicable mappings from
   * the scenario definition.
   *
   * @param id  ID of the market data value
   * @param valueResult  a result containing the market data value
   * @param scenarioDefinition  the definition of the scenarios
   */
  private void applyScenariosToBaseResult(
      MarketDataId<?> id,
      Result<?> valueResult,
      ScenarioDefinition scenarioDefinition,
      ScenarioCalculationEnvironmentBuilder builder) {

    if (valueResult.isFailure()) {
      builder.addSharedResultUnsafe(id, valueResult);
    } else {
      addNonObservableValue(id, valueResult.getValue(), scenarioDefinition, builder);
    }
  }

  /**
   * Applies perturbations from a scenario definition to a base market data value and puts the result into a builder.
   * <p>
   * If no perturbations apply the base value is put into the base data. If there is an applicable perturbation
   * it is applied to create a market value for each scenario. These scenario values are put into the scenario data.
   *
   * @param id  ID of the market data value
   * @param marketDataValue  the market data value
   * @param scenarioDefinition  the definition of the scenarios
   */
  private void addNonObservableValue(
      MarketDataId<?> id,
      Object marketDataValue,
      ScenarioDefinition scenarioDefinition,
      ScenarioCalculationEnvironmentBuilder builder) {

    Optional<PerturbationMapping<?>> optionalMapping = scenarioDefinition.getMappings().stream()
        .filter(mapping -> mapping.matches(id, marketDataValue))
        .findFirst();

    if (!optionalMapping.isPresent()) {
      builder.addSharedValueUnsafe(id, marketDataValue);
    } else {
      // This is safe because the filter matched the value and the filter and perturbation types are compatible
      @SuppressWarnings("unchecked")
      PerturbationMapping<Object> mapping = (PerturbationMapping<Object>) optionalMapping.get();
      List<Object> perturbedValues = mapping.applyPerturbations(marketDataValue);
      builder.addValuesUnsafe(id, perturbedValues);
    }
  }

  /**
   * Returns a failure for the ID indicating there is no builder available to handle it.
   *
   * @param id the market data ID
   * @return a failure for the ID indicating there is no builder available to handle it
   */
  private Result<?> failureForMissingBuilder(MarketDataId<?> id) {
    return Result.failure(
        FailureReason.INVALID_INPUT,
        "No market data function available to handle {}",
        id);
  }

  /**
   * Looks up a time series in the time series provider.
   *
   * @param id  the ID of the data in the time series
   */
  private Result<LocalDateDoubleTimeSeries> findTimeSeries(ObservableId id) {
    // TODO Should this go in the time series provider?
    // Need to convert between the input ID from the requirements and the feed ID
    // which is used to store and retrieve the data.
    Optional<ObservableId> feedId = feedIdMapping.idForFeed(id);

    if (feedId.isPresent()) {
      return timeSeriesProvider.timeSeries(feedId.get());
    } else {
      return noMappingResult(id);
    }
  }
}
