/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link FxRateScenarioArray}.
 */
public class FxRateScenarioArrayTest {

  private static final Offset<Double> TOLERANCE = within(1e-10);

  @Test
  public void getValues() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.07, 1.08, 1.09));
    assertThat(rates.getPair()).isEqualTo(CurrencyPair.of(Currency.EUR, Currency.USD));
    assertThat(rates.getScenarioCount()).isEqualTo(3);
    assertThat(rates.get(0)).isEqualTo(FxRate.of(Currency.EUR, Currency.USD, 1.07));
    assertThat(rates.get(1)).isEqualTo(FxRate.of(Currency.EUR, Currency.USD, 1.08));
    assertThat(rates.get(2)).isEqualTo(FxRate.of(Currency.EUR, Currency.USD, 1.09));
    assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class).isThrownBy(() -> rates.get(3));
  }

  @Test
  public void fxRate() {
    FxRateScenarioArray rates =
        FxRateScenarioArray.of(CurrencyPair.of(Currency.EUR, Currency.USD), DoubleArray.of(1.07, 1.08, 1.09));
    assertThat(rates.fxRate(Currency.EUR, Currency.USD, 0)).isEqualTo(1.07);
    assertThat(rates.fxRate(Currency.EUR, Currency.USD, 1)).isEqualTo(1.08);
    assertThat(rates.fxRate(Currency.EUR, Currency.USD, 2)).isEqualTo(1.09);

    assertThat(rates.fxRate(Currency.USD, Currency.EUR, 0)).isEqualTo(1 / 1.07);
    assertThat(rates.fxRate(Currency.USD, Currency.EUR, 1)).isEqualTo(1 / 1.08);
    assertThat(rates.fxRate(Currency.USD, Currency.EUR, 2)).isEqualTo(1 / 1.09);
  }

  @Test
  public void identicalCurrenciesHaveRateOfOne() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateScenarioArray.of(Currency.EUR, Currency.EUR, DoubleArray.of(1.07, 1.08, 1.09)))
        .withMessage("Conversion rate between identical currencies must be one");
  }

  @Test
  public void unknownCurrencyPair() {
    FxRateScenarioArray rates = FxRateScenarioArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.07, 1.08, 1.09));
    assertThatIllegalArgumentException().isThrownBy(() -> rates.fxRate(Currency.AED, Currency.ARS, 0));
  }

  @Test
  public void convert() {
    FxRateScenarioArray eurGbp = FxRateScenarioArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.76, 0.75));
    DoubleArray input = DoubleArray.of(1.11, 1.12);
    DoubleArray expected = DoubleArray.of(1.11 * 0.76, 1.12 * 0.75);
    DoubleArray converted = eurGbp.convert(input, Currency.EUR, Currency.GBP);
    for (int i = 0; i < converted.size(); i++) {
      assertThat(converted.get(i)).isEqualTo(expected.get(i), TOLERANCE);
    }
  }

  @Test
  public void convert_inverse() {
    FxRateScenarioArray eurGbp = FxRateScenarioArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.76, 0.75));
    DoubleArray input = DoubleArray.of(1.11, 1.12);
    DoubleArray expected = DoubleArray.of(1.11 * 1 / 0.76, 1.12 * 1 / 0.75);
    DoubleArray converted = eurGbp.convert(input, Currency.GBP, Currency.EUR);
    for (int i = 0; i < converted.size(); i++) {
      assertThat(converted.get(i)).isEqualTo(expected.get(i), TOLERANCE);
    }
  }

  @Test
  public void convert_unknown() {
    FxRateScenarioArray eurGbp = FxRateScenarioArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.76, 0.75));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> eurGbp.convert(DoubleArray.of(1.07, 1.08), Currency.EUR, Currency.USD));
  }

  @Test
  public void crossRates() {
    FxRateScenarioArray eurGbp = FxRateScenarioArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.76, 0.75));
    FxRateScenarioArray eurUsd = FxRateScenarioArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.11, 1.12));
    FxRateScenarioArray gbpEur = FxRateScenarioArray.of(Currency.GBP, Currency.EUR, DoubleArray.of(1 / 0.76, 1 / 0.75));
    FxRateScenarioArray usdEur = FxRateScenarioArray.of(Currency.USD, Currency.EUR, DoubleArray.of(1 / 1.11, 1 / 1.12));
    FxRateScenarioArray expectedGbpUsd =
        FxRateScenarioArray.of(Currency.GBP, Currency.USD, DoubleArray.of(1.460526315789474, 1.4933333333333334));

    assertArraysEqual(eurGbp.crossRates(eurUsd), expectedGbpUsd);
    assertArraysEqual(eurGbp.crossRates(usdEur), expectedGbpUsd);
    assertArraysEqual(gbpEur.crossRates(eurUsd), expectedGbpUsd);
    assertArraysEqual(gbpEur.crossRates(usdEur), expectedGbpUsd);

    assertArraysEqual(eurUsd.crossRates(eurGbp), expectedGbpUsd);
    assertArraysEqual(usdEur.crossRates(eurGbp), expectedGbpUsd);
    assertArraysEqual(eurUsd.crossRates(gbpEur), expectedGbpUsd);
    assertArraysEqual(usdEur.crossRates(gbpEur), expectedGbpUsd);
  }

  @Test
  public void crossRatesInvalidInputs() {
    // Argument has both currencies the same
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateScenarioArray.of(Currency.GBP, Currency.USD, DoubleArray.of(1))
            .crossRates(FxRateScenarioArray.of(Currency.EUR, Currency.EUR, DoubleArray.of(1))));

    // Receiver has both currencies the same
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateScenarioArray.of(Currency.GBP, Currency.GBP, DoubleArray.of(1))
            .crossRates(FxRateScenarioArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1))));

    // No currency in common
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateScenarioArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))
            .crossRates(FxRateScenarioArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1))));

    // Both pairs the same
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateScenarioArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))
            .crossRates(FxRateScenarioArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))));

    // Different length arrays
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateScenarioArray.of(Currency.GBP, Currency.CHF, DoubleArray.of(1))
            .crossRates(FxRateScenarioArray.of(Currency.EUR, Currency.CHF, DoubleArray.of(1, 2))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxRateScenarioArray rates1 = FxRateScenarioArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.07, 1.08, 1.09));
    FxRateScenarioArray rates2 = FxRateScenarioArray.of(Currency.GBP, Currency.USD, DoubleArray.of(1.46, 1.47, 1.48));
    coverImmutableBean(rates1);
    coverBeanEquals(rates1, rates2);
  }

  //-------------------------------------------------------------------------
  private static void assertArraysEqual(FxRateScenarioArray a1, FxRateScenarioArray a2) {
    assertThat(a1.getScenarioCount()).isEqualTo(a2.getScenarioCount());
    assertThat(a1.getPair()).isEqualTo(a2.getPair());

    for (int i = 0; i < a1.getScenarioCount(); i++) {
      assertThat(a1.fxRate(Currency.GBP, Currency.USD, i))
          .isEqualTo(a2.fxRate(Currency.GBP, Currency.USD, i), TOLERANCE);
    }
  }
}
