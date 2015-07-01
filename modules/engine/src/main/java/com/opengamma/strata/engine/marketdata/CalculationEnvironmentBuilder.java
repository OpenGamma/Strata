/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A mutable builder for building up {@link CalculationEnvironment} instances.
 */
public final class CalculationEnvironmentBuilder {

  /** The valuation date associated with the market data. */
  private LocalDate valuationDate;

  /** The single value market data items, keyed by ID. */
  private final Map<MarketDataId<?>, Object> values = new HashMap<>();

  /** Time series of observable market data values, keyed by ID. */
  private final Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  /** Details of failures when building single market data values. */
  private final Map<MarketDataId<?>, Failure> singleValueFailures = new HashMap<>();

  /** Details of failures when building time series of market data values. */
  private final Map<MarketDataId<?>, Failure> timeSeriesFailures = new HashMap<>();

  /**
   * Creates a builder with a valuation date but no market data.
   *
   * @param valuationDate  the valuation date associated with the market data
   */
  CalculationEnvironmentBuilder(LocalDate valuationDate) {
    this.valuationDate = valuationDate;
  }

  /**
   * Creates a builder pre-populated with data.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param values  the single value market data items, keyed by ID
   * @param timeSeries  time series of observable market data values, keyed by ID
   * @param singleValueFailures  details of failures encountered when building market data values
   * @param timeSeriesFailures  details of failures encountered when building time series
   */
  CalculationEnvironmentBuilder(
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, Object> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<MarketDataId<?>, Failure> singleValueFailures,
      Map<MarketDataId<?>, Failure> timeSeriesFailures) {

    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.values.putAll(values);
    this.timeSeries.putAll(timeSeries);
    this.singleValueFailures.putAll(singleValueFailures);
    this.timeSeriesFailures.putAll(timeSeriesFailures);
  }

  /**
   * Adds a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> CalculationEnvironmentBuilder addValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, value);
    singleValueFailures.remove(id);
    return this;
  }

  /**
   * Adds a result for a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param result  a result containing the market data value or details of why it could not be provided
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> CalculationEnvironmentBuilder addResult(MarketDataId<T> id, Result<T> result) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(result, "result");

    if (result.isSuccess()) {
      values.put(id, result.getValue());
      // TODO Check the type of the value is compatible with the market data type of the ID
      singleValueFailures.remove(id);
    } else {
      singleValueFailures.put(id, result.getFailure());
      values.remove(id);
    }
    return this;
  }

  /**
   * Adds a result for a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param result  a result containing the market data value or details of why it could not be provided
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> CalculationEnvironmentBuilder addResultUnsafe(MarketDataId<T> id, Result<?> result) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(result, "result");

    if (result.isSuccess()) {
      Object value = result.getValue();
      // TODO Check the type of the value is compatible with the market data type of the ID
      values.put(id, value);
      singleValueFailures.remove(id);
    } else {
      singleValueFailures.put(id, result.getFailure());
      values.remove(id);
    }
    return this;
  }

  /**
   * Adds results for multiple items of market data, replacing any existing value with the same IDs.
   *
   * @param results  results containing market data values or details of why they could not be provided, keyed by ID
   * @return this builder
   */
  public CalculationEnvironmentBuilder addResults(Map<? extends MarketDataId<?>, ? extends Result<?>> results) {
    ArgChecker.notNull(results, "results");

    for (Map.Entry<? extends MarketDataId<?>, ? extends Result<?>> entry : results.entrySet()) {
      MarketDataId<?> id = entry.getKey();
      Result<?> result = entry.getValue();

      if (result.isSuccess()) {
        values.put(id, result.getValue());
        singleValueFailures.remove(id);
      } else {
        singleValueFailures.put(id, result.getFailure());
        values.remove(id);
      }
    }
    return this;
  }

  /**
   * Adds a single item of market data, replacing any existing value with the same ID.
   * <p>
   * The type of the value is checked to ensure it is compatible with the ID.
   *
   * @param id  the ID of the market data
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  <T> CalculationEnvironmentBuilder addValueUnsafe(MarketDataId<T> id, Object value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, id.getMarketDataType().cast(value));
    return this;
  }

  /**
   * Adds multiple items of market data, replacing any existing values with the same IDs.
   *
   * @param values  the items of market data, keyed by ID
   * @return this builder
   */
  public CalculationEnvironmentBuilder addAllValues(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    this.values.putAll(values);
    return this;
  }

  /**
   * Adds a time series of observable market data values, replacing any existing time series with the same ID.
   *
   * @param id  the ID of the values
   * @param timeSeries  a time series of observable market data values
   * @return this builder
   */
  public CalculationEnvironmentBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(id, timeSeries);
    return this;
  }

  /**
   * Adds a time series of observable market data values, replacing any existing time series with the same ID.
   *
   * @param id  the ID of the values
   * @param result  a time series of observable market data values
   * @return this builder
   */
  public CalculationEnvironmentBuilder addTimeSeriesResult(ObservableId id, Result<LocalDateDoubleTimeSeries> result) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(result, "result");

    if (result.isSuccess()) {
      timeSeries.put(id, result.getValue());
      timeSeriesFailures.remove(id);
    } else {
      timeSeriesFailures.put(id, result.getFailure());
      timeSeries.remove(id);
    }
    return this;
  }

  /**
   * Adds multiple time series of observable market data, replacing any existing time series with the same IDs.
   *
   * @param series  the time series of market data, keyed by ID
   * @return this builder
   */
  public CalculationEnvironmentBuilder addAllTimeSeries(Map<? extends ObservableId, LocalDateDoubleTimeSeries> series) {
    ArgChecker.notNull(series, "series");
    timeSeries.putAll(series);
    return this;
  }

  /**
   * Sets the valuation date associated with the market data, replacing the existing valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return this builder
   */
  public CalculationEnvironmentBuilder valuationDate(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.valuationDate = valuationDate;
    return this;
  }

  /**
   * Builds a set of market data from the data in this builder.
   * <p>
   * It is possible to continue to add more data to a builder after calling {@code build()}. Any
   * {@code BaseMarketData} instances built previously will be unaffected.
   *
   * @return a set of market data from the data in this builder
   */
  public CalculationEnvironment build() {
    return new CalculationEnvironment(valuationDate, values, timeSeries, singleValueFailures, timeSeriesFailures);
  }
}
