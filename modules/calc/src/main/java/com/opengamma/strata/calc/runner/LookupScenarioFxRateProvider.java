/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.io.Serializable;

import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.ScenarioFxRateProvider;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * A provider of scenario FX rates that uses FX rate lookup.
 * The use of {@link FxRateLookup} allows triangulation currency and observable source to be controlled.
 */
class LookupScenarioFxRateProvider
    implements ScenarioFxRateProvider, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The market data for a set of scenarios.
   */
  private final ScenarioMarketData marketData;
  /**
   * The FX rate lookup.
   */
  private final FxRateLookup lookup;

  // obtains an instance, returning the interface type to make type system happy at call site
  static ScenarioFxRateProvider of(ScenarioMarketData marketData, FxRateLookup lookup) {
    return new LookupScenarioFxRateProvider(marketData, lookup);
  }

  private LookupScenarioFxRateProvider(ScenarioMarketData marketData, FxRateLookup lookup) {
    this.marketData = ArgChecker.notNull(marketData, "marketData");
    this.lookup = ArgChecker.notNull(lookup, "lookup");
  }

  @Override
  public int getScenarioCount() {
    return marketData.getScenarioCount();
  }

  @Override
  public FxRateProvider fxRateProvider(int scenarioIndex) {
    return lookup.fxRateProvider(marketData.scenario(scenarioIndex));
  }

}
