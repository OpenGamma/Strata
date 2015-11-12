/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * The default implementation of {@link SingleCalculationMarketData}.
 * <p>
 * This wraps an instance of {@link CalculationMarketData} which contains market data for multiple scenarios.
 * This object returns market data from one of those scenarios. The scenario used as the source of the
 * data is controlled by the {@code scenarioIndex} argument.
 */
public final class DefaultSingleCalculationMarketData implements SingleCalculationMarketData {

  /** The set of market data for all scenarios. */
  private final CalculationMarketData marketData;

  /** The index of the scenario in {@link #marketData} from which data is returned. */
  private final int scenarioIndex;

  /**
   * Creates a set of market data that uses the data of the scenario at the specified index.
   *
   * @param marketData  market data for multiple scenarios
   * @param scenarioIndex  the index of the scenario whose data is returned by the new slice
   */
  public DefaultSingleCalculationMarketData(CalculationMarketData marketData, int scenarioIndex) {
    this.marketData = marketData;
    this.scenarioIndex = scenarioIndex;
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    return marketData.getTimeSeries(key);
  }

  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate().getValue(scenarioIndex);
  }

  @Override
  public <T> T getValue(MarketDataKey<T> key) {
    return marketData.getValue(key).getValue(scenarioIndex);
  }
}
