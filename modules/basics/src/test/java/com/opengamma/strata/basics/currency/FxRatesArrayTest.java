/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertThrows;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class FxRatesArrayTest {

  public void getValues() {
    FxRatesArray rates = FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.07, 1.08, 1.09));
    assertThat(rates.getPair()).isEqualTo(CurrencyPair.of(Currency.EUR, Currency.USD));
    assertThat(rates.getScenarioCount()).isEqualTo(3);
    assertThat(rates.getValue(0)).isEqualTo(FxRate.of(Currency.EUR, Currency.USD, 1.07));
    assertThat(rates.getValue(1)).isEqualTo(FxRate.of(Currency.EUR, Currency.USD, 1.08));
    assertThat(rates.getValue(2)).isEqualTo(FxRate.of(Currency.EUR, Currency.USD, 1.09));
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> rates.getValue(3));
  }

  public void fxRate() {
    FxRatesArray rates = FxRatesArray.of(CurrencyPair.of(Currency.EUR, Currency.USD), DoubleArray.of(1.07, 1.08, 1.09));
    assertThat(rates.fxRate(Currency.EUR, Currency.USD, 0)).isEqualTo(1.07);
    assertThat(rates.fxRate(Currency.EUR, Currency.USD, 1)).isEqualTo(1.08);
    assertThat(rates.fxRate(Currency.EUR, Currency.USD, 2)).isEqualTo(1.09);

    assertThat(rates.fxRate(Currency.USD, Currency.EUR, 0)).isEqualTo(1 / 1.07);
    assertThat(rates.fxRate(Currency.USD, Currency.EUR, 1)).isEqualTo(1 / 1.08);
    assertThat(rates.fxRate(Currency.USD, Currency.EUR, 2)).isEqualTo(1 / 1.09);
  }

  public void identicalCurrenciesHaveRateOfOne() {
    assertThrowsIllegalArg(
        () -> FxRatesArray.of(Currency.EUR, Currency.EUR, DoubleArray.of(1.07, 1.08, 1.09)),
        "Conversion rate between identical currencies must be one");
  }

  public void unknownCurrencyPair() {
    FxRatesArray rates = FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.07, 1.08, 1.09));
    assertThrowsIllegalArg(() -> rates.fxRate(Currency.AED, Currency.ARS, 0));
  }

  public void coverage() {
    FxRatesArray rates1 = FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.07, 1.08, 1.09));
    FxRatesArray rates2 = FxRatesArray.of(Currency.GBP, Currency.USD, DoubleArray.of(1.46, 1.47, 1.48));
    coverImmutableBean(rates1);
    coverBeanEquals(rates1, rates2);
  }
}
