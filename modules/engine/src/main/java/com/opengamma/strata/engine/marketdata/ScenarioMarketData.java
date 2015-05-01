/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.List;

import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * A source of market data used for performing calculations across a set of scenarios.
 */
public interface ScenarioMarketData {

  /**
   * Returns a mutable builder for building a set of scenario market data.
   *
   * @param scenarioCount  the number of scenarios
   * @return a mutable builder for building a set of scenario market data
   */
  public static ScenarioMarketDataBuilder builder(int scenarioCount) {
    return new ScenarioMarketDataBuilder(scenarioCount);
  }

  /**
   * Returns a mutable builder for building a set of scenario market data where every scenario has the
   * same valuation date.
   *
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation date of all scenarios
   * @return a mutable builder for building a set of scenario market data
   */
  public static ScenarioMarketDataBuilder builder(int scenarioCount, LocalDate valuationDate) {
    return new ScenarioMarketDataBuilder(scenarioCount, valuationDate);
  }

  /**
   * Returns the valuation dates of the scenarios, one for each scenario.
   *
   * @return the valuation dates of the scenarios, one for each scenario
   */
  public abstract List<LocalDate> getValuationDates();

  /**
   * Returns the number of scenarios.
   *
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Returns a list of market data values, one from each scenario.
   * <p>
   * The date of the market data is the same as the valuation date of the scenario.
   *
   * @param id  ID of the market data
   * @param <T>  type of the market data
   * @param <I>  type of the market data ID
   * @return a list of market data values, one from each scenario
   */
  public abstract <T, I extends MarketDataId<T>> List<T> getValues(I id);

  /**
   * Returns a list of market data time series, one from each scenario.
   *
   * @param id  ID of the market data
   * @return a list of market data time series, one from each scenario
   */
  public abstract List<LocalDateDoubleTimeSeries> getTimeSeries(ObservableId id);

  /**
   * Returns a single value that is valid for all scenarios.
   * <p>
   * This allows optimizations such as pre-processing of items market data to create a single composite
   * value that can be processed more efficiently.
   *
   * @param id  ID of the market data
   * @param <T>  type of the market data
   * @param <I>  type of the market data ID
   * @return the market data value
   */
  public abstract <T, I extends MarketDataId<T>> T getGlobalValue(I id);

  /**
   * Returns the observable market data in this set of data.
   *
   * @return the observable market data in this set of data
   */
  public abstract List<Observables> getObservables();

  /**
   * Returns a market data environment containing the calibrated market data in this data set.
   *
   * @return a market data environment containing the calibrated market data in this data set
   */
  public abstract List<MarketDataEnvironment> getMarketDataEnvironment();
}
