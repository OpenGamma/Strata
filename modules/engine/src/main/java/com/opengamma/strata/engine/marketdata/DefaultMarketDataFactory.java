/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.Guavate.not;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.jooq.lambda.Seq;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.MissingDataAwareObservableFunction;
import com.opengamma.strata.engine.marketdata.functions.MissingDataAwareTimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.functions.MissingMappingMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.engine.marketdata.mapping.MissingDataAwareFeedIdMapping;
import com.opengamma.strata.engine.marketdata.scenarios.Perturbation;
import com.opengamma.strata.engine.marketdata.scenarios.PerturbationMapping;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;

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

  @SuppressWarnings("unchecked")
  @Override
  public BaseMarketDataResult buildBaseMarketData(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData,
      MarketDataConfig marketDataConfig) {

    ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<MarketDataId<?>, Result<?>> timeSeriesFailureBuilder = ImmutableMap.builder();
    BaseMarketData builtData = suppliedData;

    // Build a tree of the market data dependencies. The root of the tree represents the calculations.
    // The children of the root represent the market data directly used in the calculations. The children
    // of those nodes represent the market data required to build that data, and so on
    MarketDataNode root = MarketDataNode.buildDependencyTree(requirements, suppliedData, functions);

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

      BaseMarketDataBuilder dataBuilder = builtData.toBuilder();

      // Time series of observable data ------------------------------------------------------------

      // Build any time series that are required but not available in the built data
      Map<ObservableId, Result<LocalDateDoubleTimeSeries>> timeSeriesResults =
          leafRequirements.getTimeSeries().stream()
              .filter(not(builtData::containsTimeSeries))
              .collect(toImmutableMap(id -> id, this::findTimeSeries));

      for (Map.Entry<ObservableId, Result<LocalDateDoubleTimeSeries>> entry : timeSeriesResults.entrySet()) {
        if (entry.getValue().isSuccess()) {
          dataBuilder.addTimeSeries(entry.getKey(), entry.getValue().getValue());
        } else {
          timeSeriesFailureBuilder.put(entry.getKey(), entry.getValue());
        }
      }

      // Single values of observable data -----------------------------------------------------------

      // Filter out IDs for the data that is already present in the built data
      Set<ObservableId> observableIds =
          leafRequirements.getObservables().stream()
              .filter(not(builtData::containsValue))
              .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      Map<ObservableId, Result<Double>> observableResults = buildObservableData(observableIds);

      for (Map.Entry<ObservableId, Result<Double>> entry : observableResults.entrySet()) {
        if (entry.getValue().isSuccess()) {
          dataBuilder.addValue(entry.getKey(), entry.getValue().getValue());
        } else {
          failureBuilder.put(entry.getKey(), entry.getValue());
        }
      }

      // Non-observable data -----------------------------------------------------------------------

      // Need to copy to an effectively final var to satisfy the compiler
      BaseMarketData tmpData = builtData;

      // Filter out IDs for the data that is already present in builtData and build the rest
      Map<MarketDataId<?>, Result<?>> nonObservableResults =
          leafRequirements.getNonObservables().stream()
              .filter(not(builtData::containsValue))
              .collect(toImmutableMap(id -> id, id -> buildNonObservableData(id, tmpData, marketDataConfig)));

      for (Map.Entry<MarketDataId<?>, Result<?>> entry : nonObservableResults.entrySet()) {
        if (entry.getValue().isSuccess()) {
          MarketDataId id = entry.getKey();
          dataBuilder.addValue(id, entry.getValue().getValue());
        } else {
          failureBuilder.put(entry.getKey(), entry.getValue());
        }
      }

      // --------------------------------------------------------------------------------------------

      // Put the data built so far into a BaseMarketData that will be used in the next phase of building data
      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return BaseMarketDataResult.builder()
        .marketData(builtData)
        .singleValueFailures(failureBuilder.build())
        .timeSeriesFailures(timeSeriesFailureBuilder.build())
        .build();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ScenarioMarketDataResult buildScenarioMarketData(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData,
      ScenarioDefinition scenarioDefinition,
      MarketDataConfig marketDataConfig) {

    ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<MarketDataId<?>, Result<?>> timeSeriesFailureBuilder = ImmutableMap.builder();
    ScenarioMarketDataBuilder dataBuilder =
        ScenarioMarketData.builder(scenarioDefinition.getScenarioCount(), suppliedData.getValuationDate());
    ScenarioMarketData builtData = dataBuilder.build();

    // Apply perturbations to the supplied observable values and add to the builder
    for (ObservableId id : requirements.getObservables()) {
      if (suppliedData.containsValue(id)) {
        Double value = suppliedData.getValue(id);
        Result<List<Double>> result = applyScenarios(id, value, scenarioDefinition);

        if (result.isSuccess()) {
          dataBuilder.addValues(id, result.getValue());
        } else {
          failureBuilder.put(id, result);
        }
      }
    }
    // Apply perturbations to the supplied non-observable values and add to the builder
    for (MarketDataId<?> id : requirements.getNonObservables()) {
      if (suppliedData.containsValue(id)) {
        Object value = suppliedData.getValue(id);
        Result<List<Object>> result = perturbNonObservableValue(id, value, scenarioDefinition);

        if (result.isSuccess()) {
          dataBuilder.addValues((MarketDataId) id, result.getValue());
        } else {
          failureBuilder.put(id, result);
        }
      }
    }
    // Build a tree of the market data dependencies. The root of the tree represents the calculations.
    // The children of the root represent the market data directly used in the calculations. The children
    // of those nodes represent the market data required to build that data, and so on
    MarketDataNode root = MarketDataNode.buildDependencyTree(requirements, suppliedData, functions);

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

      // Time series of observable data ------------------------------------------------------------

      // Build any time series that are required but not available in the built data
      Map<ObservableId, Result<LocalDateDoubleTimeSeries>> timeSeriesResults =
          leafRequirements.getTimeSeries().stream()
              .filter(not(builtData::containsTimeSeries))
              .collect(toImmutableMap(id -> id, this::findTimeSeries));

      for (Map.Entry<ObservableId, Result<LocalDateDoubleTimeSeries>> entry : timeSeriesResults.entrySet()) {
        if (entry.getValue().isSuccess()) {
          dataBuilder.addTimeSeries(entry.getKey(), entry.getValue().getValue());
        } else {
          timeSeriesFailureBuilder.put(entry.getKey(), entry.getValue());
        }
      }

      // Single values of observable data -----------------------------------------------------------

      // Filter out IDs for the data that is already present in the built data
      Set<ObservableId> observableIds =
          leafRequirements.getObservables().stream()
              .filter(not(builtData::containsValues))
              .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      Map<ObservableId, Result<Double>> observableResults = buildObservableData(observableIds);

      for (Map.Entry<ObservableId, Result<Double>> entry : observableResults.entrySet()) {
        if (entry.getValue().isSuccess()) {
          Result<List<Double>> result = applyScenarios(entry.getKey(), entry.getValue().getValue(), scenarioDefinition);

          if (result.isSuccess()) {
            dataBuilder.addValues(entry.getKey(), result.getValue());
          } else {
            failureBuilder.put(entry.getKey(), result);
          }
        } else {
          failureBuilder.put(entry.getKey(), entry.getValue());
        }
      }

      // Non-observable data -----------------------------------------------------------------------

      // Filter out IDs for the data that is already present in the built data and build the rest
      List<MarketDataId<?>> nonObservableIds =
          leafRequirements.getNonObservables().stream()
              .filter(not(builtData::containsValues))
              .collect(toImmutableList());

      Map<MarketDataId<?>, Result<List<?>>> nonObservableScenarioResults =
          buildNonObservableScenarioData(nonObservableIds, builtData, marketDataConfig, scenarioDefinition);

      for (Map.Entry<MarketDataId<?>, Result<List<?>>> entry : nonObservableScenarioResults.entrySet()) {
        if (entry.getValue().isSuccess()) {
          // This local variable with a raw type is needed to keep the compiler happy
          MarketDataId id = entry.getKey();
          dataBuilder.addValues(id, entry.getValue().getValue());
        } else {
          failureBuilder.put(entry.getKey(), entry.getValue());
        }
      }

      // --------------------------------------------------------------------------------------------

      // Put the data built so far into an object that will be used in the next phase of building data
      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return ScenarioMarketDataResult.builder()
        .marketData(builtData)
        .singleValueFailures(failureBuilder.build())
        .timeSeriesFailures(timeSeriesFailureBuilder.build())
        .build();
  }

  /**
   * Applies any applicable perturbation from {@code scenarioDefinition} to an item of market data.
   *
   * @param id  ID of the market data value
   * @param value  the market data value
   * @param scenarioDefinition  definition of a set of scenarios
   * @return perturbed values derived from the market data value, one for each scenario
   */
  private Result<List<Double>> applyScenarios(ObservableId id, double value, ScenarioDefinition scenarioDefinition) {
    // Filters and perturbations can be user-supplied and we can't guarantee they won't throw exceptions
    try {
      Optional<PerturbationMapping<?>> mapping =
          scenarioDefinition.getMappings().stream()
              .filter(m -> m.matches(id, value))
              .findFirst();

      if (mapping.isPresent()) {
        List<Double> values = mapping.get().applyPerturbations(value);
        return Result.success(values);
      } else {
        List<Double> values = Collections.nCopies(scenarioDefinition.getScenarioCount(), value);
        return Result.success(values);
      }
    } catch (RuntimeException e) {
      return Result.failure(e);
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
   * Builds multiple versions of items of market data, one for each scenario.
   * <p>
   * The values are rebuilt for each scenario on the assumption that its input data or market data source might be
   * different for each scenario.
   * <p>
   * After a value is built the perturbations in the scenario definition are examined and any applicable
   * perturbation is applied to the value.
   *
   * @param ids  IDs of the market data values
   * @param marketData  market data containing any dependencies of the values being built
   * @param marketDataConfig  configuration specifying how market data should be built
   * @param scenarioDefinition  definition of the scenarios
   */
  private Map<MarketDataId<?>, Result<List<?>>> buildNonObservableScenarioData(
      List<MarketDataId<?>> ids,
      ScenarioMarketData marketData,
      MarketDataConfig marketDataConfig,
      ScenarioDefinition scenarioDefinition) {

    ImmutableMap.Builder<MarketDataId<?>, Result<List<?>>> resultMap = ImmutableMap.builder();

    for (MarketDataId<?> id : ids) {
      List<Result<?>> results =
          IntStream.range(0, scenarioDefinition.getScenarioCount()).boxed()
              .map(scenarioIndex -> new ScenarioMarketDataLookup(marketData, scenarioIndex))
              .map(data -> buildNonObservableData(id, data, marketDataConfig))
              .collect(toImmutableList());

      if (Result.anyFailures(results)) {
        resultMap.put(id, Result.failure(results));
      } else {
        List<Result<?>> perturbedValues =
            Seq.seq(results.stream())
                .map(Result::getValue)
                .zipWithIndex()
                .map(tp -> perturbNonObservableValue(id, tp.v1, scenarioDefinition, tp.v2.intValue()))
                .collect(toImmutableList());

        if (Result.anyFailures(perturbedValues)) {
          resultMap.put(id, Result.failure(perturbedValues));
        } else {
          List<Object> values = perturbedValues.stream().map(Result::getValue).collect(toImmutableList());
          resultMap.put(id, Result.success(values));
        }
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
  private Result<Object> perturbNonObservableValue(
      MarketDataId<?> id,
      Object marketDataValue,
      ScenarioDefinition scenarioDefinition,
      int scenarioIndex) {

    Optional<PerturbationMapping<?>> mapping =
        scenarioDefinition.getMappings().stream()
            .filter(m -> m.matches(id, marketDataValue))
            .findFirst();

    if (!mapping.isPresent()) {
      return Result.success(marketDataValue);
    }
    // The perturbation is definitely compatible with the market data because the filter matched above
    Perturbation<Object> perturbation = (Perturbation<Object>) mapping.get().getPerturbations().get(scenarioIndex);
    try {
      return Result.success(perturbation.apply(marketDataValue));
    } catch (Exception e) {
      return Result.failure(e);
    }
  }

  /**
   * Applies perturbations from a scenario definition to an item of non-observable market data if there is
   * one that applied.
   *
   * @param id  ID of the market data value
   * @param marketDataValue  the market data value
   * @param scenarioDefinition  the definition of the scenarios
   * @return the item of data with any applicable perturbations applied
   */
  private Result<List<Object>> perturbNonObservableValue(
      MarketDataId<?> id,
      Object marketDataValue,
      ScenarioDefinition scenarioDefinition) {

    List<Result<Object>> results =
        IntStream.range(0, scenarioDefinition.getScenarioCount())
            .mapToObj(idx -> perturbNonObservableValue(id, marketDataValue, scenarioDefinition, idx))
            .collect(toImmutableList());

    if (Result.anyFailures(results)) {
      return Result.failure(results);
    } else {
      List<Object> values = results.stream().map(Result::getValue).collect(toImmutableList());
      return Result.success(values);
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
