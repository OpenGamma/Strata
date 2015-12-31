/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A mutable builder for instances of {@link ImmutableMarketData}.
 */
public class ImmutableMarketDataBuilder {

  /**
   * The valuation date associated with the market data.
   */
  private LocalDate valuationDate;
  /**
   * The market data values.
   */
  private final Map<MarketDataKey<?>, Object> values = new HashMap<>();
  /**
   * The time-series of historic market data values.
   */
  private final Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  //-------------------------------------------------------------------------
  /**
   * Creates an empty builder.
   * 
   * @param valuationDate  the valuation date
   */
  ImmutableMarketDataBuilder(LocalDate valuationDate) {
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
  }

  /**
   * Creates a builder pre-populated with data.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param values  the single value market data items, keyed by ID
   * @param timeSeries  time-series of observable market data values, keyed by ID
   */
  ImmutableMarketDataBuilder(
      LocalDate valuationDate,
      Map<MarketDataKey<?>, Object> values,
      Map<ObservableKey, LocalDateDoubleTimeSeries> timeSeries) {

    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.values.putAll(ArgChecker.notNull(values, "values"));
    this.timeSeries.putAll(ArgChecker.notNull(timeSeries, "timeSeries"));
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the valuation date.
   *
   * @param valuationDate  the valuation date to set
   * @return this builder
   */
  public <T> ImmutableMarketDataBuilder valuationDate(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.valuationDate = valuationDate;
    return this;
  }

  /**
   * Sets the values in the builder, replacing any existing values.
   *
   * @param values  the values
   * @return this builder
   */
  public ImmutableMarketDataBuilder values(Map<? extends MarketDataKey<?>, ?> values) {
    this.values.clear();
    return addValues(values);
  }

  /**
   * Adds a value to the builder.
   *
   * @param key  the key which identifies the market data value
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> ImmutableMarketDataBuilder addValue(MarketDataKey<T> key, T value) {
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
  public ImmutableMarketDataBuilder addValues(Map<? extends MarketDataKey<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    values.entrySet().forEach(e -> {
      ImmutableMarketData.checkType(e.getKey(), e.getValue());
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
  public ImmutableMarketDataBuilder addValuesById(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    values.entrySet().forEach(e -> {
      MarketDataKey<?> key = e.getKey().toMarketDataKey();
      ImmutableMarketData.checkType(key, e.getValue());
      this.values.put(key, e.getValue());
    });
    return this;
  }

  /**
   * Sets the time-series in the builder, replacing any existing values.
   *
   * @param timeSeries  the time-series
   * @return this builder
   */
  public ImmutableMarketDataBuilder timeSeries(Map<? extends ObservableKey, LocalDateDoubleTimeSeries> timeSeries) {
    this.timeSeries.clear();
    return addTimeSeries(timeSeries);
  }

  /**
   * Adds a time-series of market data values to the builder.
   *
   * @param key  the key identifying the market data value
   * @param timeSeries  a time-series of the market data value
   * @return this builder
   */
  public ImmutableMarketDataBuilder addTimeSeries(ObservableKey key, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(key, "key");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(key, timeSeries);
    return this;
  }

  /**
   * Adds multiple time-series of market data values to the builder.
   *
   * @param timeSeriesMap  the time-series
   * @return this builder
   */
  public ImmutableMarketDataBuilder addTimeSeries(Map<? extends ObservableKey, LocalDateDoubleTimeSeries> timeSeriesMap) {
    ArgChecker.notNull(timeSeriesMap, "timeSeriesMap");
    timeSeries.putAll(timeSeriesMap);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a set of market data built from the data in this builder.
   *
   * @return a set of market data built from the data in this builder
   */
  public ImmutableMarketData build() {
    return new ImmutableMarketData(valuationDate, values, timeSeries);
  }

}
