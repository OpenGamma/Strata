/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.data.ObservableSource;

/**
 * A provider of FX rates for scenarios.
 * <p>
 * This provides the ability to obtain a set of FX rates, one for each scenario.
 * The interface does not mandate when the rate applies, however it typically represents the current rate.
 * <p>
 * This is the multi-scenario version of {@link FxRateProvider}.
 * <p>
 * Implementations do not have to be immutable, but calls to the methods must be thread-safe.
 */
public interface ScenarioFxRateProvider {

  /**
   * Returns a scenario FX rate provider which takes its data from the provided market data.
   *
   * @param marketData  market data containing FX rates
   * @return a scenario FX rate provider which takes its data from the provided market data
   */
  public static ScenarioFxRateProvider of(ScenarioMarketData marketData) {
    return new DefaultScenarioFxRateProvider(marketData, ObservableSource.NONE);
  }

  /**
   * Returns a scenario FX rate provider which takes its data from the provided market data.
   *
   * @param marketData  market data containing FX rates
   * @param source  the source of the FX rates
   * @return a scenario FX rate provider which takes its data from the provided market data
   */
  public static ScenarioFxRateProvider of(ScenarioMarketData marketData, ObservableSource source) {
    return new DefaultScenarioFxRateProvider(marketData, source);
  }

  /**
   * Gets the number of scenarios.
   * 
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Converts an amount in a currency to an amount in a different currency using a rate from this provider.
   *
   * @param amount  an amount in {@code fromCurrency}
   * @param fromCurrency  the currency of the amount
   * @param toCurrency  the currency into which the amount should be converted
   * @param scenarioIndex  the scenario index
   * @return the amount converted into {@code toCurrency}
   * @throws IllegalArgumentException if either of the currencies aren't included in the currency pair of this rate
   */
  public default double convert(double amount, Currency fromCurrency, Currency toCurrency, int scenarioIndex) {
    return amount * fxRate(fromCurrency, toCurrency, scenarioIndex);
  }

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
