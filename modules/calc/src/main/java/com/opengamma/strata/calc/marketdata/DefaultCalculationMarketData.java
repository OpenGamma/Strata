/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of market data used for a calculation across multiple scenarios.
 */
public final class DefaultCalculationMarketData implements CalculationMarketData {

  /** The market data, keyed by market data ID. */
  private final CalculationEnvironment marketData;

  /** Mappings to convert from the market data keys passed to the methods to IDs used for looking up the market data. */
  private final MarketDataMappings marketDataMappings;

  /**
   * Creates a new set of market data.
   *
   * @param marketData  the market data
   * @param marketDataMappings  mappings to convert from the market data keys passed to the methods to IDs used
   *   for looking up the market data
   */
  public DefaultCalculationMarketData(CalculationEnvironment marketData, MarketDataMappings marketDataMappings) {
    this.marketData = ArgChecker.notNull(marketData, "marketData");
    this.marketDataMappings = ArgChecker.notNull(marketDataMappings, "marketDataMappings");
  }

  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return marketData.getValuationDate();
  }

  @Override
  public int getScenarioCount() {
    return marketData.getScenarioCount();
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataKey<T> key) {
    MarketDataId<T> id = marketDataMappings.getIdForKey(key);
    return marketData.getValue(id);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    ObservableId id = marketDataMappings.getIdForObservableKey(key);
    return marketData.getTimeSeries(id);
  }
}
