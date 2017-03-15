/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;

/**
 * A mutable builder for market data.
 * <p>
 * This is used to create implementations of {@link ImmutableScenarioMarketData}.
 */
public final class ImmutableScenarioMarketDataBuilder {

  /**
   * The number of scenarios.
   */
  private int scenarioCount;
  /**
   * The valuation date associated with each scenario.
   */
  private final MarketDataBox<LocalDate> valuationDate;
  /**
   * The individual items of market data.
   */
  private final Map<MarketDataId<?>, MarketDataBox<?>> values = new HashMap<>();
  /**
   * The time-series of market data values.
   */
  private final Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  //-------------------------------------------------------------------------
  ImmutableScenarioMarketDataBuilder(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.scenarioCount = -1;
    this.valuationDate = MarketDataBox.ofSingleValue(valuationDate);
  }

  ImmutableScenarioMarketDataBuilder(MarketDataBox<LocalDate> valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.scenarioCount = -1;
    this.valuationDate = valuationDate;
  }

  ImmutableScenarioMarketDataBuilder(
      int scenarioCount,
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    ArgChecker.notNegative(scenarioCount, "scenarioCount");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(values, "values");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.scenarioCount = scenarioCount;
    this.valuationDate = valuationDate;
    this.values.putAll(values);
    this.timeSeries.putAll(timeSeries);
  }

  //-------------------------------------------------------------------------
  /**
   * Sets the values in the builder, replacing any existing values.
   *
   * @param values  the values
   * @return this builder
   */
  public ImmutableScenarioMarketDataBuilder values(Map<? extends MarketDataId<?>, ?> values) {
    this.values.clear();
    return addValueMap(values);
  }

  /**
   * Sets the time-series in the builder, replacing any existing values.
   *
   * @param timeSeries  the time-series
   * @return this builder
   */
  public ImmutableScenarioMarketDataBuilder timeSeries(Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {
    this.timeSeries.clear();
    return addTimeSeriesMap(timeSeries);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds market data that is valid for all scenarios.
   * <p>
   * Any existing value with the same identifier will be replaced.
   *
   * @param id  the identifier
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> ImmutableScenarioMarketDataBuilder addValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, MarketDataBox.ofSingleValue(value));
    return this;
  }

  /**
   * Adds market data values that are valid for all scenarios.
   * <p>
   * Each value in the map is a single item of market data used in all scenarios.
   * Any existing value with the same identifier will be replaced.
   *
   * @param values  the items of market data, keyed by identifier
   * @return this builder
   */
  public ImmutableScenarioMarketDataBuilder addValueMap(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    for (Entry<? extends MarketDataId<?>, ?> entry : values.entrySet()) {
      MarketDataId<?> id = entry.getKey();
      Object value = entry.getValue();
      MarketDataBox<?> box = MarketDataBox.ofSingleValue(value);
      checkBoxType(id, box);
      checkAndUpdateScenarioCount(box);
      this.values.put(id, box);
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds market data for each scenario.
   * <p>
   * Any existing value with the same identifier will be replaced.
   *
   * @param id  the identifier
   * @param values  the market data values, one for each scenario
   * @param <T>  the type of the market data values
   * @return this builder
   */
  public <T> ImmutableScenarioMarketDataBuilder addScenarioValue(MarketDataId<T> id, List<? extends T> values) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(values, "values");
    MarketDataBox<? extends T> box = MarketDataBox.ofScenarioValues(values);
    checkAndUpdateScenarioCount(box);
    this.values.put(id, box);
    return this;
  }

  /**
   * Adds market data for each scenario.
   * <p>
   * Any existing value with the same identifier will be replaced.
   *
   * @param id  the identifier
   * @param value  the market data values, one for each scenario
   * @param <T>  the type of the market data values
   * @return this builder
   */
  public <T> ImmutableScenarioMarketDataBuilder addScenarioValue(MarketDataId<T> id, ScenarioArray<? extends T> value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "values");
    MarketDataBox<? extends T> box = MarketDataBox.ofScenarioValue(value);
    checkAndUpdateScenarioCount(box);
    this.values.put(id, box);
    return this;
  }

  /**
   * Adds market data values for each scenario.
   * <p>
   * Each value in the map contains multiple market data items, one for each scenario.
   * Any existing value with the same identifier will be replaced.
   *
   * @param values  the items of market data, keyed by identifier
   * @return this builder
   */
  public ImmutableScenarioMarketDataBuilder addScenarioValueMap(
      Map<? extends MarketDataId<?>, ? extends ScenarioArray<?>> values) {

    ArgChecker.notNull(values, "values");
    for (Entry<? extends MarketDataId<?>, ? extends ScenarioArray<?>> entry : values.entrySet()) {
      MarketDataId<?> id = entry.getKey();
      ScenarioArray<?> value = entry.getValue();
      MarketDataBox<?> box = MarketDataBox.ofScenarioValue(value);
      checkBoxType(id, box);
      checkAndUpdateScenarioCount(box);
      this.values.put(id, box);
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds market data wrapped in a box.
   * <p>
   * Any existing value with the same identifier will be replaced.
   *
   * @param id  the identifier
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> ImmutableScenarioMarketDataBuilder addBox(MarketDataId<T> id, MarketDataBox<? extends T> value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    checkAndUpdateScenarioCount(value);
    values.put(id, value);
    return this;
  }

  /**
   * Adds market data values for each scenario.
   * <p>
   * Each value in the map is a market data box.
   * Any existing value with the same identifier will be replaced.
   *
   * @param values  the items of market data, keyed by identifier
   * @return this builder
   */
  public ImmutableScenarioMarketDataBuilder addBoxMap(
      Map<? extends MarketDataId<?>, ? extends MarketDataBox<?>> values) {

    ArgChecker.notNull(values, "values");
    for (Entry<? extends MarketDataId<?>, ? extends MarketDataBox<?>> entry : values.entrySet()) {
      MarketDataId<?> id = entry.getKey();
      MarketDataBox<?> box = entry.getValue();
      checkBoxType(id, box);
      checkAndUpdateScenarioCount(box);
      this.values.put(id, box);
    }
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
  public ImmutableScenarioMarketDataBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries timeSeries) {
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
  public ImmutableScenarioMarketDataBuilder addTimeSeriesMap(
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeriesMap) {

    ArgChecker.notNull(timeSeriesMap, "timeSeriesMap");
    this.timeSeries.putAll(timeSeriesMap);
    return this;
  }

  //-------------------------------------------------------------------------
  private static void checkBoxType(MarketDataId<?> id, MarketDataBox<?> box) {
    if (!id.getMarketDataType().isAssignableFrom(box.getMarketDataType())) {
      throw new IllegalArgumentException(Messages.format(
          "Market data type {} of value {} is not compatible with the market data type of the identifier {}",
          box.getMarketDataType().getName(),
          box,
          id.getMarketDataType().getName()));
    }
  }

  private void checkAndUpdateScenarioCount(MarketDataBox<?> value) {
    if (value.isScenarioValue()) {
      if (scenarioCount == -1) {
        scenarioCount = value.getScenarioCount();
      } else if (value.getScenarioCount() != scenarioCount) {
        throw new IllegalArgumentException(Messages.format(
            "All values must have the same number of scenarios, expecting {} but received {}",
            scenarioCount,
            value.getScenarioCount()));
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the market data.
   * 
   * @return the market data
   */
  public ImmutableScenarioMarketData build() {
    if (scenarioCount == -1) {
      scenarioCount = 1;
    }
    return new ImmutableScenarioMarketData(scenarioCount, valuationDate, values, timeSeries);
  }

}
