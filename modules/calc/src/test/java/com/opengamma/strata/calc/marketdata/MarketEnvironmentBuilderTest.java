/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.FxRatesArray;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class MarketEnvironmentBuilderTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2011, 3, 8);

  public void addSingleValues() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRate eurGbpRate = FxRate.of(Currency.EUR, Currency.GBP, 0.8);
    FxRate eurUsdRate = FxRate.of(Currency.EUR, Currency.USD, 1.1);

    Map<FxRateId, FxRate> values = ImmutableMap.of(
        eurGbpId, eurGbpRate,
        eurUsdId, eurUsdRate);
    MarketEnvironment marketData = MarketEnvironment.builder(VAL_DATE).addSingleValues(values).build();

    assertThat(marketData.getScenarioCount()).isEqualTo(1);
    assertThat(marketData.getValue(eurGbpId)).isEqualTo(MarketDataBox.ofSingleValue(eurGbpRate));
    assertThat(marketData.getValue(eurUsdId)).isEqualTo(MarketDataBox.ofSingleValue(eurUsdRate));
  }

  public void addScenarioValues() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRatesArray eurGbpRates = FxRatesArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.79, 0.8, 0.81));
    FxRatesArray eurUsdRates = FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.09, 1.1, 1.11));

    Map<FxRateId, FxRatesArray> values = ImmutableMap.of(
        eurGbpId, eurGbpRates,
        eurUsdId, eurUsdRates);
    MarketEnvironment marketData = MarketEnvironment.builder(VAL_DATE).addScenarioValues(values).build();

    assertThat(marketData.getScenarioCount()).isEqualTo(3);
    assertThat(marketData.getValue(eurGbpId)).isEqualTo(MarketDataBox.ofScenarioValue(eurGbpRates));
    assertThat(marketData.getValue(eurUsdId)).isEqualTo(MarketDataBox.ofScenarioValue(eurUsdRates));
  }

  public void addScenarioValueLists() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    List<FxRate> eurGbpRates = ImmutableList.of(
        FxRate.of(Currency.EUR, Currency.GBP, 0.79),
        FxRate.of(Currency.EUR, Currency.GBP, 0.8),
        FxRate.of(Currency.EUR, Currency.GBP, 0.81));
    List<FxRate> eurUsdRates = ImmutableList.of(
        FxRate.of(Currency.EUR, Currency.USD, 1.09),
        FxRate.of(Currency.EUR, Currency.USD, 1.1),
        FxRate.of(Currency.EUR, Currency.USD, 1.11));

    Map<FxRateId, List<FxRate>> values = ImmutableMap.of(
        eurGbpId, eurGbpRates,
        eurUsdId, eurUsdRates);
    MarketEnvironment marketData = MarketEnvironment.builder(VAL_DATE).addScenarioValueLists(values).build();

    assertThat(marketData.getScenarioCount()).isEqualTo(3);
    assertThat(marketData.getValue(eurGbpId)).isEqualTo(MarketDataBox.ofScenarioValues(eurGbpRates));
    assertThat(marketData.getValue(eurUsdId)).isEqualTo(MarketDataBox.ofScenarioValues(eurUsdRates));
  }
}
