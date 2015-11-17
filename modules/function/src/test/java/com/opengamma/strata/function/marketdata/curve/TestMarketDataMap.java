/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;
import java.util.Map;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test implementation of {@link CalculationMarketData} backed by a map.
 */
public final class TestMarketDataMap implements CalculationMarketData {

  private final MarketDataBox<LocalDate> valuationDate;

  private final Map<MarketDataKey<?>, Object> marketData;

  private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap;

  public TestMarketDataMap(
      LocalDate valuationDate,
      Map<MarketDataKey<?>, Object> marketData,
      Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap) {

    this.valuationDate = MarketDataBox.ofSingleValue(valuationDate);
    this.marketData = marketData;
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

  @SuppressWarnings("unchecked")
  @Override
  public <T> MarketDataBox<T> getValue(MarketDataKey<T> key) {
    T value = (T) marketData.get(key);

    if (value != null) {
      return MarketDataBox.ofSingleValue(value);
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

}
