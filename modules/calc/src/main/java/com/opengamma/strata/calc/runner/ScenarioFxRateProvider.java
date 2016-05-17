/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * A provider of FX rates for scenarios.
 * <p>
 * This provides the ability to obtain a set of FX rates, one for each scenario.
 * The interface does not mandate when the rate applies, however it typically represents the current rate.
 * <p>
 * Implementations do not have to be immutable, but calls to the methods must be thread-safe.
 */
public interface ScenarioFxRateProvider {

  /**
   * Gets the number of scenarios.
   * 
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Gets the FX rate for the specified currency pair and scenario index.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * This will return 1 if the two input currencies are the same.
   * 
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @param scenarioIndex  the scenario index
   * @return the FX rate for the currency pair
   * @throws RuntimeException if no FX rate could be found
   */
  public default double fxRate(Currency baseCurrency, Currency counterCurrency, int scenarioIndex) {
    return fxRateProvider(scenarioIndex).fxRate(baseCurrency, counterCurrency);
  }

  /**
   * Gets the FX rate provider for the specified scenario index.
   * 
   * @param scenarioIndex  the scenario index
   * @return the FX rate for the currency pair
   * @throws RuntimeException if no FX rate could be found
   */
  public abstract FxRateProvider fxRateProvider(int scenarioIndex);

}
