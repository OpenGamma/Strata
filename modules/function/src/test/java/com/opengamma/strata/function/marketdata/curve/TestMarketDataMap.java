/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test implementation of {@link CalculationMarketData} backed by a map.
 */
public final class TestMarketDataMap implements CalculationMarketData {

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

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    @SuppressWarnings("unchecked")
    T value = (T) valueMap.get(id);
    return value == null ? Optional.empty() : Optional.of(MarketDataBox.ofSingleValue(value));
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
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries timeSeries = timeSeriesMap.get(id);
    return timeSeries == null ? LocalDateDoubleTimeSeries.empty() : timeSeries;
  }

}
