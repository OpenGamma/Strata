/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculation.function.result;

import java.util.List;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

/**
 * A provider of FX rates which takes its data from one scenario in a set of data for multiple scenarios.
 */
class ScenarioRateProvider implements FxRateProvider {

  /** The market data for a set of scenarios. */
  private final CalculationMarketData marketData;

  /** The index of the scenario in {@link #marketData} from which the FX rates are taken. */
  private final int scenarioIndex;

  /**
   * Returns a rate provider which uses rates from the scenario at the specified index in the market data.
   *
   * @param marketData  market data for a set of scenarios
   * @param scenarioIndex  the index of the scenario from which FX rates are taken
   * @return a rate provider which uses rates from the scenario at the specified index in the market data
   */
  static ScenarioRateProvider of(CalculationMarketData marketData, Long scenarioIndex) {
    return new ScenarioRateProvider(marketData, scenarioIndex.intValue());
  }

  private ScenarioRateProvider(CalculationMarketData marketData, int scenarioIndex) {
    this.marketData = marketData;
    this.scenarioIndex = scenarioIndex;

    if (marketData.getScenarioCount() <= scenarioIndex) {
      throw new IllegalArgumentException(
          Messages.format(
              "The number of values is greater than the number of rates ({})",
              marketData.getScenarioCount()));
    }
  }

  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1;
    }
    List<FxRate> rates = marketData.getValues(FxRateKey.of(baseCurrency, counterCurrency));
    return rates.get(scenarioIndex).fxRate(baseCurrency, counterCurrency);
  }
}
