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
 * Test {@link DefaultScenarioArray}.
 */
@Test
public class DefaultScenarioArrayTest {

  public void create() {
    DefaultScenarioArray<Integer> test = DefaultScenarioArray.of(1, 2, 3);
    assertThat(test.getValues()).isEqualTo(ImmutableList.of(1, 2, 3));
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(1)).isEqualTo(2);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).isEqualTo(ImmutableList.of(1, 2, 3));
  }

  public void create_withFunction() {
    DefaultScenarioArray<Integer> test = DefaultScenarioArray.of(3, i -> (i + 1));
    assertThat(test.getValues()).isEqualTo(ImmutableList.of(1, 2, 3));
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(1)).isEqualTo(2);
    assertThat(test.get(2)).isEqualTo(3);
    assertThat(test.stream().collect(toList())).isEqualTo(ImmutableList.of(1, 2, 3));
  }

  //-------------------------------------------------------------------------
  public void convertCurrencyAmount() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioArray<CurrencyAmount> test = DefaultScenarioArray.of(values);

    ScenarioArray<?> convertedList = test.convertedTo(Currency.USD, fxProvider);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61),
        CurrencyAmount.of(Currency.USD, 2 * 1.62),
        CurrencyAmount.of(Currency.USD, 3 * 1.63));
    DefaultScenarioArray<CurrencyAmount> expectedList = DefaultScenarioArray.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void noConversionNecessary() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioArray<CurrencyAmount> test = DefaultScenarioArray.of(values);

    ScenarioArray<?> convertedList = test.convertedTo(Currency.GBP, fxProvider);
    ScenarioArray<CurrencyAmount> expectedList = DefaultScenarioArray.of(values);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void notConvertible() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<String> values = ImmutableList.of("a", "b", "c");
    DefaultScenarioArray<String> test = DefaultScenarioArray.of(values);

    ScenarioArray<?> convertedList = test.convertedTo(Currency.GBP, fxProvider);
    assertThat(convertedList).isEqualTo(test);
  }

  public void missingFxRates() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(EUR, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioArray<CurrencyAmount> test = DefaultScenarioArray.of(values);

    assertThrows(() -> test.convertedTo(Currency.USD, fxProvider), IllegalArgumentException.class);
  }

  public void wrongNumberOfFxRates() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2));
    DefaultScenarioArray<CurrencyAmount> test = DefaultScenarioArray.of(values);

    assertThrows(
        () -> test.convertedTo(Currency.USD, fxProvider),
        IllegalArgumentException.class,
        "Expected 2 FX rates but received 3");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultScenarioArray<Integer> test = DefaultScenarioArray.of(1, 2, 3);
    coverImmutableBean(test);
    DefaultScenarioArray<String> test2 = DefaultScenarioArray.of("2", "3");
    coverBeanEquals(test, test2);
  }

}
