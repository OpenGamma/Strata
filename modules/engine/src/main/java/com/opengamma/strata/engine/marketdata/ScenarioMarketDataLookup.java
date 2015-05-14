/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * For looking up market data from one scenario in a set of scenario data.
 * <p>
 * This class contains a set of market data for multiple scenarios and the index of one scenario. All
 * data is taken from the scenario at the specified index.
 */
class ScenarioMarketDataLookup implements MarketDataLookup {

  /** The market data for a set of scenarios. */
  private final ScenarioMarketData marketData;

  /** The index of a single scenario. */
  private final int scenarioNumber;

  /**
   * Creates a new instance that delegates to the specified scenario in the set of scenario market data.
   *
   * @param marketData  a set of market data for multiple scenarios
   * @param scenarioNumber  the index of the scenario in which data should be looked up
   */
  ScenarioMarketDataLookup(ScenarioMarketData marketData, int scenarioNumber) {
    this.marketData = marketData;
    this.scenarioNumber = scenarioNumber;
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    return marketData.containsValues(id);
  }

  @Override
  public <T, I extends MarketDataId<T>> T getValue(I id) {
    return marketData.getValues(id).get(scenarioNumber);
  }

  @Override
  public boolean containsTimeSeries(ObservableId id) {
    return marketData.containsTimeSeries(id);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    return marketData.getTimeSeries(id);
  }

  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDates().get(scenarioNumber);
  }
}
