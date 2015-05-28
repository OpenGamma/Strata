/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.engine.calculations.function.result.CurrencyAmountList;
import com.opengamma.strata.engine.calculations.function.result.DefaultScenarioResult;
import com.opengamma.strata.engine.calculations.function.result.MultiCurrencyAmountList;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;

@Test
public class FunctionUtilsTest {

  public void toCurrencyAmountList() {
    List<CurrencyAmount> amounts = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.USD, 2));

    CurrencyAmountList expectedList = CurrencyAmountList.of(amounts);
    CurrencyAmountList list = amounts.stream().collect(FunctionUtils.toCurrencyAmountList());
    assertThat(list).isEqualTo(expectedList);
  }

  public void toMultiCurrencyAmountList() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 2), CurrencyAmount.of(Currency.GBP, 3)));

    MultiCurrencyAmountList expectedList = MultiCurrencyAmountList.of(amounts);
    MultiCurrencyAmountList list = amounts.stream().collect(FunctionUtils.toMultiCurrencyAmountList());
    assertThat(list).isEqualTo(expectedList);
  }

  public void toScenarioResultWithConversion1() {
    List<CurrencyAmount> amounts = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.USD, 2));

    CurrencyAmountList expectedResult = CurrencyAmountList.of(amounts);
    ScenarioResult<CurrencyAmount> result = amounts.stream().collect(FunctionUtils.toScenarioResult());
    assertThat(result).isEqualTo(expectedResult);
  }

  public void toScenarioResultWithConversion2() {
    List<MultiCurrencyAmount> amounts = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(CurrencyAmount.of(Currency.USD, 2), CurrencyAmount.of(Currency.GBP, 3)));

    MultiCurrencyAmountList expectedResult = MultiCurrencyAmountList.of(amounts);
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
}
