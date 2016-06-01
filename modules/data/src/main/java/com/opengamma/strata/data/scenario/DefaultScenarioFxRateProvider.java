/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.data.MarketDataFxRateProvider;

/**
 * A provider of FX rates which takes its data from one scenario in a set of data for multiple scenarios.
 */
class DefaultScenarioFxRateProvider implements ScenarioFxRateProvider {

  /**
   * The market data for a set of scenarios.
   */
  private final ScenarioMarketData marketData;

  // creates an instance
  DefaultScenarioFxRateProvider(ScenarioMarketData marketData) {
    this.marketData = marketData;
  }

  @Override
  public int getScenarioCount() {
    return marketData.getScenarioCount();
  }

  @Override
  public FxRateProvider fxRateProvider(int scenarioIndex) {
    return MarketDataFxRateProvider.of(marketData.scenario(scenarioIndex));
  }

}
