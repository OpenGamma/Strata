/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class MultiCurrencyValuesArrayTest {

  private static final MultiCurrencyValuesArray VALUES_ARRAY =
      MultiCurrencyValuesArray.of(
          ImmutableList.of(
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
                  CurrencyAmount.of(Currency.EUR, 44))));

  public void createAndGetValues() {
    assertThat(VALUES_ARRAY.getValues(Currency.GBP)).isEqualTo(DoubleArray.of(20, 21, 22));
    assertThat(VALUES_ARRAY.getValues(Currency.USD)).isEqualTo(DoubleArray.of(30, 32, 33));
    assertThat(VALUES_ARRAY.getValues(Currency.EUR)).isEqualTo(DoubleArray.of(40, 43, 44));

    MultiCurrencyValuesArray raggedArray = MultiCurrencyValuesArray.of(
        ImmutableList.of(
            MultiCurrencyAmount.of(
                CurrencyAmount.of(Currency.EUR, 4)),
            MultiCurrencyAmount.of(
                CurrencyAmount.of(Currency.GBP, 21),
                CurrencyAmount.of(Currency.USD, 32),
                CurrencyAmount.of(Currency.EUR, 43)),
            MultiCurrencyAmount.of(
                CurrencyAmount.of(Currency.EUR, 44))));

    assertThat(raggedArray.getValues(Currency.GBP)).isEqualTo(DoubleArray.of(0, 21, 0));
    assertThat(raggedArray.getValues(Currency.USD)).isEqualTo(DoubleArray.of(0, 32, 0));
    assertThat(raggedArray.getValues(Currency.EUR)).isEqualTo(DoubleArray.of(4, 43, 44));
  }

  public void getAllValuesUnsafe() {
    Map<Currency, DoubleArray> expected = ImmutableMap.of(
        Currency.GBP, DoubleArray.of(20, 21, 22),
        Currency.USD, DoubleArray.of(30, 32, 33),
        Currency.EUR, DoubleArray.of(40, 43, 44));
    assertThat(VALUES_ARRAY.getValues()).isEqualTo(expected);
  }

  public void get() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(
        CurrencyAmount.of(Currency.GBP, 22),
        CurrencyAmount.of(Currency.USD, 33),
        CurrencyAmount.of(Currency.EUR, 44));
    assertThat(VALUES_ARRAY.get(2)).isEqualTo(expected);
    assertThrows(() -> VALUES_ARRAY.get(3), IndexOutOfBoundsException.class);
    assertThrows(() -> VALUES_ARRAY.get(-1), IndexOutOfBoundsException.class);
  }

  public void stream() {
    List<MultiCurrencyAmount> expected = ImmutableList.of(
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

    assertThat(VALUES_ARRAY.stream().collect(toList())).isEqualTo(expected);
  }

  public void convert() {
    MarketDataBox<FxRate> gbpCadRate = MarketDataBox.ofScenarioValues(
        FxRate.of(Currency.GBP, Currency.CAD, 2.00),
        FxRate.of(Currency.GBP, Currency.CAD, 2.01),
        FxRate.of(Currency.GBP, Currency.CAD, 2.02));
    MarketDataBox<FxRate> usdCadRate = MarketDataBox.ofScenarioValues(
        FxRate.of(Currency.USD, Currency.CAD, 1.30),
        FxRate.of(Currency.USD, Currency.CAD, 1.31),
        FxRate.of(Currency.USD, Currency.CAD, 1.32));
    MarketDataBox<FxRate> eurCadRate = MarketDataBox.ofSingleValue(FxRate.of(Currency.EUR, Currency.CAD, 1.4));
    MarketEnvironment marketEnvironment = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.CAD), gbpCadRate)
        .addValue(FxRateId.of(Currency.EUR, Currency.CAD), eurCadRate)
        .addValue(FxRateId.of(Currency.USD, Currency.CAD), usdCadRate)
        .build();
    DefaultCalculationMarketData marketData =
        new DefaultCalculationMarketData(marketEnvironment, MarketDataMappings.empty());
    CurrencyValuesArray convertedArray = VALUES_ARRAY.convertedTo(Currency.CAD, marketData);
    DoubleArray expected = DoubleArray.of(
        20 * 2.00 + 30 * 1.30 + 40 * 1.4,
        21 * 2.01 + 32 * 1.31 + 43 * 1.4,
        22 * 2.02 + 33 * 1.32 + 44 * 1.4);
    assertThat(convertedArray.getValues()).isEqualTo(expected);
  }

  public void convertIntoAnExistingCurrency() {
    MarketDataBox<FxRate> gbpUsdRate = MarketDataBox.ofScenarioValues(
        FxRate.of(Currency.GBP, Currency.USD, 1.50),
        FxRate.of(Currency.GBP, Currency.USD, 1.51),
        FxRate.of(Currency.GBP, Currency.USD, 1.52));
    MarketDataBox<FxRate> eurGbpRate = MarketDataBox.ofSingleValue(FxRate.of(Currency.EUR, Currency.GBP, 0.7));
    MarketEnvironment marketEnvironment = MarketEnvironment.builder()
        .valuationDate(LocalDate.of(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), gbpUsdRate)
        .addValue(FxRateId.of(Currency.EUR, Currency.GBP), eurGbpRate)
        .build();
    DefaultCalculationMarketData marketData =
        new DefaultCalculationMarketData(marketEnvironment, MarketDataMappings.empty());
    CurrencyValuesArray convertedArray = VALUES_ARRAY.convertedTo(Currency.GBP, marketData);
    double[] expected = new double[]{
        20 + 30 / 1.50 + 40 * 0.7,
        21 + 32 / 1.51 + 43 * 0.7,
        22 + 33 / 1.52 + 44 * 0.7};

    for (int i = 0; i < 3; i++) {
      assertThat(convertedArray.get(i)).isEqualTo(expected[i], offset(1e-6));
    }
  }

  /**
   * Test the hand-written equals and hashCode methods which correctly handle maps with array values
   */
  public void equalsHashCode() {
    MultiCurrencyValuesArray array =
        MultiCurrencyValuesArray.of(
            ImmutableList.of(
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
                    CurrencyAmount.of(Currency.EUR, 44))));
    assertThat(array).isEqualTo(VALUES_ARRAY);
    assertThat(array.hashCode()).isEqualTo(VALUES_ARRAY.hashCode());
  }

  public void getCurrencies() {
    assertThat(VALUES_ARRAY.getCurrencies()).isEqualTo(ImmutableSet.of(Currency.GBP, Currency.USD, Currency.EUR));
  }
}
