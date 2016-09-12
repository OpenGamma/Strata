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
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurrencyScenarioArray}.
 */
@Test
public class CurrencyScenarioArrayTest {

  public void create() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmounts().getValues()).isEqualTo(values);
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(GBP, 3));
    assertThat(test.stream().collect(toList())).containsExactly(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
  }

  public void create_fromList() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
    CurrencyScenarioArray test = CurrencyScenarioArray.of(values);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmounts().getValues()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(GBP, 3));
    assertThat(test.stream().collect(toList())).containsExactly(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
  }

  public void create_fromList_mixedCurrency() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThrowsIllegalArg(() -> CurrencyScenarioArray.of(values));
  }

  public void create_fromFunction() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
    CurrencyScenarioArray test = CurrencyScenarioArray.of(3, i -> values.get(i));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmounts().getValues()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
    assertThat(test.getScenarioCount()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(GBP, 3));
    assertThat(test.stream().collect(toList())).containsExactly(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
  }

  public void create_fromFunction_mixedCurrency() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThrowsIllegalArg(() -> CurrencyScenarioArray.of(3, i -> values.get(i)));
  }

  //-------------------------------------------------------------------------
  /**
   * Test that values are converted to the reporting currency using the rates in the market data.
   */
  public void convert() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);

    CurrencyScenarioArray convertedList = test.convertedTo(USD, fxProvider);
    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.62, 3 * 1.63);
    CurrencyScenarioArray expectedList = CurrencyScenarioArray.of(USD, expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test that no conversion is done and no rates are used if the values are already in the reporting currency.
   */
  public void noConversionNecessary() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);

    CurrencyScenarioArray convertedList = test.convertedTo(GBP, fxProvider);
    assertThat(convertedList).isEqualTo(test);
  }

  /**
   * Test the expected exception is thrown when there are no FX rates available to convert the values.
   */
  public void missingFxRates() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    FxRateScenarioArray rates = FxRateScenarioArray.of(EUR, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);

    assertThrows(() -> test.convertedTo(USD, fxProvider), IllegalArgumentException.class);
  }

  /**
   * Test the expected exception is thrown if there are not the same number of rates as there are values.
   */
  public void wrongNumberOfFxRates() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);

    assertThrows(
        () -> test.convertedTo(USD, fxProvider),
        IllegalArgumentException.class,
        "Expected 3 FX rates but received 2");
  }

  public void coverage() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);
    coverImmutableBean(test);
    DoubleArray values2 = DoubleArray.of(1, 2, 3);
    CurrencyScenarioArray test2 = CurrencyScenarioArray.of(USD, values2);
    coverBeanEquals(test, test2);
  }

}
