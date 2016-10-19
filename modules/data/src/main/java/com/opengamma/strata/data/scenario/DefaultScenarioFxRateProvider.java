/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.io.Serializable;

import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.data.MarketDataFxRateProvider;
import com.opengamma.strata.data.ObservableSource;

/**
 * A provider of FX rates which takes its data from one scenario in a set of data for multiple scenarios.
 */
class DefaultScenarioFxRateProvider
    implements ScenarioFxRateProvider, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The market data for a set of scenarios.
   */
  private final ScenarioMarketData marketData;

  /**
   * The source of the FX rates.
   */
  private final ObservableSource source;

  // creates an instance
  DefaultScenarioFxRateProvider(ScenarioMarketData marketData, ObservableSource source) {
    this.marketData = marketData;
    this.source = source;
  }

  @Override
  public int getScenarioCount() {
    return marketData.getScenarioCount();
  }

  @Override
  public FxRateProvider fxRateProvider(int scenarioIndex) {
    return MarketDataFxRateProvider.of(marketData.scenario(scenarioIndex), source);
  }

}
