/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link DefaultScenarioResult}.
 */
@Test
public class DefaultScenarioResultTest {

  public void create() {
    DefaultScenarioResult<Integer> test = DefaultScenarioResult.of(1, 2, 3);
    assertThat(test.getValues()).isEqualTo(ImmutableList.of(1, 2, 3));
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(1)).isEqualTo(2);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).isEqualTo(ImmutableList.of(1, 2, 3));
  }

  public void create_withFunction() {
    DefaultScenarioResult<Integer> test = DefaultScenarioResult.of(3, i -> (i + 1));
    assertThat(test.getValues()).isEqualTo(ImmutableList.of(1, 2, 3));
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(1)).isEqualTo(2);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).isEqualTo(ImmutableList.of(1, 2, 3));
  }

  //-------------------------------------------------------------------------
  public void convertCurrencyAmount() {
    FxRatesArray rates = FxRatesArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    ScenarioResult<?> convertedList = test.convertedTo(Currency.USD, fxProvider);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61),
        CurrencyAmount.of(Currency.USD, 2 * 1.62),
        CurrencyAmount.of(Currency.USD, 3 * 1.63));
    DefaultScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void noConversionNecessary() {
    FxRatesArray rates = FxRatesArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    ScenarioResult<?> convertedList = test.convertedTo(Currency.GBP, fxProvider);
    ScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(values);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void missingFxRates() {
    FxRatesArray rates = FxRatesArray.of(EUR, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    assertThrows(() -> test.convertedTo(Currency.USD, fxProvider), IllegalArgumentException.class);
  }

  public void wrongNumberOfFxRates() {
    FxRatesArray rates = FxRatesArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    assertThrows(
        () -> test.convertedTo(Currency.USD, fxProvider),
        IllegalArgumentException.class,
        "Expected 2 FX rates but received 3");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultScenarioResult<Integer> test = DefaultScenarioResult.of(1, 2, 3);
    coverImmutableBean(test);
    DefaultScenarioResult<String> test2 = DefaultScenarioResult.of("2", "3");
    coverBeanEquals(test, test2);
  }

}
