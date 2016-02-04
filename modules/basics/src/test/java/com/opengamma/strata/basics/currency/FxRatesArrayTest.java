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
import static org.assertj.core.api.Assertions.offset;
import static org.testng.Assert.assertThrows;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class FxRatesArrayTest {

  private static final double TOLERANCE = 1e-10;

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

  public void crossRates() {
    FxRatesArray eurGbp = FxRatesArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.76, 0.75));
    FxRatesArray eurUsd = FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.11, 1.12));
    FxRatesArray gbpEur = FxRatesArray.of(Currency.GBP, Currency.EUR, DoubleArray.of(1 / 0.76, 1 / 0.75));
    FxRatesArray usdEur = FxRatesArray.of(Currency.USD, Currency.EUR, DoubleArray.of(1 / 1.11, 1 / 1.12));
    FxRatesArray expectedGbpUsd =
        FxRatesArray.of(Currency.GBP, Currency.USD, DoubleArray.of(1.460526315789474, 1.4933333333333334));

    assertArraysEqual(eurGbp.crossRates(eurUsd), expectedGbpUsd);
    assertArraysEqual(eurGbp.crossRates(usdEur), expectedGbpUsd);
    assertArraysEqual(gbpEur.crossRates(eurUsd), expectedGbpUsd);
    assertArraysEqual(gbpEur.crossRates(usdEur), expectedGbpUsd);

    assertArraysEqual(eurUsd.crossRates(eurGbp), expectedGbpUsd);
    assertArraysEqual(usdEur.crossRates(eurGbp), expectedGbpUsd);
    assertArraysEqual(eurUsd.crossRates(gbpEur), expectedGbpUsd);
    assertArraysEqual(usdEur.crossRates(gbpEur), expectedGbpUsd);
  }

  public void crossRatesInvalidInputs() {
    // Argument has both currencies the same
    assertThrowsIllegalArg(() ->
        FxRatesArray.of(Currency.GBP, Currency.USD, DoubleArray.of(1))
            .crossRates(FxRatesArray.of(Currency.EUR, Currency.EUR, DoubleArray.of(1))));

    // Receiver has both currencies the same
    assertThrowsIllegalArg(() ->
        FxRatesArray.of(Currency.GBP, Currency.GBP, DoubleArray.of(1))
            .crossRates(FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1))));

    // No currency in common
    assertThrowsIllegalArg(() ->
        FxRatesArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))
            .crossRates(FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1))));

    // Both pairs the same
    assertThrowsIllegalArg(() ->
        FxRatesArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))
            .crossRates(FxRatesArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))));

    // Different length arrays
    assertThrowsIllegalArg(() ->
        FxRatesArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))
            .crossRates(FxRatesArray.of(Currency.EUR, Currency.CHF, DoubleArray.of(1, 2))));
  }

  //--------------------------------------------------------------------------------------------------

  private static void assertArraysEqual(FxRatesArray a1, FxRatesArray a2) {
    assertThat(a1.getScenarioCount()).isEqualTo(a2.getScenarioCount());
    assertThat(a1.getPair()).isEqualTo(a2.getPair());

    for (int i = 0; i < a1.getScenarioCount(); i++) {
      assertThat(a1.fxRate(Currency.GBP, Currency.USD, i)).isEqualTo(a2.fxRate(Currency.GBP, Currency.USD, i), offset(TOLERANCE));
    }
  }
}
