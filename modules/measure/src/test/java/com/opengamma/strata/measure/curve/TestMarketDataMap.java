/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.NamedMarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Test implementation of {@link ScenarioMarketData} backed by a map.
 */
public final class TestMarketDataMap implements ScenarioMarketData {

  private final MarketDataBox<LocalDate> valuationDate;

  private final Map<MarketDataId<?>, Object> valueMap;

  private final Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap;

  public TestMarketDataMap(
      LocalDate valuationDate,
      Map<MarketDataId<?>, Object> valueMap,
      Map<ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap) {

    this.valuationDate = MarketDataBox.ofSingleValue(valuationDate);
    this.valueMap = valueMap;
    this.timeSeriesMap = timeSeriesMap;
  }

  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return valuationDate;
  }

  @Override
  public int getScenarioCount() {
    return 1;
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    return valueMap.containsKey(id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    T value = (T) valueMap.get(id);
    if (value != null) {
      return MarketDataBox.ofSingleValue(value);
    } else {
      throw new IllegalArgumentException("No market data for " + id);
    }
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    @SuppressWarnings("unchecked")
    T value = (T) valueMap.get(id);
    return value == null ? Optional.empty() : Optional.of(MarketDataBox.ofSingleValue(value));
  }

  @Override
  public Set<MarketDataId<?>> getIds() {
    return ImmutableSet.copyOf(valueMap.keySet());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
    // no type check against id.getMarketDataType() as checked in factory
    return valueMap.keySet().stream()
        .filter(id -> id instanceof NamedMarketDataId)
        .filter(id -> ((NamedMarketDataId<?>) id).getMarketDataName().equals(name))
        .map(id -> (MarketDataId<T>) id)
        .collect(toImmutableSet());
  }

  @Override
  public Set<ObservableId> getTimeSeriesIds() {
    return timeSeriesMap.keySet();
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries timeSeries = timeSeriesMap.get(id);
    return timeSeries == null ? LocalDateDoubleTimeSeries.empty() : timeSeries;
  }

}
