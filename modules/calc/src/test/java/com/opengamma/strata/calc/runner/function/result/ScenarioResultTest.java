/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link ScenarioResult}.
 */
@Test
public class ScenarioResultTest {

  private static final CurrencyAmount AMOUNT = CurrencyAmount.of(GBP, 10);

  public void test_of_arrayNotConvertible() {
    ScenarioResult<String> test = ScenarioResult.of("1", "2", "3");
    DefaultScenarioResult<String> expected = DefaultScenarioResult.of("1", "2", "3");
    assertEquals(test, expected);
  }

  public void test_of_arrayConvertible() {
    ScenarioResult<CurrencyAmount> test = ScenarioResult.of(AMOUNT);
    FxConvertibleList<CurrencyAmount> expected = FxConvertibleList.of(AMOUNT);
    assertEquals(test, expected);
  }

  public void test_of_listNotConvertible() {
    ScenarioResult<String> test = ScenarioResult.of(ImmutableList.of("1", "2", "3"));
    DefaultScenarioResult<String> expected = DefaultScenarioResult.of("1", "2", "3");
    assertEquals(test, expected);
  }

  public void test_of_listConvertible() {
    ScenarioResult<CurrencyAmount> test = ScenarioResult.of(ImmutableList.of(AMOUNT));
    FxConvertibleList<CurrencyAmount> expected = FxConvertibleList.of(AMOUNT);
    assertEquals(test, expected);
  }

  public void test_of_functionNotConvertible() {
    ScenarioResult<String> test = ScenarioResult.of(3, i -> Integer.toString(i + 1));
    DefaultScenarioResult<String> expected = DefaultScenarioResult.of("1", "2", "3");
    assertEquals(test, expected);
  }

  public void test_of_functionPartlyConvertible() {
    ScenarioResult<Object> test = ScenarioResult.of(2, i -> i == 0 ? AMOUNT : "1");
    DefaultScenarioResult<Object> expected = DefaultScenarioResult.of(AMOUNT, "1");
    assertEquals(test, expected);
  }

  public void test_of_functionConvertible() {
    ScenarioResult<CurrencyAmount> test = ScenarioResult.of(1, i -> AMOUNT);
    FxConvertibleList<CurrencyAmount> expected = FxConvertibleList.of(AMOUNT);
    assertEquals(test, expected);
  }

}
