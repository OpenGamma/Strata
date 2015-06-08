/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function.result;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.engine.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class CurrencyAmountListTest {

  /**
   * Test that values are converted to the reporting currency using the rates in the market data.
   */
  public void convert() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    CurrencyAmountList list = CurrencyAmountList.of(values);
    List<FxRate> rates = ImmutableList.of(1.61, 1.62, 1.63).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8))
        .addValues(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    CurrencyAmountList convertedList = list.convertedTo(Currency.USD, calculationMarketData);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 1 * 1.61),
        CurrencyAmount.of(Currency.USD, 2 * 1.62),
        CurrencyAmount.of(Currency.USD, 3 * 1.63));
    CurrencyAmountList expectedList = CurrencyAmountList.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  /**
   * Test that no conversion is done and no rates are used if the values are already in the reporting currency.
   */
  public void noConversionNecessary() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    CurrencyAmountList list = CurrencyAmountList.of(values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(3, date(2011, 3, 8)).build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    CurrencyAmountList convertedList = list.convertedTo(Currency.GBP, calculationMarketData);
    assertThat(convertedList).isEqualTo(list);
  }

  /**
   * Test the expected exception is thrown when there are no FX rates available to convert the values.
   */
  public void missingFxRates() {
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    CurrencyAmountList list = CurrencyAmountList.of(values);
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
    List<CurrencyAmount> values = ImmutableList.of(
        CurrencyAmount.of(Currency.GBP, 1),
        CurrencyAmount.of(Currency.GBP, 2),
        CurrencyAmount.of(Currency.GBP, 3));
    List<FxRate> rates = ImmutableList.of(1.61, 1.62).stream()
        .map(rate -> FxRate.of(Currency.GBP, Currency.USD, rate))
        .collect(toImmutableList());
            CurrencyAmountList list = CurrencyAmountList.of(values);
    ScenarioMarketData marketData = ScenarioMarketData.builder(2, date(2011, 3, 8))
        .addValues(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE, FxRateMapping.INSTANCE);
    DefaultCalculationMarketData calculationMarketData = new DefaultCalculationMarketData(marketData, mappings);

    assertThrows(
        () -> list.convertedTo(Currency.USD, calculationMarketData),
        IllegalArgumentException.class,
        "The number of values is greater than the number of rates .*");
  }

}
