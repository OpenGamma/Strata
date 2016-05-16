/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.runner.SingleScenarioMarketData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of market data used for a calculation across multiple scenarios.
 * <p>
 * This implementation is backed by a {@link CalculationEnvironment} and {@link MarketDataFeed}.
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
   * The source of market data.
   */
  private final MarketDataFeed feed;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an underlying market data environment and mappings.
   * <p>
   * Methods on this interface take a {@linkplain MarketDataKey key}.
   * The mappings are used to resolve the key into the {@linkplain MarketDataId ID}
   * necessary to query {@code CalculationEnvironment}.
   *
   * @param marketData  the market data
   * @return the calculation market data
   */
  public static DefaultCalculationMarketData of(CalculationEnvironment marketData) {
    return new DefaultCalculationMarketData(marketData, MarketDataFeed.NONE);
  }

  /**
   * Obtains an instance from an underlying market data environment and mappings.
   * <p>
   * Methods on this interface take a {@linkplain MarketDataKey key}.
   * The mappings are used to resolve the key into the {@linkplain MarketDataId ID}
   * necessary to query {@code CalculationEnvironment}.
   *
   * @param marketData  the market data
   * @param feed  the source of market data
   * @return the calculation market data
   */
  public static DefaultCalculationMarketData of(CalculationEnvironment marketData, MarketDataFeed feed) {
    return new DefaultCalculationMarketData(marketData, feed);
  }

  // restricted constructor
  private DefaultCalculationMarketData(CalculationEnvironment marketData, MarketDataFeed feed) {
    this.marketData = ArgChecker.notNull(marketData, "marketData");
    this.feed = ArgChecker.notNull(feed, "feed");
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
        .mapToObj(scenarioIndex -> SingleScenarioMarketData.of(this, scenarioIndex));
  }

  @Override
  public MarketData scenario(int scenarioIndex) {
    return SingleScenarioMarketData.of(this, scenarioIndex);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsValue(MarketDataKey<?> key) {
    return marketData.containsValue(key.toMarketDataId(feed));
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataKey<T> key) {
    return marketData.findValue(key.toMarketDataId(feed));
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataKey<T> key) {
    return marketData.getValue(key.toMarketDataId(feed));
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
    return marketData.getTimeSeries(key.toMarketDataId(feed));
  }
}
