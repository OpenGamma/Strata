/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * A set of scenario market data that contains data for a single scenario.
 * <p>
 * This class is an adapter that wraps an instance of {@link BaseMarketData} and exposes its data via
 * the {@link ScenarioMarketData} interface.
 */
public final class SingleScenarioMarketData implements ScenarioMarketData {

  /** A set of market data for a single scenario. */
  private final BaseMarketData baseData;

  /**
   * @param baseData  a set of market data for a single scenario
   */
  public SingleScenarioMarketData(BaseMarketData baseData) {
    this.baseData = ArgChecker.notNull(baseData, "baseData");
  }

  @Override
  public List<LocalDate> getValuationDates() {
    return ImmutableList.of(baseData.getValuationDate());
  }

  @Override
  public int getScenarioCount() {
    return 1;
  }

  @Override
  public <T, I extends MarketDataId<T>> List<T> getValues(I id) {
    return ImmutableList.of(baseData.getValue(id));
  }

  @Override
  public List<LocalDateDoubleTimeSeries> getTimeSeries(ObservableId id) {
    return ImmutableList.of(baseData.getTimeSeries(id));
  }

  @Override
  public <T, I extends MarketDataId<T>> T getGlobalValue(I id) {
    return baseData.getValue(id);
  }

  @Override
  public List<Observables> getObservables() {
    throw new UnsupportedOperationException("getObservables not implemented");
  }

  @Override
  public List<MarketDataEnvironment> getMarketDataEnvironment() {
    throw new UnsupportedOperationException("getMarketDataEnvironment not implemented");
  }
}
