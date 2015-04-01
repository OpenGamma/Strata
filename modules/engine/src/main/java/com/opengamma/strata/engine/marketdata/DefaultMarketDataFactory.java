/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.Guavate.not;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
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
import com.opengamma.strata.engine.marketdata.builders.ObservableMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.builders.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.VendorIdMapping;
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
  private final Map<Class<?>, MarketDataBuilder<?, ?>> builders;

  /** For looking up IDs that are suitable for a particular market data vendor. */
  private final VendorIdMapping vendorIdMapping;

  /**
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param observablesBuilder  builder to create observable market data
   * @param vendorIdMapping  for looking up IDs that are suitable for a particular market data vendor
   * @param builders  builders that create the market data
   */
  public DefaultMarketDataFactory(
      TimeSeriesProvider timeSeriesProvider,
      ObservableMarketDataBuilder observablesBuilder,
      VendorIdMapping vendorIdMapping,
      MarketDataBuilder<?, ?>... builders) {

    this(timeSeriesProvider, observablesBuilder, vendorIdMapping, ImmutableList.copyOf(builders));
  }

  /**
   * @param timeSeriesProvider  provides time series of observable market data values
   * @param observablesBuilder  builder to create observable market data
   * @param vendorIdMapping  for looking up IDs that are suitable for a particular market data vendor
   * @param builders  builders that create the market data
   */
  public DefaultMarketDataFactory(
      TimeSeriesProvider timeSeriesProvider,
      ObservableMarketDataBuilder observablesBuilder,
      VendorIdMapping vendorIdMapping,
      List<MarketDataBuilder<?, ?>> builders) {

    this.timeSeriesProvider = ArgChecker.notNull(timeSeriesProvider, "timeSeriesProvider");
    this.observablesBuilder = ArgChecker.notNull(observablesBuilder, "observablesBuilder");
    this.vendorIdMapping = ArgChecker.notNull(vendorIdMapping, "vendorIdMapping");
    this.builders = builders.stream()
        .collect(toImmutableMap(MarketDataBuilder::getMarketDataIdType, builder -> builder));
  }

  @Override
  public MarketDataResult buildBaseMarketData(MarketDataRequirements requirements, BaseMarketData suppliedData) {
    BaseMarketDataBuilder baseDataBuilder = suppliedData.toBuilder();
    ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<MarketDataId<?>, Result<?>> timeSeriesFailureBuilder = ImmutableMap.builder();

    requirements.getTimeSeries().stream()
        .filter(not(suppliedData::containsTimeSeries))
        .forEach(id -> addTimeSeries(baseDataBuilder, timeSeriesFailureBuilder, id));

    // TODO This method only works for a single level of dependencies.
    // i.e. A requirement can depend on other market data but that data must be in the supplied data.
    // This is adequate for the current use case where the requirements for individual curves depend
    // on the curve bundle, and the curve bundle is supplied.
    // Eventually we will need to recursively gather requirements from all the market data builders
    // into a tree and build the data from the leaves inwards.
    // This is only necessary for non-observable IDs as observable market data has no dependencies.

    buildObservableData(requirements.getObservables(), baseDataBuilder, failureBuilder);

    // TODO Is this necessary any more? Building in bulk was done for observable data which is now separate. FX?
    // Group the IDs by type so each type of market data can be built in bulk.
    // This can be more efficient for some types of data.
    Map<Class<?>, List<MarketDataId<?>>> idsByType =
        requirements.getNonObservables().stream()
            .collect(groupingBy(Object::getClass));

    // Build all instances of the same type of market data at the same time
    for (Map.Entry<Class<?>, List<MarketDataId<?>>> entry : idsByType.entrySet()) {
      Class<?> idType = entry.getKey();
      Set<MarketDataId<?>> ids = ImmutableSet.copyOf(entry.getValue());
      MarketDataBuilder<?, ?> builder = builders.get(idType);

      if (builder != null) {
        buildNonObservableData(builder, ids, suppliedData, baseDataBuilder, failureBuilder);
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

  @Override
  public ScenarioMarketData buildScenarioMarketData(BaseMarketData baseData, ScenarioDefinition scenarioDefinition) {
    throw new UnsupportedOperationException("buildScenarioMarketData not implemented");
  }

  /**
   * Builds items of observable market data and adds them to the results.
   *
   * @param ids  IDs of the market data that should be built
   * @param baseDataBuilder  a builder to receive the built data
   * @param failureBuilder  a builder to receive details of data that couldn't be built
   */
  private void buildObservableData(
      Set<? extends ObservableId> ids,
      BaseMarketDataBuilder baseDataBuilder,
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder) {

    // We need to convert between the input IDs from the requirements and the vendor IDs
    // which are passed to the builder and used to request the data.
    Map<ObservableId, ObservableId> idMappings =
        ids.stream()
            .collect(toImmutableMap(vendorIdMapping::idForVendor, id -> id));

    Map<ObservableId, Result<Double>> builtValues = observablesBuilder.build(idMappings.keySet());

    for (Map.Entry<ObservableId, Result<Double>> entry : builtValues.entrySet()) {
      ObservableId vendorId = entry.getKey();
      ObservableId id = idMappings.get(vendorId);
      Result<Double> result = entry.getValue();

      if (result.isSuccess()) {
        baseDataBuilder.addValue(id, result.getValue());
      } else {
        failureBuilder.put(id, result);
      }
    }
  }

  /**
   * Builds items of non-observable market data using a market data builder and adds them to the results.
   *
   * @param builder  the builder for building the market data
   * @param ids  IDs of the market data that should be built
   * @param suppliedData  existing set of market data that contains any data required to build the values
   * @param baseDataBuilder  a builder to receive the built data
   * @param failureBuilder  a builder to receive details of data that couldn't be built
   */
  @SuppressWarnings("unchecked")
  private void buildNonObservableData(
      MarketDataBuilder<?, ?> builder,
      Set<MarketDataId<?>> ids,
      BaseMarketData suppliedData,
      BaseMarketDataBuilder baseDataBuilder,
      ImmutableMap.Builder<MarketDataId<?>, Result<?>> failureBuilder) {

    // The raw types in this method are an unfortunate necessity. The type parameters on MarketDataBuilder
    // are mainly a useful guide for implementors as they constrain the method type signatures.
    // In this class a mixture of builders with different types are stored in a map. This loses the type
    // parameter information. When the builders are extracted from the map and used it's impossible to
    // convince the compiler the operations are safe, although the logic guarantees it.

    Map<? extends MarketDataId<?>, ? extends Result<?>> builtValues =
        builder.build(((Set) ids), suppliedData);

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

    // Need to convert between the input ID from the requirements and the vendor ID
    // which is used to store and retrieve the data.
    ObservableId vendorId = vendorIdMapping.idForVendor(id);
    Result<LocalDateDoubleTimeSeries> result = timeSeriesProvider.timeSeries(vendorId);

    if (result.isSuccess()) {
      dataBuilder.addTimeSeries(id, result.getValue());
    } else {
      failureBuilder.put(id, result);
    }
  }
}
