/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

/**
 * Test implementation of {@link CalculationMarketData} backed by a map.
 */
public final class MarketDataMap
    implements CalculationMarketData, ObservableValues {

  private final LocalDate valuationDate;

  private final Map<MarketDataKey<?>, Object> marketData;

  private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap;

  public MarketDataMap(
      LocalDate valuationDate,
      Map<MarketDataKey<?>, Object> marketData,
      Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap) {

    this.valuationDate = valuationDate;
    this.marketData = marketData;
    this.timeSeriesMap = timeSeriesMap;
  }

  @Override
  public List<LocalDate> getValuationDates() {
    return ImmutableList.of(valuationDate);
  }

  @Override
  public int getScenarioCount() {
    return 1;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> getValues(MarketDataKey<T> key) {
    T value = (T) marketData.get(key);

    if (value != null) {
      return ImmutableList.of(value);
    } else {
      throw new IllegalArgumentException("No market data for " + key);
    }
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    LocalDateDoubleTimeSeries timeSeries = timeSeriesMap.get(key);

    if (timeSeries != null) {
      return timeSeries;
    } else {
      throw new IllegalArgumentException("No time series for " + key);
    }
  }

  @Override
  public <T, K extends MarketDataKey<T>> T getGlobalValue(K key) {
    throw new UnsupportedOperationException("getGlobalValue not implemented");
  }

  @Override
  public boolean containsValue(ObservableKey marketDataKey) {
    return marketData.containsKey(marketDataKey);
  }

  @Override
  public double getValue(ObservableKey marketDataKey) {
    Object value = marketData.get(marketDataKey);
    if (value instanceof Double) {
      return (Double) value;
    }
    throw new IllegalArgumentException("Market data not found: " + marketDataKey);
  }

}
