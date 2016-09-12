/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

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
  private final Map<MarketDataId<?>, Object> values = new HashMap<>();
  /**
   * The time-series of historic market data values.
   */
  private final Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

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
   * @param values  the single value market data items, keyed by identifier
   * @param timeSeries  time-series of observable market data values, keyed by identifier
   */
  ImmutableMarketDataBuilder(
      LocalDate valuationDate,
      Map<MarketDataId<?>, Object> values,
      Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

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
  public ImmutableMarketDataBuilder valuationDate(LocalDate valuationDate) {
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
  public ImmutableMarketDataBuilder values(Map<? extends MarketDataId<?>, ?> values) {
    this.values.clear();
    return addValueMap(values);
  }

  /**
   * Sets the time-series in the builder, replacing any existing values.
   *
   * @param timeSeries  the time-series
   * @return this builder
   */
  public ImmutableMarketDataBuilder timeSeries(Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
    this.timeSeries.clear();
    return addTimeSeriesMap(timeSeries);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a value to the builder.
   *
   * @param id  the identifier
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> ImmutableMarketDataBuilder addValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, value);
    return this;
  }

  /**
   * Adds multiple values to the builder.
   *
   * @param values  the values
   * @return this builder
   */
  public ImmutableMarketDataBuilder addValueMap(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    values.entrySet().forEach(e -> {
      ImmutableMarketData.checkType(e.getKey(), e.getValue());
      this.values.put(e.getKey(), e.getValue());
    });
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a time-series of observable market data values.
   * <p>
   * Any existing time-series with the same identifier will be replaced.
   *
   * @param id  the identifier
   * @param timeSeries  a time-series of observable market data values
   * @return this builder
   */
  public ImmutableMarketDataBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(id, timeSeries);
    return this;
  }

  /**
   * Adds multiple time-series of observable market data values to the builder.
   * <p>
   * Any existing time-series with the same identifier will be replaced.
   *
   * @param timeSeriesMap  the map of time-series
   * @return this builder
   */
  public ImmutableMarketDataBuilder addTimeSeriesMap(
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap) {

    ArgChecker.notNull(timeSeriesMap, "timeSeriesMap");
    this.timeSeries.putAll(timeSeriesMap);
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
