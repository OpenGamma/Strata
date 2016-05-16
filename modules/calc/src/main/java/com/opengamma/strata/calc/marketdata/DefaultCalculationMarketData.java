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
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.runner.SingleScenarioMarketData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of market data used for a calculation across multiple scenarios.
 * <p>
 * This implementation is backed by a {@link CalculationEnvironment}.
 */
public final class DefaultCalculationMarketData implements CalculationMarketData {

  /**
   * The underlying market data store, accessed by market data ID.
   */
  private final CalculationEnvironment marketData;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an underlying market data environment.
   *
   * @param marketData  the market data
   * @return the calculation market data
   */
  public static DefaultCalculationMarketData of(CalculationEnvironment marketData) {
    return new DefaultCalculationMarketData(marketData);
  }

  // restricted constructor
  private DefaultCalculationMarketData(CalculationEnvironment marketData) {
    this.marketData = ArgChecker.notNull(marketData, "marketData");
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
  public boolean containsValue(MarketDataId<?> id) {
    return marketData.containsValue(id);
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    return marketData.findValue(id);
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    return marketData.getValue(id);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    return marketData.getTimeSeries(id);
  }
}
