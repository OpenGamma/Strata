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

import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;

/**
 * Test {@link CalculationRules}.
 */
@Test
public class CalculationRulesTest {

  private static final CalculationFunctions FUNCTIONS = CalculationFunctions.empty();

  //-------------------------------------------------------------------------
  public void test_of_MarketRules() {
    CalculationRules test = CalculationRules.of(FUNCTIONS);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MarketRulesCurrency() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, USD);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
  }

  public void test_of_MarketRulesCurrencyParameters() {
    Param param = new Param();
    CalculationRules test = CalculationRules.of(FUNCTIONS, USD, param);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getParameters(),
        CalculationParameters.of(ReportingCurrency.of(USD)).combinedWith(CalculationParameters.of(param)));
  }

  public void test_of_ParametersArray() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, ReportingCurrency.of(USD));
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
  }

  public void test_of_Parameters() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, CalculationParameters.of(ReportingCurrency.of(USD)));
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationRules test = CalculationRules.of(FUNCTIONS);
    coverImmutableBean(test);
    CalculationRules test2 = CalculationRules.of(FUNCTIONS, USD);
    coverBeanEquals(test, test2);
  }

  static class Param implements CalculationParameter {
  }

}
