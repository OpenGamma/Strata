/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketDataBuilder;
import com.opengamma.strata.data.scenario.MarketDataBox;

/**
 * A mutable builder for building up {@link BuiltScenarioMarketData} instances.
 */
final class BuiltScenarioMarketDataBuilder {

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
   * Creates a builder pre-populated with the valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   */
  BuiltScenarioMarketDataBuilder(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.valuationDate = MarketDataBox.ofSingleValue(valuationDate);
    updateScenarioCount(this.valuationDate);
  }

  /**
   * Creates a builder pre-populated with the valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   */
  BuiltScenarioMarketDataBuilder(MarketDataBox<LocalDate> valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");

    if (valuationDate.getScenarioCount() == 0) {
      throw new IllegalArgumentException("Valuation date must not be empty");
    }
    updateScenarioCount(valuationDate);
    this.valuationDate = valuationDate;
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
  BuiltScenarioMarketDataBuilder(
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
  <T> BuiltScenarioMarketDataBuilder addValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, MarketDataBox.ofSingleValue(value));
    return this;
  }

  /**
   * Adds a single market data box, replacing any existing box with the same ID.
   * <p>
   * The type of the box is checked to ensure it is compatible with the ID.
   *
   * @param id  the ID of the market data
   * @param box  the market data box
   * @return this builder
   */
  BuiltScenarioMarketDataBuilder addBox(MarketDataId<?> id, MarketDataBox<?> box) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(box, "box");
    updateScenarioCount(box);
    checkBoxType(id, box);
    values.put(id, box);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a result for a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param result  a result containing the market data value or details of why it could not be provided
   * @param <T>  the type of the market data value
   * @return this builder
   */
  <T> BuiltScenarioMarketDataBuilder addResult(MarketDataId<T> id, Result<MarketDataBox<?>> result) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(result, "result");

    if (result.isSuccess()) {
      MarketDataBox<?> box = result.getValue();
      checkBoxType(id, box);
      updateScenarioCount(box);
      values.put(id, box);
      valueFailures.remove(id);
    } else {
      valueFailures.put(id, result.getFailure());
      values.remove(id);
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a time series of observable market data values, replacing any existing time series with the same ID.
   *
   * @param id  the ID of the values
   * @param timeSeries  a time series of observable market data values
   * @return this builder
   */
  BuiltScenarioMarketDataBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries timeSeries) {
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
  BuiltScenarioMarketDataBuilder addTimeSeriesResult(ObservableId id, Result<LocalDateDoubleTimeSeries> result) {
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

  //-------------------------------------------------------------------------
  /**
   * Builds a set of market data from the data in this builder.
   * <p>
   * It is possible to continue to add more data to a builder after calling {@code build()}.
   *
   * @return a set of market data from the data in this builder
   */
  BuiltScenarioMarketData build() {
    if (valuationDate.getScenarioCount() == 0) {
      // This isn't checked in the main class otherwise it would be impossible to have an empty instance
      throw new IllegalArgumentException("Valuation date must be specified");
    }
    ImmutableScenarioMarketDataBuilder builder = ImmutableScenarioMarketData.builder(valuationDate)
        .addBoxMap(values)
        .addTimeSeriesMap(timeSeries);
    return new BuiltScenarioMarketData(builder.build(), valueFailures, timeSeriesFailures);
  }

  //-------------------------------------------------------------------------
  private static void checkBoxType(MarketDataId<?> id, MarketDataBox<?> box) {
    if (!id.getMarketDataType().isAssignableFrom(box.getMarketDataType())) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data type {} of value {} is not compatible with the market data type of the identifier {}",
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
      throw new IllegalArgumentException(Messages.format(
          "All values must have the same number of scenarios, expecting {} but received {}",
          this.scenarioCount,
          scenarioCount));
    }
  }
}
