/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.List;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

/**
 * A source of market data used for a calculation across multiple scenarios.
 */
public final class LocalScenarioCalculationMarketData implements CalculationMarketData {

  /** The market data, keyed by market data ID. */
  private final ScenarioCalculationEnvironment baseMarketData;

  /** The market data, keyed by market data ID. */
  private final ScenarioCalculationEnvironment scenarioMarketData;

  /** Mappings to convert from the market data keys passed to the methods to IDs used for looking up the market data. */
  private final MarketDataMappings marketDataMappings;

  /**
   * Creates a new set of market data.
   *
   * @param baseMarketData  the market data
   * @param scenarioMarketData
   * @param marketDataMappings  mappings to convert from the market data keys passed to the methods to IDs used
   *   for looking up the market data
   */
  public LocalScenarioCalculationMarketData(
      ScenarioCalculationEnvironment baseMarketData,
      ScenarioCalculationEnvironment scenarioMarketData,
      MarketDataMappings marketDataMappings) {

    this.baseMarketData = ArgChecker.notNull(baseMarketData, "baseMarketData");
    this.scenarioMarketData = ArgChecker.notNull(scenarioMarketData, "scenarioMarketData");
    this.marketDataMappings = ArgChecker.notNull(marketDataMappings, "marketDataMappings");
  }

  @Override
  public List<LocalDate> getValuationDates() {
    return baseMarketData.getValuationDates();
  }

  @Override
  public int getScenarioCount() {
    return baseMarketData.getScenarioCount();
  }

  @Override
  public <T> List<T> getValues(MarketDataKey<T> key) {
    MarketDataId<T> id = marketDataMappings.getIdForKey(key);

    if (scenarioMarketData.containsValues(id)) {
      return scenarioMarketData.getValues(id);
    } else {
      return baseMarketData.getValues(id);
    }
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    ObservableId id = marketDataMappings.getIdForObservableKey(key);

    if (scenarioMarketData.containsTimeSeries(id)) {
      return scenarioMarketData.getTimeSeries(id);
    } else {
      return baseMarketData.getTimeSeries(id);
    }
  }

  @Override
  public <T, K extends MarketDataKey<T>> T getGlobalValue(K key) {
    MarketDataId<T> id = marketDataMappings.getIdForKey(key);

    if (scenarioMarketData.containsGlobalValue(id)) {
      return scenarioMarketData.getGlobalValue(id);
    } else {
      return baseMarketData.getGlobalValue(id);
    }
  }

  @Override
  public CalculationMarketData getScenarioData(int scenarioIndex) {
    throw new UnsupportedOperationException("getScenarioData is not supported on scenario data");
  }
}
