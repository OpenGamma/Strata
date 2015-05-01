/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.Guavate.not;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.MissingDataAwareObservableBuilder;
import com.opengamma.strata.engine.marketdata.builders.MissingDataAwareTimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.builders.MissingMappingMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.ObservableMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.engine.marketdata.mapping.MissingDataAwareFeedIdMapping;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * Co-ordinates building of market data.
 */
public final class DefaultMarketDataFactory implements MarketDataFactory {

  /** Provides time series of observable market data values. */
  private final TimeSeriesProvider timeSeriesProvider;

  /** Builds observable market data. */
  private final ObservableMarketDataBuilder observablesBuilder;

  /** Market data builders, keyed by the type of the market data ID they can handle. */
  private final Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builders;

  /** For looking up IDs that are suitable for a particular market data feed. */
  private final FeedIdMapping feedIdMapping;

  /**
   * Creates a new factory.
   *
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param observablesBuilder  builder to create observable market data
   * @param feedIdMapping  for looking up IDs that are suitable for a particular market data feed
   * @param builders  builders that create the market data
   */
  public DefaultMarketDataFactory(
      TimeSeriesProvider timeSeriesProvider,
      ObservableMarketDataBuilder observablesBuilder,
      FeedIdMapping feedIdMapping,
      MarketDataBuilder<?, ?>... builders) {

    this(timeSeriesProvider, observablesBuilder, feedIdMapping, ImmutableList.copyOf(builders));
  }

  /**
   * Creates a new factory.
   *
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param observablesBuilder  builder to create observable market data
   * @param feedIdMapping  for looking up IDs that are suitable for a particular market data feed
   * @param builders  builders that create the market data
   */
  @SuppressWarnings("unchecked")
  public DefaultMarketDataFactory(
      TimeSeriesProvider timeSeriesProvider,
      ObservableMarketDataBuilder observablesBuilder,
      FeedIdMapping feedIdMapping,
      List<MarketDataBuilder<?, ?>> builders) {

    // Wrap these 3 to handle market data where there is missing data for the calculation
    this.feedIdMapping = new MissingDataAwareFeedIdMapping(feedIdMapping);
    this.observablesBuilder = new MissingDataAwareObservableBuilder(observablesBuilder);
    this.timeSeriesProvider = new MissingDataAwareTimeSeriesProvider(timeSeriesProvider);

    // Use a HashMap instead of an ImmutableMap.Builder so values can be overwritten.
    // If the builders argument includes a missing mapping builder it can overwrite the one inserted below
    Map<Class<? extends MarketDataId<?>>, MarketDataBuilder<?, ?>> builderMap = new HashMap<>();

    // Add a builder that adds failures with helpful error messages when there is no mapping configured for a key type
    builderMap.put(
        MissingMappingMarketDataBuilder.INSTANCE.getMarketDataIdType(),
        MissingMappingMarketDataBuilder.INSTANCE);

    // Add a builder that adds failures with helpful error messages when there is no market data rule for a calculation
    builderMap.put(
        NoMatchingRulesMarketDataBuilder.INSTANCE.getMarketDataIdType(),
        NoMatchingRulesMarketDataBuilder.INSTANCE);

    builders.stream().forEach(builder -> builderMap.put(builder.getMarketDataIdType(), builder));
    this.builders = ImmutableMap.copyOf(builderMap);
  }

  @Override
  public MarketDataResult buildBaseMarketData(
      MarketDataRequirements requirements,
      BaseMarketData suppliedData) {

    ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<MarketDataId<?>, Result<?>> timeSeriesFailureBuilder = ImmutableMap.builder();
    BaseMarketData builtData = suppliedData;

    // Build a tree of the market data dependencies. The root of the tree represents the calculations.
    // The children of the root represent the market data directly used in the calculations. The children
    // of those nodes represent the market data required to build that data, and so on
    MarketDataNode root = MarketDataNode.buildDependencyTree(requirements, suppliedData, builders);

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

      // Build any time series that are required but not available in the built data
      leafRequirements.getTimeSeries().stream()
          .filter(not(builtData::containsTimeSeries))
          .forEach(id -> addTimeSeries(dataBuilder, timeSeriesFailureBuilder, id));

      // Filter out IDs for the data that is already present in the built data
      Set<ObservableId> observableIds =
          leafRequirements.getObservables().stream()
              .filter(not(builtData::containsValue))
              .collect(toImmutableSet());

      // Observable data is built in bulk so it can be efficiently requested from data provider in one operation
      buildObservableData(observableIds, dataBuilder, failureBuilder);

      // Need to copy to an effectively final var to satisfy the compiler
      BaseMarketData tmpData = builtData;

      // Filter out IDs for the data that is already present in builtData and build the rest
      leafRequirements.getNonObservables().stream()
          .filter(not(builtData::containsValue))
          .forEach(id -> buildNonObservableData(id, tmpData, dataBuilder, failureBuilder));

      builtData = dataBuilder.build();

      // A copy of the dependency tree not including the leaf nodes
      root = pair.getFirst();
    }
    return MarketDataResult.builder()
        .marketData(builtData)
        .singleValueFailures(failureBuilder.build())
        .timeSeriesFailures(timeSeriesFailureBuilder.build())
        .build();
  }

  @Override
  public ScenarioMarketData buildScenarioMarketData(
      BaseMarketData baseData,
      ScenarioDefinition scenarioDefinition) {

    throw new UnsupportedOperationException("buildScenarioMarketData not implemented");
  }

  /**
   * Builds items of observable market data and adds them to the results.
   *
   * @param requirementIds  IDs of the market data that should be built
   * @param baseDataBuilder  a builder to receive the built data
   * @param failureBuilder  a builder to receive details of data that couldn't be built
   */
  private void buildObservableData(
      Set<? extends ObservableId> requirementIds,
      BaseMarketDataBuilder baseDataBuilder,
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder) {

    // We need to convert between the input IDs from the requirements and the feed IDs
    // which are passed to the builder and used to request the data.
    Map<ObservableId, ObservableId> feedIdToRequirementId = new HashMap<>();
    // IDs that are in the requirements but have no mapping to an ID the data provider understands
    Set<ObservableId> unmappedIds = new HashSet<>();

    for (ObservableId id : requirementIds) {
      Optional<ObservableId> feedId = feedIdMapping.idForFeed(id);

      if (feedId.isPresent()) {
        feedIdToRequirementId.put(feedId.get(), id);
      } else {
        unmappedIds.add(id);
      }
    }
    Map<ObservableId, Result<Double>> builtValues = observablesBuilder.build(feedIdToRequirementId.keySet());

    for (Map.Entry<ObservableId, Result<Double>> entry : builtValues.entrySet()) {
      ObservableId feedId = entry.getKey();
      ObservableId id = feedIdToRequirementId.get(feedId);
      Result<Double> result = entry.getValue();

      if (result.isSuccess()) {
        baseDataBuilder.addValue(id, result.getValue());
      } else {
        failureBuilder.put(id, result);
      }
    }
    // Add failures for IDs that don't have mappings to market data feed IDs
    unmappedIds.forEach(id -> failureBuilder.put(id, noMappingResult(id)));
  }

  /**
   * Returns a failure result for an observable ID that can't be mapped to an ID recognised by the market
   * data feed.
   *
   * @param id  an observable ID that can't be mapped to an ID recognised by the market data feed
   * @return a failure result for the ID
   */
  private Result<Double> noMappingResult(ObservableId id) {
    return Result.failure(FailureReason.MISSING_DATA, "No feed ID mapping found for ID {}", id);
  }

  /**
   * Builds items of non-observable market data using a market data builder and adds them to the results.
   *
   * @param id  ID of the market data that should be built
   * @param suppliedData  existing set of market data that contains any data required to build the values
   * @param baseDataBuilder  a builder to receive the built data
   * @param failureBuilder  a builder to receive details of data that couldn't be built
   */
  @SuppressWarnings("unchecked")
  private void buildNonObservableData(
      MarketDataId id,
      BaseMarketData suppliedData,
      BaseMarketDataBuilder baseDataBuilder,
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder) {

    // The raw types in this method are an unfortunate necessity. The type parameters on MarketDataBuilder
    // are mainly a useful guide for implementors as they constrain the method type signatures.
    // In this class a mixture of builders with different types are stored in a map. This loses the type
    // parameter information. When the builders are extracted from the map and used it's impossible to
    // convince the compiler the operations are safe, although the logic guarantees it.

    // This cast removes a spurious warning
    MarketDataBuilder marketDataBuilder = builders.get((Class<? extends MarketDataId<?>>) id.getClass());

    if (marketDataBuilder == null) {
      addFailureForMissingBuilder(failureBuilder, id);
      return;
    }
    Result<?> result = marketDataBuilder.build(id, suppliedData);

    if (result.isSuccess()) {
      baseDataBuilder.addValue(id, result.getValue());
    } else {
      failureBuilder.put(id, result);
    }
  }

  /**
   * Adds a failure for the ID indicating there is no builder available to handle it.
   *
   * @param failureBuilder  builder for collecting failures when building market data
   * @param id  the market data ID
   */
  private void addFailureForMissingBuilder(
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder,
      MarketDataId<?> id) {

    Result<Object> failure =
        Result.failure(
            FailureReason.INVALID_INPUT,
            "No market data builder available to handle {}",
            id);

    failureBuilder.put(id, failure);
  }

  /**
   * Adds a time series to the data builder if it can be found, else add details of the failure to the failure builder.
   *
   * @param dataBuilder  builder for market data
   * @param failureBuilder  builder for details of failures when building time series
   * @param id  the ID of the data in the time series
   */
  private void addTimeSeries(
      BaseMarketDataBuilder dataBuilder,
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder,
      ObservableId id) {

    // Need to convert between the input ID from the requirements and the feed ID
    // which is used to store and retrieve the data.
    Optional<ObservableId> feedId = feedIdMapping.idForFeed(id);

    if (feedId.isPresent()) {
      Result<LocalDateDoubleTimeSeries> result = timeSeriesProvider.timeSeries(feedId.get());

      if (result.isSuccess()) {
        dataBuilder.addTimeSeries(id, result.getValue());
      } else {
        failureBuilder.put(id, result);
      }
    } else {
      failureBuilder.put(id, noMappingResult(id));
    }
  }
}
