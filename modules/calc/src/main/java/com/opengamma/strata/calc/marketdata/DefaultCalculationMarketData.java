/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of market data used for a calculation across multiple scenarios.
 * <p>
 * This implementation is backed by a {@link CalculationEnvironment} and {@link MarketDataMappings}.
 * Methods on this interface take a {@linkplain MarketDataKey key}.
 * The mappings are used to resolve the key into the {@linkplain MarketDataId ID}
 * necessary to query {@code CalculationEnvironment}.
 */
public final class DefaultCalculationMarketData implements CalculationMarketData {

  /**
   * The underlying market data store, accessed by market data ID.
   */
  private final CalculationEnvironment marketData;
  /**
   * The mappings used to convert from market data keys to IDs
   * The methods on this interface take keys, which the mappings convert to IDs
   * to look up the market data in the environment.
   */
  private final MarketDataMappings marketDataMappings;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an underlying market data environment and mappings.
   * <p>
   * Methods on this interface take a {@linkplain MarketDataKey key}.
   * The mappings are used to resolve the key into the {@linkplain MarketDataId ID}
   * necessary to query {@code CalculationEnvironment}.
   *
   * @param marketData  the market data
   * @param marketDataMappings  the mappings
   * @return the calculation market data
   */
  public static DefaultCalculationMarketData of(CalculationEnvironment marketData, MarketDataMappings marketDataMappings) {
    return new DefaultCalculationMarketData(marketData, marketDataMappings);
  }

  // restricted constructor
  private DefaultCalculationMarketData(CalculationEnvironment marketData, MarketDataMappings marketDataMappings) {
    this.marketData = ArgChecker.notNull(marketData, "marketData");
    this.marketDataMappings = ArgChecker.notNull(marketDataMappings, "marketDataMappings");
  }

  //-------------------------------------------------------------------------
  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return marketData.getValuationDate();
  }

  @Override
  public int getScenarioCount() {
    return marketData.getScenarioCount();
  }

  @Override
  public Stream<MarketData> scenarios() {
    return IntStream.range(0, getScenarioCount())
        .mapToObj(scenarioIndex -> SingleCalculationMarketData.of(this, scenarioIndex));
  }

  @Override
  public MarketData scenario(int scenarioIndex) {
    return SingleCalculationMarketData.of(this, scenarioIndex);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsValue(MarketDataKey<?> key) {
    MarketDataId<?> id = marketDataMappings.getIdForKey(key);
    return marketData.containsValue(id);
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataKey<T> key) {
    return marketDataMappings.getValue(key, marketData);
  }

  @Override
  public boolean containsTimeSeries(ObservableKey key) {
    ObservableId id = marketDataMappings.getIdForObservableKey(key);
    return marketData.containsTimeSeries(id);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    return marketDataMappings.getTimeSeries(key, marketData);
  }
}
