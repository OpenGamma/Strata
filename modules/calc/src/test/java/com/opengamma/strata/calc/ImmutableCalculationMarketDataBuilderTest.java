/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.FxRatesArray;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link ImmutableCalculationMarketDataBuilder}.
 */
@Test
public class ImmutableCalculationMarketDataBuilderTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2011, 3, 8);

  //-------------------------------------------------------------------------
  public void addNothing() {
    ImmutableCalculationMarketData marketData = ImmutableCalculationMarketData.builder(VAL_DATE).build();
    assertEquals(marketData.getScenarioCount(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_addSingleAndList() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRate eurGbpRate = FxRate.of(Currency.EUR, Currency.GBP, 0.8);
    FxRate eurUsdRate1 = FxRate.of(Currency.EUR, Currency.USD, 1.1);
    FxRate eurUsdRate2 = FxRate.of(Currency.EUR, Currency.USD, 1.2);

    ImmutableCalculationMarketData marketData = ImmutableCalculationMarketData.builder(VAL_DATE)
        .addValue(eurGbpId, eurGbpRate)
        .addScenarioValue(eurUsdId, ImmutableList.of(eurUsdRate1, eurUsdRate2))
        .build();
    assertEquals(marketData.getScenarioCount(), 2);
    assertEquals(marketData.getValue(eurGbpId), MarketDataBox.ofSingleValue(eurGbpRate));
    assertEquals(marketData.getValue(eurUsdId), MarketDataBox.ofScenarioValues(eurUsdRate1, eurUsdRate2));
  }

  public void test_addSingleAndBox() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRate eurGbpRate = FxRate.of(Currency.EUR, Currency.GBP, 0.8);
    FxRate eurUsdRate1 = FxRate.of(Currency.EUR, Currency.USD, 1.1);
    FxRate eurUsdRate2 = FxRate.of(Currency.EUR, Currency.USD, 1.2);

    ImmutableCalculationMarketData marketData = ImmutableCalculationMarketData.builder(VAL_DATE)
        .addValue(eurGbpId, eurGbpRate)
        .addBox(eurUsdId, MarketDataBox.ofScenarioValues(eurUsdRate1, eurUsdRate2))
        .build();
    assertEquals(marketData.getScenarioCount(), 2);
    assertEquals(marketData.getValue(eurGbpId), MarketDataBox.ofSingleValue(eurGbpRate));
    assertEquals(marketData.getValue(eurUsdId), MarketDataBox.ofScenarioValues(eurUsdRate1, eurUsdRate2));
  }

  public void test_addBadScenarioCount() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRate eurGbpRate1 = FxRate.of(Currency.EUR, Currency.GBP, 0.8);
    FxRate eurGbpRate2 = FxRate.of(Currency.EUR, Currency.GBP, 0.9);
    FxRate eurGbpRate3 = FxRate.of(Currency.EUR, Currency.GBP, 0.95);
    FxRate eurUsdRate1 = FxRate.of(Currency.EUR, Currency.USD, 1.1);
    FxRate eurUsdRate2 = FxRate.of(Currency.EUR, Currency.USD, 1.2);

    ImmutableCalculationMarketDataBuilder builder = ImmutableCalculationMarketData.builder(VAL_DATE)
        .addBox(eurGbpId, MarketDataBox.ofScenarioValues(eurGbpRate1, eurGbpRate2, eurGbpRate3));
    assertThrowsIllegalArg(() -> builder.addBox(eurUsdId, MarketDataBox.ofScenarioValues(eurUsdRate1, eurUsdRate2)));
  }

  //-------------------------------------------------------------------------
  public void test_addValueMap() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRate eurGbpRate = FxRate.of(Currency.EUR, Currency.GBP, 0.8);
    FxRate eurUsdRate = FxRate.of(Currency.EUR, Currency.USD, 1.1);
    Map<FxRateId, FxRate> values = ImmutableMap.of(
        eurGbpId, eurGbpRate,
        eurUsdId, eurUsdRate);

    ImmutableCalculationMarketData marketData = ImmutableCalculationMarketData.builder(VAL_DATE)
        .addValueMap(values)
        .build();
    assertEquals(marketData.getScenarioCount(), 1);
    assertEquals(marketData.getValue(eurGbpId), MarketDataBox.ofSingleValue(eurGbpRate));
    assertEquals(marketData.getValue(eurUsdId), MarketDataBox.ofSingleValue(eurUsdRate));
  }

  //-------------------------------------------------------------------------
  public void test_addScenarioValueMap() {
    FxRateId eurGbpId = FxRateId.of(Currency.EUR, Currency.GBP);
    FxRateId eurUsdId = FxRateId.of(Currency.EUR, Currency.USD);
    FxRatesArray eurGbpRates = FxRatesArray.of(Currency.EUR, Currency.GBP, DoubleArray.of(0.79, 0.8, 0.81));
    FxRatesArray eurUsdRates = FxRatesArray.of(Currency.EUR, Currency.USD, DoubleArray.of(1.09, 1.1, 1.11));
    Map<FxRateId, FxRatesArray> values = ImmutableMap.of(
        eurGbpId, eurGbpRates,
        eurUsdId, eurUsdRates);

    ImmutableCalculationMarketData marketData = ImmutableCalculationMarketData.builder(VAL_DATE)
        .addScenarioValueMap(values)
        .build();
    assertEquals(marketData.getScenarioCount(), 3);
    assertEquals(marketData.getValue(eurGbpId), MarketDataBox.ofScenarioValue(eurGbpRates));
    assertEquals(marketData.getValue(eurUsdId), MarketDataBox.ofScenarioValue(eurUsdRates));
  }

}
