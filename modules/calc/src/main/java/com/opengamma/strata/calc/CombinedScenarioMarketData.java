/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A set of market data which combines two underlying sets of data.
 * <p>
 * If the same item of data is available in both sets, it will be taken from the first.
 * <p>
 * The underlying sets must contain the same number of scenarios, or one of them must have one scenario.
 * If one of the underlying sets of data has one scenario the combined set will have the scenario count
 * of the other set.
 */
class CombinedScenarioMarketData implements ScenarioMarketData {

  /** The first underlying set of market data. */
  private final ScenarioMarketData underlying1;

  /** The second underlying set of market data. */
  private final ScenarioMarketData underlying2;

  /** The number of scenarios for which market data is available. */
  private final int scenarioCount;

  /**
   * Creates a new instance.
   *
   * @param underlying1  the first underlying set of market data
   * @param underlying2  the second underlying set of market data
   */
  CombinedScenarioMarketData(ScenarioMarketData underlying1, ScenarioMarketData underlying2) {
    this.underlying1 = underlying1;
    this.underlying2 = underlying2;

    if (underlying1.getScenarioCount() == 1) {
      scenarioCount = underlying2.getScenarioCount();
    } else if (underlying2.getScenarioCount() == 1) {
      scenarioCount = underlying1.getScenarioCount();
    } else if (underlying1.getScenarioCount() == underlying2.getScenarioCount()) {
      scenarioCount = underlying1.getScenarioCount();
    } else {
      throw new IllegalArgumentException(
          Messages.format(
              "When combining scenario market data, both sets of data must have the same number of scenarios or one " +
                  "of them must have one scenario. Found {} and {} scenarios",
              underlying1.getScenarioCount(),
              underlying2.getScenarioCount()));
    }
  }

  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return underlying1.getValuationDate();
  }

  @Override
  public int getScenarioCount() {
    return scenarioCount;
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    Optional<MarketDataBox<T>> optional = underlying1.findValue(id);
    return optional.isPresent() ? optional : underlying2.findValue(id);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries timeSeries = underlying1.getTimeSeries(id);
    return !timeSeries.isEmpty() ? timeSeries : underlying2.getTimeSeries(id);
  }
}
