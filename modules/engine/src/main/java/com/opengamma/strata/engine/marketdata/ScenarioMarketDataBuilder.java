/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * A mutable builder for building an instance of {@link ScenarioMarketData}.
 */
public final class ScenarioMarketDataBuilder {

  /** The number of scenarios. */
  private final int scenarioCount;

  /** The valuation dates of the scenarios, one for each scenario. */
  private final List<LocalDate> valuationDates = new ArrayList<>();

  /**
   * The market data values for the scenarios, keyed by the ID of the market data.
   * The number of values for each key is the same as the number of scenarios.
   */
  private final ListMultimap<MarketDataId<?>, Object> values = ArrayListMultimap.create();

  /**
   * The time series of market data for the scenarios, keyed by the ID of the market data.
   * The number of values for each key is the same as the number of scenarios.
   */
  private final ListMultimap<ObservableId, LocalDateDoubleTimeSeries> timeSeries = ArrayListMultimap.create();

  /** The global market data values that are applicable to all scenarios. */
  private final Map<MarketDataId<?>, Object> globalValues = new HashMap<>();

  /**
   * @param scenarioCount  the number of scenarios
   */
  ScenarioMarketDataBuilder(int scenarioCount) {
    this.scenarioCount = ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
  }

  /**
   * Returns a new builder where every scenario has the same valuation date.
   *
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation date for all scenarios
   */
  ScenarioMarketDataBuilder(int scenarioCount, LocalDate valuationDate) {
    this.scenarioCount = ArgChecker.notNegativeOrZero(scenarioCount, "scenarioCount");
    valuationDate(valuationDate);
  }

  /**
   * Sets the valuation date for all scenarios.
   *
   * @param valuationDate  the valuation date for all scenarios
   * @return this builder
   */
  public ScenarioMarketDataBuilder valuationDate(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    valuationDates.addAll(Collections.nCopies(scenarioCount, valuationDate));
    return this;
  }

  /**
   * Sets the valuation date for all scenarios. The number of date arguments must be the same as
   * the number of scenarios.
   *
   * @param valuationDates  the valuation dates for the scenarios, one for each scenario
   * @return this builder
   */
  public ScenarioMarketDataBuilder valuationDates(LocalDate... valuationDates) {
    ArgChecker.notNull(valuationDates, "valuationDates");
    checkLength(valuationDates.length, "valuation dates");
    this.valuationDates.clear();
    this.valuationDates.addAll(Arrays.asList(valuationDates));
    return this;
  }

  /**
   * Sets the valuation date for all scenarios. The number of date arguments must be the same as
   * the number of scenarios.
   *
   * @param valuationDates  the valuation dates for the scenarios, one for each scenario
   * @return this builder
   */
  public ScenarioMarketDataBuilder valuationDates(List<LocalDate> valuationDates) {
    ArgChecker.notNull(valuationDates, "valuationDates");
    checkLength(valuationDates.size(), "valuation dates");
    this.valuationDates.clear();
    this.valuationDates.addAll(valuationDates);
    return this;
  }

  /**
   * Adds market data values for all scenarios. The number of values must be the same as the number
   * of scenarios.
   *
   * @param id  the ID of the market data values
   * @param values  the market data values, one for each scenario
   * @param <T>  the type of the market data values
   * @return this builder
   */
  @SafeVarargs
  public final <T> ScenarioMarketDataBuilder addValues(MarketDataId<T> id, T... values) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(values, "values");
    checkLength(values.length, "values");
    this.values.putAll(id, Arrays.asList(values));
    return this;
  }

  /**
   * Adds market data values for all scenarios. The number of values must be the same as the number
   * of scenarios.
   *
   * @param id  the ID of the market data values
   * @param values  the market data values, one for each scenario
   * @param <T>  the type of the market data values
   * @return this builder
   */
  public <T> ScenarioMarketDataBuilder addValues(MarketDataId<T> id, List<T> values) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(values, "values");
    checkLength(values.size(), "values");
    this.values.putAll(id, values);
    return this;
  }

  /**
   * Adds time series of market data values for all scenarios. The number of time series must be the same as
   * the number of scenarios.
   *
   * @param id  the ID of the market data values
   * @param timeSeries  the time series of market data values, one for each scenario
   * @return this builder
   */
  public ScenarioMarketDataBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries... timeSeries) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(timeSeries, "timeSeries");
    checkLength(timeSeries.length, "time series");
    this.timeSeries.putAll(id, Arrays.asList(timeSeries));
    return this;
  }

  /**
   * Adds time series of market data values for all scenarios. The number of time series must be the same as
   * the number of scenarios.
   *
   * @param id  the ID of the market data values
   * @param timeSeries  the time series of market data values, one for each scenario
   * @return this builder
   */
  public ScenarioMarketDataBuilder addTimeSeries(ObservableId id, List<LocalDateDoubleTimeSeries> timeSeries) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(timeSeries, "timeSeries");
    checkLength(timeSeries.size(), "time series");
    this.timeSeries.putAll(id, timeSeries);
    return this;
  }

  /**
   * Adds a global value that is applicable to all scenarios.
   *
   * @param id  the identifier to associate the value with
   * @param value  the value to add
   * @return this builder
   */
  public <T> ScenarioMarketDataBuilder addGlobalValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    globalValues.put(id, value);
    return this;
  }

  /**
   * Returns a set of scenario market data built from the data in this builder.
   *
   * @return a set of scenario market data built from the data in this builder
   */
  public ScenarioMarketData build() {
    return DefaultScenarioMarketData.builder()
        .scenarioCount(scenarioCount)
        .valuationDates(valuationDates)
        .values(values)
        .timeSeries(timeSeries)
        .globalValues(globalValues)
        .build();
  }

  private void checkLength(int length, String itemName) {
    if (length != scenarioCount) {
      throw new IllegalArgumentException(
          Messages.format(
              "The number of {} ({}) must be the same as the number of scenarios ({})",
              itemName,
              length,
              scenarioCount));
    }
  }
}
