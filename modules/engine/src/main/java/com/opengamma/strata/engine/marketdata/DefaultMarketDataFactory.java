/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.Guavate.not;
import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.builders.MarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * Default market data factory implementation.
 */
public final class DefaultMarketDataFactory implements MarketDataFactory {

  /** Provides time series of observable market data values. */
  private final TimeSeriesProvider timeSeriesProvider;

  /** Market data builders, keyed by the type of the market data ID they can handle. */
  private final Map<Class<?>, MarketDataBuilder<?, ?>> builders;

  /**
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param builders  builders that create the market data
   */
  public DefaultMarketDataFactory(TimeSeriesProvider timeSeriesProvider, MarketDataBuilder<?, ?>... builders) {
    this(timeSeriesProvider, ImmutableList.copyOf(builders));
  }

  /**
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param builders  builders that create the market data
   */
  public DefaultMarketDataFactory(TimeSeriesProvider timeSeriesProvider, List<MarketDataBuilder<?, ?>> builders) {
    this.timeSeriesProvider = ArgChecker.notNull(timeSeriesProvider, "timeSeriesProvider");
    ImmutableMap.Builder<Class<?>, MarketDataBuilder<?, ?>> mapBuilder = ImmutableMap.builder();

    for (MarketDataBuilder<?, ?> builder : builders) {
      mapBuilder.put(builder.getMarketDataIdType(), builder);
    }
    this.builders = mapBuilder.build();
  }

  @Override
  public MarketDataResult buildBaseMarketData(MarketDataRequirements requirements, BaseMarketData suppliedData) {
    BaseMarketDataBuilder baseDataBuilder = suppliedData.toBuilder();
    ImmutableMap.Builder<MarketDataId<?>, Result<?>> timeSeriesFailureBuilder = ImmutableMap.builder();

    requirements.getTimeSeriesRequirements().stream()
        .filter(not(suppliedData::containsTimeSeries))
        .forEach(id -> addTimeSeries(baseDataBuilder, timeSeriesFailureBuilder, id));

    // TODO This method only works for a single level of dependencies.
    // i.e. A requirement can depend on other market data but that data must be in the supplied data.
    // This is adequate for the current use case where the requirements for individual curves depend
    // on the curve bundle, and the curve bundle is supplied.
    // Eventually we will need to recursively gather requirements from all the market data builders
    // into a tree and build the data from the leaves inwards.

    // Group the IDs by type so each type of market data can be built in bulk.
    // This can be more efficient for some types of data.
    Map<Class<?>, List<MarketDataId<?>>> idsByType =
        requirements.getSingleValueRequirements().stream()
            .filter(not(suppliedData::containsValue))
            .collect(groupingBy(Object::getClass));

    ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder = ImmutableMap.builder();

    for (Map.Entry<Class<?>, List<MarketDataId<?>>> entry : idsByType.entrySet()) {
      Class<?> idType = entry.getKey();
      Set<MarketDataId<?>> ids = ImmutableSet.copyOf(entry.getValue());
      MarketDataBuilder<?, ?> builder = builders.get(idType);

      if (builder != null) {
        buildValues(builder, ids, suppliedData, baseDataBuilder, failureBuilder);
      } else {
        addFailuresForMissingBuilder(failureBuilder, idType, ids);
      }
    }
    return MarketDataResult.builder()
        .marketData(baseDataBuilder.build())
        .singleValueFailures(failureBuilder.build())
        .timeSeriesFailures(timeSeriesFailureBuilder.build())
        .build();
  }

  /**
   * Builds items of market data using a market data builder and adds them to the results.
   *
   * @param builder  the builder for building the market data
   * @param ids  IDs of the market data that should be built
   * @param suppliedData  existing set of market data that contains any data required to build the values
   * @param baseDataBuilder  a builder to receive the built data
   * @param failureBuilder  a builder to receive details of data that couldn't be built
   */
  @SuppressWarnings("unchecked")
  private void buildValues(
      MarketDataBuilder<?, ?> builder,
      Set ids,
      BaseMarketData suppliedData,
      BaseMarketDataBuilder baseDataBuilder,
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder) {

    // The raw types in this method are an unfortunate necessity. The type parameters on MarketDataBuilder
    // are mainly a useful guide for implementors as they constrain the method type signatures.
    // In this class a mixture of builders with different types are stored in a map. This loses the type
    // parameter information. When the builders are extracted from the map and used it's impossible to
    // convince the compiler the operations are safe, although the logic guarantees it.
    Map<? extends MarketDataId<?>, ? extends Result<?>> builtValues =
        builder.build(ids, suppliedData);

    for (Map.Entry<? extends MarketDataId<?>, ? extends Result<?>> valueEntry : builtValues.entrySet()) {
      MarketDataId id = valueEntry.getKey();
      Result<?> result = valueEntry.getValue();

      if (result.isSuccess()) {
        baseDataBuilder.addValue(id, result.getValue());
      } else {
        failureBuilder.put(id, result);
      }
    }
  }

  /**
   * Adds a failure for each of the IDs indicating there is no builder available to handle it.
   *
   * @param failureBuilder  builder for collecting failures when building market data
   * @param idType  the type of the market data IDs
   * @param ids  the market data IDs
   */
  private void addFailuresForMissingBuilder(
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder,
      Class<?> idType,
      Set<MarketDataId<?>> ids) {

    Result<Object> failure =
        Result.failure(
            FailureReason.INVALID_INPUT,
            "No builder found for ID type {}",
            idType.getName());

    ids.forEach(id -> failureBuilder.put(id, failure));
  }

  @Override
  public ScenarioMarketData buildScenarioMarketData(BaseMarketData baseData, ScenarioDefinition scenarioDefinition) {
    throw new UnsupportedOperationException("buildScenarioMarketData not implemented");
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

    Result<LocalDateDoubleTimeSeries> result = timeSeriesProvider.timeSeries(id);

    if (result.isSuccess()) {
      dataBuilder.addTimeSeries(id, result.getValue());
    } else {
      failureBuilder.put(id, result);
    }
  }
}
