/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.runner.function.result.DefaultScenarioResult;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;

@Test
public class FunctionUtilsTest {

  public void toFxConvertibleList() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 2), CurrencyAmount.of(Currency.GBP, 3)));

    FxConvertibleList expectedList = FxConvertibleList.of(amounts);
    FxConvertibleList list = amounts.stream().collect(FunctionUtils.toFxConvertibleList());
    assertThat(list).isEqualTo(expectedList);
  }

  public void toScenarioResultWithConversion1() {
    List<CurrencyAmount> amounts = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.USD, 2));

    ScenarioResult<?> expectedResult = FxConvertibleList.of(amounts);
    ScenarioResult<CurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioResult());
    assertThat(result).isEqualTo(expectedResult);
  }

  public void toScenarioResultWithConversion2() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 2), CurrencyAmount.of(Currency.GBP, 3)));

    ScenarioResult<?> expectedResult = FxConvertibleList.of(amounts);
    ScenarioResult<MultiCurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioResult(true));
    assertThat(result).isEqualTo(expectedResult);
  }

  public void toScenarioResultNoConversion1() {
    List<CurrencyAmount> amounts = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.USD, 2));

    ScenarioResult<CurrencyAmount> expectedResult = DefaultScenarioResult.of(amounts);
    ScenarioResult<CurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioResult(false));
    assertThat(result).isEqualTo(expectedResult);
  }

  public void toScenarioResultNoConversion2() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 2), CurrencyAmount.of(Currency.GBP, 3)));

    ScenarioResult<MultiCurrencyAmount> expectedResult = DefaultScenarioResult.of(amounts);
    ScenarioResult<MultiCurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioResult(false));
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

    MultiCurrencyValuesArray expected = MultiCurrencyValuesArray.of(amounts);
    MultiCurrencyValuesArray array = amounts.stream().collect(FunctionUtils.toMultiCurrencyArray());
    assertThat(array).isEqualTo(expected);
  }
}
