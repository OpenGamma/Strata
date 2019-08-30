/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.TestParameter;

/**
 * Test {@link CalculationRules}.
 */
public class CalculationRulesTest {

  private static final CalculationFunctions FUNCTIONS = CalculationFunctions.empty();
  private static final CalculationParameter PARAM = new TestParameter();

  //-------------------------------------------------------------------------
  @Test
  public void test_of_FunctionsParametersArray() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, PARAM);
    assertThat(test.getFunctions()).isEqualTo(FUNCTIONS);
    assertThat(test.getReportingCurrency()).isEqualTo(ReportingCurrency.NATURAL);
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  @Test
  public void test_of_FunctionsParametersObject() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, CalculationParameters.of(PARAM));
    assertThat(test.getFunctions()).isEqualTo(FUNCTIONS);
    assertThat(test.getReportingCurrency()).isEqualTo(ReportingCurrency.NATURAL);
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  @Test
  public void test_of_FunctionsCurrencyParametersArray() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, USD, PARAM);
    assertThat(test.getFunctions()).isEqualTo(FUNCTIONS);
    assertThat(test.getReportingCurrency()).isEqualTo(ReportingCurrency.of(USD));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  @Test
  public void test_of_All() {
    CalculationRules test = CalculationRules.of(FUNCTIONS, ReportingCurrency.of(USD), CalculationParameters.of(PARAM));
    assertThat(test.getFunctions()).isEqualTo(FUNCTIONS);
    assertThat(test.getReportingCurrency()).isEqualTo(ReportingCurrency.of(USD));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CalculationRules test = CalculationRules.of(FUNCTIONS);
    coverImmutableBean(test);
    CalculationRules test2 = CalculationRules.of(FUNCTIONS, USD);
    coverBeanEquals(test, test2);
  }

}
