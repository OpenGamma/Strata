/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;

/**
 * Test {@link CalculationRules}.
 */
@Test
public class CalculationRulesTest {

  private static final CalculationFunctions FUNCTIONS = CalculationFunctions.empty();
  private static final MarketDataRules MD_RULES = MarketDataRules.empty();

  //-------------------------------------------------------------------------
  public void test_of_MarketRules() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, MD_RULES);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getMarketDataRules(), MD_RULES);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MarketRulesCurrency() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, MD_RULES, USD);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getMarketDataRules(), MD_RULES);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
  }

  public void test_of_MarketRulesCurrencyParameters() {
    Param param = new Param();
    CalculationRules test = CalculationRules.of(FUNCTIONS, MD_RULES, USD, param);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getMarketDataRules(), MD_RULES);
    assertEquals(test.getParameters(),
        CalculationParameters.of(ReportingCurrency.of(USD)).combinedWith(CalculationParameters.of(param)));
  }

  public void test_of_ParametersArray() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, MD_RULES, ReportingCurrency.of(USD));
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getMarketDataRules(), MD_RULES);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
  }

  public void test_of_Parameters() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, MD_RULES, CalculationParameters.of(ReportingCurrency.of(USD)));
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getMarketDataRules(), MD_RULES);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, MD_RULES);
    coverImmutableBean(test);
    CalculationRules test2 = CalculationRules.of(FUNCTIONS, MD_RULES, USD);
    coverBeanEquals(test, test2);
  }

  static class Param implements CalculationParameter {
  }

}
