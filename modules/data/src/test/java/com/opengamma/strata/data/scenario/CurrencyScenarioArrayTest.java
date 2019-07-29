/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurrencyScenarioArray}.
 */
public class CurrencyScenarioArrayTest {

  @Test
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

  @Test
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

  @Test
  public void create_fromList_mixedCurrency() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyScenarioArray.of(values));
  }

  @Test
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

  @Test
  public void create_fromFunction_mixedCurrency() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyScenarioArray.of(3, i -> values.get(i)));
  }

  //-------------------------------------------------------------------------
  /**
   * Test that values are converted to the reporting currency using the rates in the market data.
   */
  @Test
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
  @Test
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
  @Test
  public void missingFxRates() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    FxRateScenarioArray rates = FxRateScenarioArray.of(EUR, USD, DoubleArray.of(1.61, 1.62, 1.63));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);

    assertThatIllegalArgumentException().isThrownBy(() -> test.convertedTo(USD, fxProvider));
  }

  /**
   * Test the expected exception is thrown if there are not the same number of rates as there are values.
   */
  @Test
  public void wrongNumberOfFxRates() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    FxRateScenarioArray rates = FxRateScenarioArray.of(GBP, USD, DoubleArray.of(1.61, 1.62));
    ScenarioFxRateProvider fxProvider = new TestScenarioFxRateProvider(rates);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.convertedTo(USD, fxProvider))
        .withMessage("Expected 3 FX rates but received 2");
  }
  
  /**
   * Test the plus() methods work as expected.
   */
  @Test
  public void plus() {
    CurrencyScenarioArray currencyScenarioArray = CurrencyScenarioArray.of(GBP, DoubleArray.of(1, 2, 3));
  
    CurrencyScenarioArray arrayToAdd = CurrencyScenarioArray.of(GBP, DoubleArray.of(4, 5, 6));
    CurrencyScenarioArray plusArraysResult = currencyScenarioArray.plus(arrayToAdd);
    assertThat(plusArraysResult).isEqualTo(CurrencyScenarioArray.of(GBP, DoubleArray.of(5, 7, 9)));
  
    CurrencyAmount amountToAdd = CurrencyAmount.of(Currency.GBP, 10);
    CurrencyScenarioArray plusAmountResult = currencyScenarioArray.plus(amountToAdd);
    assertThat(plusAmountResult).isEqualTo(CurrencyScenarioArray.of(GBP, DoubleArray.of(11, 12, 13)));
  }
  
  /**
   * Test the minus() methods work as expected.
   */
  @Test
  public void minus() {
    CurrencyScenarioArray currencyScenarioArray = CurrencyScenarioArray.of(GBP, DoubleArray.of(1, 2, 3));
    
    CurrencyScenarioArray arrayToSubtract = CurrencyScenarioArray.of(GBP, DoubleArray.of(3, 2, 1));
    CurrencyScenarioArray minusArrayResult = currencyScenarioArray.minus(arrayToSubtract);
    assertThat(minusArrayResult).isEqualTo(CurrencyScenarioArray.of(GBP, DoubleArray.of(-2, 0, 2)));
    
    CurrencyAmount amountToSubtract = CurrencyAmount.of(Currency.GBP, 2);
    CurrencyScenarioArray minusAmountResult = currencyScenarioArray.minus(amountToSubtract);
    assertThat(minusAmountResult).isEqualTo(CurrencyScenarioArray.of(GBP, DoubleArray.of(-1, 0, 1)));
  }
  

  @Test
  public void coverage() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyScenarioArray test = CurrencyScenarioArray.of(GBP, values);
    coverImmutableBean(test);
    DoubleArray values2 = DoubleArray.of(1, 2, 3);
    CurrencyScenarioArray test2 = CurrencyScenarioArray.of(USD, values2);
    coverBeanEquals(test, test2);
  }

}
