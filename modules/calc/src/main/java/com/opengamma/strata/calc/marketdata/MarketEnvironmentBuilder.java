/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A mutable builder for building up {@link MarketEnvironment} instances.
 */
public final class MarketEnvironmentBuilder {

  /** The valuation date associated with the market data. */
  private MarketDataBox<LocalDate> valuationDate = MarketDataBox.empty();

  /** The number of scenarios for which this builder contains market data. */
  private int scenarioCount;

  /** The single value market data items, keyed by ID. */
  private final Map<MarketDataId<?>, MarketDataBox<?>> values = new HashMap<>();

  /** Time series of observable market data values, keyed by ID. */
  private final Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  /** Details of failures when building single market data values. */
  private final Map<MarketDataId<?>, Failure> valueFailures = new HashMap<>();

  /** Details of failures when building time series of market data values. */
  private final Map<MarketDataId<?>, Failure> timeSeriesFailures = new HashMap<>();

  /**
   * Creates an empty builder.
   */
  MarketEnvironmentBuilder() {
  }

  /**
   * Creates a builder pre-populated with data.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param scenarioCount  the number of scenarios for which this builder contains market data
   * @param values  the single value market data items, keyed by ID
   * @param timeSeries  time series of observable market data values, keyed by ID
   * @param valueFailures  details of failures encountered when building market data values
   * @param timeSeriesFailures  details of failures encountered when building time series
   */
  MarketEnvironmentBuilder(
      MarketDataBox<LocalDate> valuationDate,
      int scenarioCount,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<MarketDataId<?>, Failure> valueFailures,
      Map<MarketDataId<?>, Failure> timeSeriesFailures) {

    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.scenarioCount = scenarioCount;
    this.values.putAll(values);
    this.timeSeries.putAll(timeSeries);
    this.valueFailures.putAll(valueFailures);
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
  public <T> MarketEnvironmentBuilder addValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, MarketDataBox.ofSingleValue(value));
    return this;
  }

  /**
   * Adds a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> MarketEnvironmentBuilder addValue(MarketDataId<T> id, MarketDataBox<T> value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    updateScenarioCount(value);
    values.put(id, value);
    valueFailures.remove(id);
    return this;
  }

  /**
   * Adds multiple values for an item of market data, one for each scenario.
   *
   * @param id  the ID of the market data
   * @param values  the market data values, one for each scenario
   * @param <T>  the type of the market data values
   * @return this builder
   */
  public <T> MarketEnvironmentBuilder addValue(MarketDataId<T> id, List<T> values) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(values, "values");
    MarketDataBox<T> box = MarketDataBox.ofScenarioValues(values);
    updateScenarioCount(box);
    this.values.put(id, box);
    valueFailures.remove(id);
    return this;
  }

  /**
   * Adds a single item of market data, replacing any existing value with the same ID.
   * <p>
   * The type of the value is checked to ensure it is compatible with the ID.
   *
   * @param id  the ID of the market data
   * @param value  the market data value
   * @return this builder
   */
  MarketEnvironmentBuilder addValueUnsafe(MarketDataId<?> id, MarketDataBox<?> value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    updateScenarioCount(value);
    checkType(id, value);
    values.put(id, value);
    return this;
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Adds multiple items of market data, replacing any existing values with the same IDs.
   *
   * @param values  the items of market data, keyed by ID
   * @return this builder
   */
  public MarketEnvironmentBuilder addValues(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    Map<? extends MarketDataId<?>, MarketDataBox<Object>> boxedValues = values.entrySet().stream()
        .map(MarketEnvironmentBuilder::checkTypes)
        .collect(toMap(e -> e.getKey(), e -> MarketDataBox.ofSingleValue(e.getValue())));
    this.values.putAll(boxedValues);
    return this;
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Adds a result for a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param result  a result containing the market data value or details of why it could not be provided
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> MarketEnvironmentBuilder addResult(MarketDataId<T> id, Result<MarketDataBox<T>> result) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(result, "result");

    if (result.isSuccess()) {
      MarketDataBox<T> box = result.getValue();
      updateScenarioCount(box);
      values.put(id, box);
      valueFailures.remove(id);
    } else {
      valueFailures.put(id, result.getFailure());
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
  public <T> MarketEnvironmentBuilder addResultUnsafe(MarketDataId<T> id, Result<MarketDataBox<?>> result) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(result, "result");

    if (result.isSuccess()) {
      MarketDataBox<?> box = result.getValue();
      checkType(id, box);
      updateScenarioCount(box);
      values.put(id, box);
      valueFailures.remove(id);
    } else {
      valueFailures.put(id, result.getFailure());
      values.remove(id);
    }
    return this;
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Adds a time series of observable market data values, replacing any existing time series with the same ID.
   *
   * @param id  the ID of the values
   * @param timeSeries  a time series of observable market data values
   * @return this builder
   */
  public MarketEnvironmentBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(id, timeSeries);
    return this;
  }

  /**
   * Adds multiple time series of observable market data, replacing any existing time series with the same IDs.
   *
   * @param series  the time series of market data, keyed by ID
   * @return this builder
   */
  public MarketEnvironmentBuilder addTimeSeries(Map<? extends ObservableId, LocalDateDoubleTimeSeries> series) {
    ArgChecker.notNull(series, "series");
    timeSeries.putAll(series);
    return this;
  }

  /**
   * Adds a time series of observable market data values, replacing any existing time series with the same ID.
   *
   * @param id  the ID of the values
   * @param result  a time series of observable market data values
   * @return this builder
   */
  public MarketEnvironmentBuilder addTimeSeriesResult(ObservableId id, Result<LocalDateDoubleTimeSeries> result) {
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
   * Sets the valuation date associated with the market data, replacing the existing valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return this builder
   */
  public MarketEnvironmentBuilder valuationDate(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.valuationDate = MarketDataBox.ofSingleValue(valuationDate);
    updateScenarioCount(this.valuationDate);
    return this;
  }

  /**
   * Sets the valuation date associated with the market data, replacing the existing valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return this builder
   */
  public MarketEnvironmentBuilder valuationDate(MarketDataBox<LocalDate> valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");

    if (valuationDate.getScenarioCount() == 0) {
      throw new IllegalArgumentException("Valuation date must not be empty");
    }
    updateScenarioCount(valuationDate);
    this.valuationDate = valuationDate;
    return this;
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Sets the market data values in this builder, replacing the existing set.
   *
   * @param values  the market data values
   * @return this builder
   */
  public MarketEnvironmentBuilder values(Map<? extends MarketDataId<?>, MarketDataBox<?>> values) {
    ArgChecker.notNull(values, "values");
    this.values.clear();
    this.values.putAll(values);
    return this;
  }

  /**
   * Sets the time series in this builder, replacing the existing set.
   *
   * @param timeSeries  the time series
   * @return this builder
   */
  public MarketEnvironmentBuilder timeSeries(Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.clear();
    this.timeSeries.putAll(timeSeries);
    return this;
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Builds a set of market data from the data in this builder.
   * <p>
   * It is possible to continue to add more data to a builder after calling {@code build()}. Any
   * {@code BaseMarketData} instances built previously will be unaffected.
   *
   * @return a set of market data from the data in this builder
   */
  public MarketEnvironment build() {
    if (valuationDate.getScenarioCount() == 0) {
      // This isn't checked in MarketEnvironment otherwise it would be impossible to have an empty environment
      throw new IllegalArgumentException("Valuation date must be specified");
    }
    return new MarketEnvironment(valuationDate, scenarioCount, values, timeSeries, valueFailures, timeSeriesFailures);
  }

  private static Map.Entry<? extends MarketDataId<?>, ?> checkTypes(Map.Entry<? extends MarketDataId<?>, ?> entry) {
    if (!entry.getKey().getMarketDataType().isInstance(entry.getValue())) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data value {} does not match the type of the key {}",
              entry.getValue(),
              entry.getKey()));
    }
    return entry;
  }

  private static void checkType(MarketDataId<?> id, MarketDataBox<?> box) {
    if (!id.getMarketDataType().isAssignableFrom(box.getMarketDataType())) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data type {} of value {} is not compatible with the market data type of the ID {}",
              box.getMarketDataType().getName(),
              box,
              id.getMarketDataType().getName()));
    }
  }

  private void updateScenarioCount(MarketDataBox<?> box) {
    // If the box has a single value then it can be used with any number of scenarios - the same value is used
    // for all scenarios.
    if (box.isSingleValue()) {
      if (scenarioCount == 0) {
        scenarioCount = 1;
      }
      return;
    }
    int scenarioCount = box.getScenarioCount();

    if (this.scenarioCount == 0 || this.scenarioCount == 1) {
      this.scenarioCount = scenarioCount;
      return;
    }
    if (scenarioCount != this.scenarioCount) {
      throw new IllegalArgumentException(
          Messages.format(
              "Cannot add value {} with {} scenarios to an environment with {} scenarios",
              box,
              scenarioCount,
              this.scenarioCount));
    }
  }
}
