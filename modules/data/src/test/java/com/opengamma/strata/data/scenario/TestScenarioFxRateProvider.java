/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * Testing implementation of {@link ScenarioFxRateProvider}.
 */
public class TestScenarioFxRateProvider implements ScenarioFxRateProvider {

  private final FxRateScenarioArray rates1;
  private final FxRateScenarioArray rates2;
  private final FxRateScenarioArray rates3;

  public TestScenarioFxRateProvider(FxRateScenarioArray rates1) {
    this.rates1 = rates1;
    this.rates2 = rates1;
    this.rates3 = rates1;
  }

  public TestScenarioFxRateProvider(FxRateScenarioArray rates1, FxRateScenarioArray rates2) {
    this.rates1 = rates1;
    this.rates2 = rates2;
    this.rates3 = rates2;
  }

  public TestScenarioFxRateProvider(FxRateScenarioArray rates1, FxRateScenarioArray rates2, FxRateScenarioArray rates3) {
    this.rates1 = rates1;
    this.rates2 = rates2;
    this.rates3 = rates3;
  }

  @Override
  public int getScenarioCount() {
    return rates1.getScenarioCount();
  }

  @Override
  public FxRateProvider fxRateProvider(int scenarioIndex) {
    return new FxRateProvider() {

      @Override
      public double fxRate(Currency baseCurrency, Currency counterCurrency) {
        if (baseCurrency.equals(counterCurrency)) {
          return 1;
        }
        if (baseCurrency.equals(rates1.getPair().getBase())) {
          return rates1.fxRate(baseCurrency, counterCurrency, scenarioIndex);
        } else if (baseCurrency.equals(rates2.getPair().getBase())) {
          return rates2.fxRate(baseCurrency, counterCurrency, scenarioIndex);
        } else {
          return rates3.fxRate(baseCurrency, counterCurrency, scenarioIndex);
        }
      }
    };
  }

}
