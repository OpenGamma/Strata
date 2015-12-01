/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Mutable builder for building instances of {@link MarketData}.
 */
public class MarketDataBuilder {

  /** The market data values. */
  private final Map<MarketDataKey<?>, Object> values = new HashMap<>();

  /** The time series of market data values. */
  private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  /**
   * Adds a value to the builder.
   *
   * @param key  the key which identifies the market data value
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> MarketDataBuilder addValue(MarketDataKey<T> key, T value) {
    ArgChecker.notNull(key, "key");
    ArgChecker.notNull(value, "value");
    values.put(key, value);
    return this;
  }

  /**
   * Adds multiple values to the builder.
   *
   * @param values  the values
   * @return this builder
   */
  public MarketDataBuilder addValues(Map<? extends MarketDataKey<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    values.entrySet().forEach(e -> {
      checkType(e.getKey(), e.getValue());
      this.values.put(e.getKey(), e.getValue());
    });
    return this;
  }

  /**
   * Adds multiple values to the builder.
   *
   * @param values  the values
   * @return this builder
   */
  public MarketDataBuilder addValuesById(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    values.entrySet().forEach(e -> {
      MarketDataKey<?> key = e.getKey().toMarketDataKey();
      checkType(key, e.getValue());
      this.values.put(key, e.getValue());
    });
    return this;
  }

  /**
   * Adds a time series of market data values to the builder.
   *
   * @param key  the key identifying the market data value
   * @param timeSeries  a time series of the market data value
   * @return this builder
   */
  public MarketDataBuilder addTimeSeries(ObservableKey key, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(key, "key");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(key, timeSeries);
    return this;
  }

  /**
   * Adds multiple time series of market data values to the builder.
   *
   * @param timeSeriesMap  the time series
   * @return this builder
   */
  public MarketDataBuilder addTimeSeries(Map<? extends ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap) {
    ArgChecker.notNull(timeSeriesMap, "timeSeriesMap");
    timeSeries.putAll(timeSeriesMap);
    return this;
  }

  /**
   * Returns a set of market data built from the data in this builder.
   *
   * @return a set of market data built from the data in this builder
   */
  public MarketData build() {
    return ImmutableMarketData.builder().values(values).timeSeries(timeSeries).build();
  }

  /**
   * Checks the value is an instance of the market data type of the key.
   */
  private static void checkType(MarketDataKey<?> key, Object value) {
    if (!key.getMarketDataType().isInstance(value)) {
      throw new IllegalArgumentException(
          Messages.format(
              "Value type doesn't match expected type. expected type: {}, value type: {}, key: {}, value: {}",
              key.getMarketDataType(),
              value.getClass().getName(),
              key,
              value));
    }
  }
}
