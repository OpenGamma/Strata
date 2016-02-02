/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

/**
 * Test {@link SingleScenarioResult}.
 */
@Test
public class SingleScenarioResultTest {

  public void create() {
    SingleScenarioResult<String> test = SingleScenarioResult.of(3, "A");
    assertEquals(test.getScenarioCount(), 3);
    assertEquals(test.getValue(), "A");
    assertEquals(test.get(0), "A");
    assertEquals(test.get(1), "A");
    assertEquals(test.get(2), "A");
    assertEquals(test.stream().collect(toList()), ImmutableList.of("A", "A", "A"));
  }

  public void convertCurrencyAmount() {
    List<FxRate> rates = ImmutableList.of(
        FxRate.of(Currency.GBP, Currency.USD, 1.61),
        FxRate.of(Currency.GBP, Currency.USD, 1.62),
        FxRate.of(Currency.GBP, Currency.USD, 1.63));
    CalculationEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), rates)
        .build();
    MarketDataMappings mappings = MarketDataMappings.of(MarketDataFeed.NONE);
    DefaultCalculationMarketData calculationMarketData = DefaultCalculationMarketData.of(marketData, mappings);

    SingleScenarioResult<CurrencyAmount> test = SingleScenarioResult.of(3, CurrencyAmount.of(Currency.GBP, 2));

    ScenarioResult<?> convertedList = test.convertedTo(Currency.USD, calculationMarketData);
    List<CurrencyAmount> expectedValues = ImmutableList.of(
        CurrencyAmount.of(Currency.USD, 2 * 1.61),
        CurrencyAmount.of(Currency.USD, 2 * 1.62),
        CurrencyAmount.of(Currency.USD, 2 * 1.63));
    DefaultScenarioResult<CurrencyAmount> expectedList = DefaultScenarioResult.of(expectedValues);
    assertThat(convertedList).isEqualTo(expectedList);
  }

  public void coverage() {
    SingleScenarioResult<String> test = SingleScenarioResult.of(3, "A");
    coverImmutableBean(test);
    SingleScenarioResult<String> test2 = SingleScenarioResult.of(2, "B");
    coverBeanEquals(test, test2);
  }

}
