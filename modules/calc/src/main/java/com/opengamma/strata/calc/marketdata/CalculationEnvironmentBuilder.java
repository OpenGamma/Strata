/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

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
 * A mutable builder for building up {@link CalculationMarketDataMap} instances.
 */
public final class CalculationEnvironmentBuilder {

  /**
   * The number of scenarios in the data in the environment. If the market data boxes have a single value they
   * can be used with any number of scenarios (the same value is used in every scenario). Therefore this field
   * is not updated when single value boxes are added.
   * <p>
   * It is set when the first multi-value box is added and each time another multi-value box is added the number
   * of scenarios must match the existing count.
   */
  private int scenarioCount;

  /** The valuation date associated with the market data. */
  private MarketDataBox<LocalDate> valuationDate = MarketDataBox.empty();

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
  CalculationEnvironmentBuilder() {
  }

  /**
   * Creates a builder pre-populated with data.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param values  the single value market data items, keyed by ID
   * @param timeSeries  time series of observable market data values, keyed by ID
   * @param valueFailures  details of failures encountered when building market data values
   * @param timeSeriesFailures  details of failures encountered when building time series
   */
  CalculationEnvironmentBuilder(
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries,
      Map<MarketDataId<?>, Failure> valueFailures,
      Map<MarketDataId<?>, Failure> timeSeriesFailures) {

    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
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
  public <T> CalculationEnvironmentBuilder addValue(MarketDataId<T> id, MarketDataBox<T> value) {
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
  public <T> CalculationEnvironmentBuilder addValue(MarketDataId<T> id, List<T> values) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(values, "values");
    MarketDataBox<T> box = MarketDataBox.ofScenarioValues(values);
    updateScenarioCount(box);
    this.values.put(id, box);
    valueFailures.remove(id);
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
  public <T> CalculationEnvironmentBuilder addResult(MarketDataId<T> id, Result<MarketDataBox<T>> result) {
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
  public <T> CalculationEnvironmentBuilder addResultUnsafe(MarketDataId<T> id, Result<MarketDataBox<?>> result) {
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

  /**
   * Adds a single item of market data, replacing any existing value with the same ID.
   * <p>
   * The type of the value is checked to ensure it is compatible with the ID.
   *
   * @param id  the ID of the market data
   * @param value  the market data value
   * @return this builder
   */
  CalculationEnvironmentBuilder addValueUnsafe(MarketDataId<?> id, MarketDataBox<?> value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    updateScenarioCount(value);
    checkType(id, value);
    values.put(id, value);
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
   * Sets the valuation date associated with the market data, replacing the existing valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return this builder
   */
  public CalculationEnvironmentBuilder valuationDate(LocalDate valuationDate) {
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
  public CalculationEnvironmentBuilder valuationDate(MarketDataBox<LocalDate> valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    updateScenarioCount(valuationDate);

    if (valuationDate.getScenarioCount() == 0) {
      throw new IllegalArgumentException("Valuation date must not be empty");
    }
    this.valuationDate = valuationDate;
    return this;
  }

  /**
   * Builds a set of market data from the data in this builder.
   * <p>
   * It is possible to continue to add more data to a builder after calling {@code build()}. Any
   * {@code CalculationEnvironment} instances built previously will be unaffected.
   *
   * @return a set of market data from the data in this builder
   */
  public CalculationEnvironment build() {
    return new CalculationMarketDataMap(valuationDate, scenarioCount, values, timeSeries, valueFailures, timeSeriesFailures);
  }

  /**
   * Builds a set of market data from the data in this builder.
   * <p>
   * It is possible to continue to add more data to a builder after calling {@code build()}. Any
   * {@code CalculationMarketDataMap} instances built previously will be unaffected.
   *
   * @return a set of market data from the data in this builder
   */
  CalculationMarketDataMap buildMarketDataMap() {
    return new CalculationMarketDataMap(valuationDate, scenarioCount, values, timeSeries, valueFailures, timeSeriesFailures);
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
}
