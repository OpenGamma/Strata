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
import com.opengamma.strata.calc.runner.TestParameter;

/**
 * Test {@link CalculationRules}.
 */
@Test
public class CalculationRulesTest {

  private static final CalculationFunctions FUNCTIONS = CalculationFunctions.empty();
  private static final CalculationParameter PARAM = new TestParameter();

  //-------------------------------------------------------------------------
  public void test_of_FunctionsParametersArray() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, PARAM);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getReportingCurrency(), ReportingCurrency.NATURAL);
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  public void test_of_FunctionsParametersObject() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, CalculationParameters.of(PARAM));
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getReportingCurrency(), ReportingCurrency.NATURAL);
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  public void test_of_FunctionsCurrencyParametersArray() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, USD, PARAM);
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  public void test_of_All() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, ReportingCurrency.of(USD), CalculationParameters.of(PARAM));
    assertEquals(test.getFunctions(), FUNCTIONS);
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationRules test = CalculationRules.of(FUNCTIONS);
    coverImmutableBean(test);
    CalculationRules test2 = CalculationRules.of(FUNCTIONS, USD);
    coverBeanEquals(test, test2);
  }

}
