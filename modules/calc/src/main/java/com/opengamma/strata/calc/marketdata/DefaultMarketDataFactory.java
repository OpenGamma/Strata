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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.MissingDataAwareObservableFunction;
import com.opengamma.strata.calc.marketdata.function.MissingDataAwareTimeSeriesProvider;
import com.opengamma.strata.calc.marketdata.function.MissingMappingMarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.calc.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.calc.marketdata.mapping.MissingDataAwareFeedIdMapping;
import com.opengamma.strata.calc.marketdata.scenario.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;

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
  public MarketEnvironment buildMarketData(
      MarketDataRequirements requirements,
      CalculationEnvironment suppliedData,
      MarketDataConfig marketDataConfig) {

    return buildMarketData(requirements, suppliedData, marketDataConfig, ScenarioDefinition.empty());
  }

  @Override
  public MarketEnvironment buildMarketData(
      MarketDataRequirements requirements,
      CalculationEnvironment suppliedData,
      MarketDataConfig marketDataConfig,
      ScenarioDefinition scenarioDefinition) {

    MarketEnvironmentBuilder dataBuilder = MarketEnvironment.builder().valuationDate(suppliedData.getValuationDate());
    MarketEnvironment builtData = dataBuilder.build();

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
      MarketEnvironment marketData = builtData;

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
          .filter(not(marketData::containsValue))
          .filter(not(suppliedData::containsValue))
          .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      Map<ObservableId, Result<Double>> observableResults = buildObservableData(observableIds);
      observableResults.entrySet().stream()
          .forEach(e -> addObservableResult(e.getKey(), e.getValue(), scenarioDefinition, dataBuilder));

      // Copy observable data from the supplied data to the builder, applying any matching perturbations
      leafRequirements.getObservables().stream()
          .filter(suppliedData::containsValue)
          .forEach(id -> addValue(id, suppliedData.getValue(id), scenarioDefinition, dataBuilder));

      // Non-observable data -----------------------------------------------------------------------

      // Filter out IDs for the data that is already available and build the rest
      Set<MarketDataId<?>> nonObservableIds = leafRequirements.getNonObservables().stream()
          .filter(not(marketData::containsValue))
          .filter(not(suppliedData::containsValue))
          .collect(toImmutableSet());

      Map<MarketDataId<?>, Result<MarketDataBox<?>>> nonObservableResults =
          buildNonObservableData(nonObservableIds, marketDataConfig, marketData);

      nonObservableResults.entrySet().stream()
          .forEach(e -> addResult(e.getKey(), e.getValue(), scenarioDefinition, dataBuilder));

      // Copy supplied data to the scenario data after applying perturbations
      leafRequirements.getNonObservables().stream()
          .filter(suppliedData::containsValue)
          .forEach(id -> addValue(id, suppliedData.getValue(id), scenarioDefinition, dataBuilder));

      // --------------------------------------------------------------------------------------------

      // Put the data built so far into an object that will be used in the next phase of building data
      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return builtData;
  }

  /**
   * Builds items of non-observable market data using a market data function.
   *
   * @param id  ID of the market data that should be built
   * @param suppliedData  existing set of market data that contains any data required to build the values
   * @param marketDataConfig  configuration specifying how the market data should be built
   * @return a result containing the market data or details of why it wasn't built
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Result<MarketDataBox<?>> buildNonObservableData(
      MarketDataId id,
      MarketEnvironment suppliedData,
      MarketDataConfig marketDataConfig) {

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
    return Result.of(() -> marketDataFunction.build(id, suppliedData, marketDataConfig));
  }

  @SuppressWarnings("unchecked")
  private Map<MarketDataId<?>, Result<MarketDataBox<?>>> buildNonObservableData(
      Set<? extends MarketDataId<?>> ids,
      MarketDataConfig marketDataConfig,
      MarketEnvironment marketData) {

    return ids.stream().collect(toImmutableMap(id -> id, id -> buildNonObservableData(id, marketData, marketDataConfig)));
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
      ScenarioDefinition scenarioDefinition,
      MarketEnvironmentBuilder builder) {

    if (valueResult.isFailure()) {
      builder.addResultUnsafe(id, valueResult);
    } else {
      addValue(id, valueResult.getValue(), scenarioDefinition, builder);
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
      ScenarioDefinition scenarioDefinition,
      MarketEnvironmentBuilder builder) {

    if (valueResult.isFailure()) {
      builder.addResultUnsafe(id, Result.failure(valueResult));
    } else {
      addValue(id, MarketDataBox.ofSingleValue(valueResult.getValue()), scenarioDefinition, builder);
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
      ScenarioDefinition scenarioDefinition,
      MarketEnvironmentBuilder builder) {

    Optional<PerturbationMapping<?>> optionalMapping = scenarioDefinition.getMappings().stream()
        .filter(m -> m.matches(id, value))
        .findFirst();

    if (optionalMapping.isPresent()) {
      // This is definitely safe because the filter matched the value and the types of the filter and perturbation
      // are compatible
      PerturbationMapping<Object> mapping = (PerturbationMapping<Object>) optionalMapping.get();
      MarketDataBox<Object> objectValue = ((MarketDataBox<Object>) value);
      // Result.of() catches any exceptions thrown by the mapping and wraps them in a failure
      Result<MarketDataBox<?>> result = Result.of(() -> mapping.applyPerturbation(objectValue));
      builder.addResultUnsafe(id, result);
    } else {
      builder.addValueUnsafe(id, value);
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
