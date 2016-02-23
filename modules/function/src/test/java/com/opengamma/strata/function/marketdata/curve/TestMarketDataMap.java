/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test implementation of {@link CalculationMarketData} backed by a map.
 */
public final class TestMarketDataMap implements CalculationMarketData {

  private final MarketDataBox<LocalDate> valuationDate;

  private final Map<MarketDataKey<?>, Object> valueMap;

  private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap;

  public TestMarketDataMap(
      LocalDate valuationDate,
      Map<MarketDataKey<?>, Object> valueMap,
      Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap) {

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
  public Stream<MarketData> scenarios() {
    return IntStream.range(0, getScenarioCount())
        .mapToObj(scenarioIndex -> SingleCalculationMarketData.of(this, scenarioIndex));
  }

  @Override
  public MarketData scenario(int scenarioIndex) {
    return SingleCalculationMarketData.of(this, scenarioIndex);
  }

  @Override
  public boolean containsValue(MarketDataKey<?> key) {
    return valueMap.containsKey(key);
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataKey<T> key) {
    @SuppressWarnings("unchecked")
    T value = (T) valueMap.get(key);
    return value == null ? Optional.empty() : Optional.of(MarketDataBox.ofSingleValue(value));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> MarketDataBox<T> getValue(MarketDataKey<T> key) {
    T value = (T) valueMap.get(key);
    if (value != null) {
      return MarketDataBox.ofSingleValue(value);
    } else {
      throw new IllegalArgumentException("No market data for " + key);
    }
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    LocalDateDoubleTimeSeries timeSeries = timeSeriesMap.get(key);
    return timeSeries == null ? LocalDateDoubleTimeSeries.empty() : timeSeries;
  }

}
