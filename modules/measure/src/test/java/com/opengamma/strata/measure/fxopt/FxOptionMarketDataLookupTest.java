/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesId;

/**
 * Test {@link FxOptionMarketDataLookup}.
 */
@Test
public class FxOptionMarketDataLookupTest {

  private static final FxOptionVolatilitiesId VOL_ID1 = FxOptionVolatilitiesId.of("EURUSD1");
  private static final FxOptionVolatilities MOCK_VOLS = mock(FxOptionVolatilities.class);
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);
  private static final CurrencyPair EUR_USD = CurrencyPair.of(EUR, USD);
  private static final CurrencyPair GBP_USD = CurrencyPair.of(GBP, USD);
  private static final CurrencyPair EUR_GBP = CurrencyPair.of(EUR, GBP);

  static {
    when(MOCK_MARKET_DATA.getValue(VOL_ID1)).thenReturn(MOCK_VOLS);
  }

  //-------------------------------------------------------------------------
  public void test_of_single() {
    FxOptionMarketDataLookup test = FxOptionMarketDataLookup.of(EUR_USD, VOL_ID1);
    assertEquals(test.queryType(), FxOptionMarketDataLookup.class);
    assertEquals(test.getVolatilityCurrencyPairs(), ImmutableSet.of(EUR_USD));
    assertEquals(test.getVolatilityIds(EUR_USD), ImmutableSet.of(VOL_ID1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getVolatilityIds(GBP_USD));

    assertEquals(
        test.requirements(EUR_USD),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertEquals(
        test.requirements(ImmutableSet.of(EUR_USD)),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(EUR_GBP)));
  }

  public void test_of_map() {
    ImmutableMap<CurrencyPair, FxOptionVolatilitiesId> ids = ImmutableMap.of(EUR_USD, VOL_ID1, GBP_USD, VOL_ID1);
    FxOptionMarketDataLookup test = FxOptionMarketDataLookup.of(ids);
    assertEquals(test.queryType(), FxOptionMarketDataLookup.class);
    assertEquals(test.getVolatilityCurrencyPairs(), ImmutableSet.of(EUR_USD, GBP_USD));
    assertEquals(test.getVolatilityIds(EUR_USD), ImmutableSet.of(VOL_ID1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getVolatilityIds(EUR_GBP));

    assertEquals(
        test.requirements(EUR_USD),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertEquals(
        test.requirements(ImmutableSet.of(EUR_USD)),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(EUR_GBP)));

    assertEquals(test.volatilities(EUR_USD, MOCK_MARKET_DATA), MOCK_VOLS);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.volatilities(EUR_GBP, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  public void test_marketDataView() {
    FxOptionMarketDataLookup test = FxOptionMarketDataLookup.of(EUR_USD, VOL_ID1);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    FxOptionScenarioMarketData multiScenario = test.marketDataView(md);
    assertEquals(multiScenario.getLookup(), test);
    assertEquals(multiScenario.getMarketData(), md);
    assertEquals(multiScenario.getScenarioCount(), 1);
    FxOptionMarketData scenario = multiScenario.scenario(0);
    assertEquals(scenario.getLookup(), test);
    assertEquals(scenario.getMarketData(), md.scenario(0));
    assertEquals(scenario.getValuationDate(), valDate);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultFxOptionMarketDataLookup test =
        DefaultFxOptionMarketDataLookup.of(ImmutableMap.of(EUR_USD, VOL_ID1, GBP_USD, VOL_ID1));
    coverImmutableBean(test);
    DefaultFxOptionMarketDataLookup test2 = DefaultFxOptionMarketDataLookup.of(EUR_USD, VOL_ID1);
    coverBeanEquals(test, test2);

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
  }

  public void test_serialization() {
    DefaultFxOptionMarketDataLookup test =
        DefaultFxOptionMarketDataLookup.of(ImmutableMap.of(EUR_USD, VOL_ID1, GBP_USD, VOL_ID1));
    assertSerialization(test);
  }

}
