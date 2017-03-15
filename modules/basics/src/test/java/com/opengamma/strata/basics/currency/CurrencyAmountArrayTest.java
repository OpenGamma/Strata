/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurrencyAmountArray}.
 */
@Test
public class CurrencyAmountArrayTest {

  //-------------------------------------------------------------------------
  public void test_of_CurrencyDoubleArray() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyAmountArray test = CurrencyAmountArray.of(GBP, values);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValues()).isEqualTo(values);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(GBP, 3));
    assertThat(test.stream().collect(toList())).containsExactly(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
  }

  public void test_of_List() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
    CurrencyAmountArray test = CurrencyAmountArray.of(values);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValues()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(GBP, 3));
    assertThat(test.stream().collect(toList())).containsExactly(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
  }

  public void test_of_CurrencyList_mixedCurrency() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThrowsIllegalArg(() -> CurrencyAmountArray.of(values));
  }

  public void test_of_function() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
    CurrencyAmountArray test = CurrencyAmountArray.of(3, i -> values.get(i));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getValues()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(GBP, 3));
    assertThat(test.stream().collect(toList())).containsExactly(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(GBP, 2), CurrencyAmount.of(GBP, 3));
  }

  public void test_of_function_mixedCurrency() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThrowsIllegalArg(() -> CurrencyAmountArray.of(3, i -> values.get(i)));
  }

  //-------------------------------------------------------------------------
  public void test_plus() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(GBP, 1), CurrencyAmount.of(USD, 2), CurrencyAmount.of(GBP, 3));
    assertThrowsIllegalArg(() -> CurrencyAmountArray.of(3, i -> values.get(i)));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyAmountArray test = CurrencyAmountArray.of(GBP, values);

    FxRate fxRate = FxRate.of(GBP, USD, 1.61);
    CurrencyAmountArray convertedList = test.convertedTo(USD, fxRate);
    DoubleArray expectedValues = DoubleArray.of(1 * 1.61, 2 * 1.61, 3 * 1.61);
    CurrencyAmountArray expectedList = CurrencyAmountArray.of(USD, expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void test_convertedTo_noConversionNecessary() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyAmountArray test = CurrencyAmountArray.of(GBP, values);

    FxRate fxRate = FxRate.of(GBP, USD, 1.61);
    CurrencyAmountArray convertedList = test.convertedTo(GBP, fxRate);
    assertThat(convertedList).isEqualTo(test);
  }

  public void test_convertedTo_missingFxRate() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyAmountArray test = CurrencyAmountArray.of(GBP, values);

    FxRate fxRate = FxRate.of(EUR, USD, 1.61);
    assertThrows(() -> test.convertedTo(USD, fxRate), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DoubleArray values = DoubleArray.of(1, 2, 3);
    CurrencyAmountArray test = CurrencyAmountArray.of(GBP, values);
    coverImmutableBean(test);
    DoubleArray values2 = DoubleArray.of(1, 2, 3);
    CurrencyAmountArray test2 = CurrencyAmountArray.of(USD, values2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurrencyAmountArray test = CurrencyAmountArray.of(GBP, DoubleArray.of(1, 2, 3));
    assertSerialization(test);
  }

}
