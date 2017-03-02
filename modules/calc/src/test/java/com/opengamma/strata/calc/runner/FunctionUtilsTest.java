/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;

@Test
public class FunctionUtilsTest {

  public void toScenarioArray() {
    List<CurrencyAmount> amounts = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.USD, 2));

    ScenarioArray<CurrencyAmount> expectedResult = ScenarioArray.of(amounts);
    ScenarioArray<CurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioArray());
    assertThat(result).isEqualTo(expectedResult);
  }

  public void toScenarioArray2() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 2), CurrencyAmount.of(Currency.GBP, 3)));

    ScenarioArray<MultiCurrencyAmount> expectedResult = ScenarioArray.of(amounts);
    ScenarioArray<MultiCurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioArray());
    assertThat(result).isEqualTo(expectedResult);
  }

  public void toMultiCurrencyArray() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 20),
            CurrencyAmount.of(Currency.USD, 30),
            CurrencyAmount.of(Currency.EUR, 40)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 21),
            CurrencyAmount.of(Currency.USD, 32),
            CurrencyAmount.of(Currency.EUR, 43)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 22),
            CurrencyAmount.of(Currency.USD, 33),
            CurrencyAmount.of(Currency.EUR, 44)));

    MultiCurrencyScenarioArray expected = MultiCurrencyScenarioArray.of(amounts);
    MultiCurrencyScenarioArray array = amounts.stream().collect(FunctionUtils.toMultiCurrencyValuesArray());
    assertThat(array).isEqualTo(expected);
  }
}
