/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A single scenario view of multi-scenario market data.
 * <p>
 * This wraps an instance of {@link CalculationMarketData} which contains market data for multiple scenarios.
 * This object returns market data from one of those scenarios. The scenario used as the source of the
 * data is controlled by the {@code scenarioIndex} argument.
 */
public final class SingleCalculationMarketData implements MarketData {

  /** The set of market data for all scenarios. */
  private final CalculationMarketData marketData;

  /** The index of the scenario in {@link #marketData} from which data is returned. */
  private final int scenarioIndex;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an underlying set of market data and scenario index.
   * <p>
   * This provides a single scenario view of the underlying market data.
   *
   * @param marketData  the market data
   * @param scenarioIndex  the index of the scenario to be viewed
   * @return the market data
   * @throws IllegalArgumentException if the scenario index is invalid
   */
  public static SingleCalculationMarketData of(CalculationMarketData marketData, int scenarioIndex) {
    return new SingleCalculationMarketData(marketData, scenarioIndex);
  }

  // restricted constructor
  private SingleCalculationMarketData(CalculationMarketData marketData, int scenarioIndex) {
    ArgChecker.inRange(scenarioIndex, 0, marketData.getScenarioCount(), "scenarioIndex");
    this.marketData = marketData;
    this.scenarioIndex = scenarioIndex;
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate().getValue(scenarioIndex);
  }

  @Override
  public boolean containsValue(MarketDataKey<?> key) {
    return marketData.containsValue(key);
  }

  @Override
  public <T> Optional<T> findValue(MarketDataKey<T> key) {
    Optional<MarketDataBox<T>> optionalBox = marketData.findValue(key);
    return optionalBox.map(box -> box.getValue(scenarioIndex));
  }

  @Override
  public <T> T getValue(MarketDataKey<T> key) {
    return marketData.getValue(key).getValue(scenarioIndex);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    return marketData.getTimeSeries(key);
  }

}
