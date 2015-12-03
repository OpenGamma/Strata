/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

@Test
public class FxConvertibleListTest {

  public void create() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    FxConvertibleList test = FxConvertibleList.of(values);
    assertThat(test.getValues()).isEqualTo(values);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(0)).isEqualTo(CurrencyAmount.of(Currency.GBP, 1));
    assertThat(test.get(1)).isEqualTo(CurrencyAmount.of(Currency.GBP, 2));
    assertThat(test.get(2)).isEqualTo(CurrencyAmount.of(Currency.GBP, 3));
    assertThat(test.stream().collect(toList())).isEqualTo(ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1), CurrencyAmount.of(Currency.GBP, 2), CurrencyAmount.of(Currency.GBP, 3)));
  }

  /**
   * Test that values are converted to the reporting currency using the rates in the market data.
   */
  public void convertCurrencyAmount() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    FxConvertibleList list = FxConvertibleList.of(values);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    ScenarioResult<?> convertedList = list.convertedTo(Currency.USD, calculationMarketData);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61),
        CurrencyAmount.of(Currency.USD, 2 * 1.62),
        CurrencyAmount.of(Currency.USD, 3 * 1.63));
    DefaultScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test that values are converted to the reporting currency using the rates in the market data.
   */
  public void convertMultiCurrencyAmount() {
    List<MultiCurrencyAmount> values = ImmutableList.of(
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 1),
            CurrencyAmount.of(Currency.USD, 10),
            CurrencyAmount.of(Currency.EUR, 100)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 2),
            CurrencyAmount.of(Currency.USD, 20),
            CurrencyAmount.of(Currency.EUR, 200)),
        MultiCurrencyAmount.of(
            CurrencyAmount.of(Currency.GBP, 3),
            CurrencyAmount.of(Currency.USD, 30),
            CurrencyAmount.of(Currency.EUR, 300)));
    FxConvertibleList list = FxConvertibleList.of(values);
    List<FxRate> usdRates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    List<FxRate> eurRates = ImmutableList.of(1.07, 1.08, 1.09).stream()
        .map(rate -> FxRate.of(Currency.EUR, Currency.USD, rate))
        .collect(toImmutableList());
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), usdRates)
        .addValue(FxRateId.of(Currency.EUR, Currency.USD), eurRates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    ScenarioResult<?> convertedList = list.convertedTo(Currency.USD, calculationMarketData);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61 + 10 + 100 * 1.07),
        CurrencyAmount.of(Currency.USD, 2 * 1.62 + 20 + 200 * 1.08),
        CurrencyAmount.of(Currency.USD, 3 * 1.63 + 30 + 300 * 1.09));
    ScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test that no conversion is done if the values are already in the reporting currency.
   */
  public void noConversionNecessary() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    FxConvertibleList list = FxConvertibleList.of(values);
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(MarketDataBox.ofScenarioValues(date(2011, 3, 8), date(2011, 3, 9), date(2011, 3, 10)))
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    ScenarioResult<?> convertedList = list.convertedTo(Currency.GBP, calculationMarketData);
    ScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(values);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test the expected exception is thrown when there are no FX rates available to convert the values.
   */
  public void missingFxRates() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    FxConvertibleList list = FxConvertibleList.of(values);
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(date(2011, 3, 8)).build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    assertThrows(
        () -> list.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "No market data available for .*");
  }

  /**
   * Test the expected exception is thrown if there are not the same number of rates as there are values.
   */
  public void wrongNumberOfFxRates() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    List<FxRate> rates = ImmutableList.of(1.61, 1.62).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    FxConvertibleList list = FxConvertibleList.of(values);
    CalculationEnvironment marketData = CalculationEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    assertThrows(
        () -> list.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "The number of scenarios is greater than the number of rates .*");
  }

  public void coverage() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    FxConvertibleList test = FxConvertibleList.of(values);
    coverImmutableBean(test);
    List<CurrencyAmount> values2 = ImmutableList.of(CurrencyAmount.of(Currency.GBP, 2));
    FxConvertibleList test2 = FxConvertibleList.of(values2);
    coverBeanEquals(test, test2);
  }

}
