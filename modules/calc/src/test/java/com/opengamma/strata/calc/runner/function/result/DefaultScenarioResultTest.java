/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

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
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

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
    List<FxRate> rates = ImmutableList.of(
        FxRate.of(Currency.GBP, Currency.USD, 1.61),
        FxRate.of(Currency.GBP, Currency.USD, 1.62),
        FxRate.of(Currency.GBP, Currency.USD, 1.63));
    CalculationEnvironment marketData = MarketEnvironment.builder(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = DefaultCalculationMarketData.of(marketData, mappings);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    ScenarioResult<?> convertedList = test.convertedTo(Currency.USD, calculationMarketData);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61),
        CurrencyAmount.of(Currency.USD, 2 * 1.62),
        CurrencyAmount.of(Currency.USD, 3 * 1.63));
    DefaultScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void noConversionNecessary() {
    CalculationEnvironment marketData = MarketEnvironment.builder(MarketDataBox.ofScenarioValues(date(2011, 3, 8), date(2011, 3, 9), date(2011, 3, 10)))
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = DefaultCalculationMarketData.of(marketData, mappings);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    ScenarioResult<?> convertedList = test.convertedTo(Currency.GBP, calculationMarketData);
    ScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(values);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void missingFxRates() {
    CalculationEnvironment marketData = MarketEnvironment.builder(MarketDataBox.ofScenarioValues(date(2011, 3, 8), date(2011, 3, 9), date(2011, 3, 10)))
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = DefaultCalculationMarketData.of(marketData, mappings);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    assertThrows(
        () -> test.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "No market data available.*");
  }

  public void wrongNumberOfFxRates() {
    List<FxRate> rates = ImmutableList.of(
        FxRate.of(Currency.GBP, Currency.USD, 1.61),
        FxRate.of(Currency.GBP, Currency.USD, 1.62),
        FxRate.of(Currency.GBP, Currency.USD, 1.63));
    CalculationEnvironment marketData = MarketEnvironment.builder(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = DefaultCalculationMarketData.of(marketData, mappings);

    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2));
    DefaultScenarioResult<CurrencyAmount> test = DefaultScenarioResult.of(values);

    assertThrows(
        () -> test.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "Market data must contain same number of scenarios.*");
  }
  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultScenarioResult<Integer> test = DefaultScenarioResult.of(1, 2, 3);
    coverImmutableBean(test);
    DefaultScenarioResult<String> test2 = DefaultScenarioResult.of("2", "3");
    coverBeanEquals(test, test2);
  }

}
