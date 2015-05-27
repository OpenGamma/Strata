/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function.result;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.engine.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class MultiCurrencyAmountListTest {

  /**
   * Test that values are converted to the reporting currency using the rates in the market data.
   */
  public void convert() {
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
    MultiCurrencyAmountList list = MultiCurrencyAmountList.of(values);
    List<Double> usdRates = ImmutableList.of(1.61, 1.62, 1.63);
    List<Double> eurRates = ImmutableList.of(1.07, 1.08, 1.09);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8))
        .addValues(FxRateId.of(Currency.GBP, Currency.USD), usdRates)
        .addValues(FxRateId.of(Currency.EUR, Currency.USD), eurRates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    CurrencyAmountList convertedList = list.convertedTo(Currency.USD, calculationMarketData);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61 + 10 + 100 * 1.07),
        CurrencyAmount.of(Currency.USD, 2 * 1.62 + 20 + 200 * 1.08),
        CurrencyAmount.of(Currency.USD, 3 * 1.63 + 30 + 300 * 1.09));
    CurrencyAmountList expectedList = CurrencyAmountList.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test that no FX conversion is done and no rates are used if the values are already in the reporting currency.
   */
  public void noConversionNecessary() {
    List<MultiCurrencyAmount> values = ImmutableList.of(
        MultiCurrencyAmount.of(Currency.GBP, 1),
        MultiCurrencyAmount.of(Currency.GBP, 2),
        MultiCurrencyAmount.of(Currency.GBP, 3));
    MultiCurrencyAmountList list = MultiCurrencyAmountList.of(values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    CurrencyAmountList expectedList = CurrencyAmountList.of(expectedValues);

    CurrencyAmountList convertedList = list.convertedTo(Currency.GBP, calculationMarketData);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test the expected exception is thrown when there are no FX rates available to convert the values.
   */
  public void missingFxRates() {
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
    MultiCurrencyAmountList list = MultiCurrencyAmountList.of(values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    assertThrows(
        () -> list.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "No values available for market data ID .*");
  }

  /**
   * Test the expected exception is thrown if there are not the same number of rates as there are values.
   */
  public void wrongNumberOfFxRates() {
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
    List<Double> usdRates = ImmutableList.of(1.61, 1.62);
    List<Double> eurRates = ImmutableList.of(1.07, 1.08);
    MultiCurrencyAmountList list = MultiCurrencyAmountList.of(values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(2, date(2011, 3, 8))
        .addValues(FxRateId.of(Currency.GBP, Currency.USD), usdRates)
        .addValues(FxRateId.of(Currency.EUR, Currency.USD), eurRates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    assertThrows(
        () -> list.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "The number of values is greater than the number of rates .*");
  }
}
