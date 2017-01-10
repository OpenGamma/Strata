/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;

/**
 * Test {@link SwaptionMarketDataLookup}.
 */
@Test
public class SwaptionMarketDataLookupTest {

  private static final SwaptionVolatilitiesId VOL_ID1 = SwaptionVolatilitiesId.of("USD1");
  private static final SwaptionVolatilities MOCK_VOLS = mock(SwaptionVolatilities.class);
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  static {
    when(MOCK_MARKET_DATA.getValue(VOL_ID1)).thenReturn(MOCK_VOLS);
  }

  //-------------------------------------------------------------------------
  public void test_of_single() {
    SwaptionMarketDataLookup test = SwaptionMarketDataLookup.of(USD_LIBOR_3M, VOL_ID1);
    assertEquals(test.queryType(), SwaptionMarketDataLookup.class);
    assertEquals(test.getVolatilityIndices(), ImmutableSet.of(USD_LIBOR_3M));
    assertEquals(test.getVolatilityIds(USD_LIBOR_3M), ImmutableSet.of(VOL_ID1));
    assertThrowsIllegalArg(() -> test.getVolatilityIds(GBP_LIBOR_3M));

    assertEquals(
        test.requirements(USD_LIBOR_3M),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertEquals(
        test.requirements(ImmutableSet.of(USD_LIBOR_3M)),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(GBP_LIBOR_3M)));
  }

  public void test_of_map() {
    ImmutableMap<IborIndex, SwaptionVolatilitiesId> ids = ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1);
    SwaptionMarketDataLookup test = SwaptionMarketDataLookup.of(ids);
    assertEquals(test.queryType(), SwaptionMarketDataLookup.class);
    assertEquals(test.getVolatilityIndices(), ImmutableSet.of(USD_LIBOR_3M, USD_LIBOR_6M));
    assertEquals(test.getVolatilityIds(USD_LIBOR_3M), ImmutableSet.of(VOL_ID1));
    assertThrowsIllegalArg(() -> test.getVolatilityIds(GBP_LIBOR_3M));

    assertEquals(
        test.requirements(USD_LIBOR_3M),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertEquals(
        test.requirements(ImmutableSet.of(USD_LIBOR_3M)),
        FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(GBP_LIBOR_3M)));

    assertEquals(test.volatilities(USD_LIBOR_3M, MOCK_MARKET_DATA), MOCK_VOLS);
    assertThrowsIllegalArg(() -> test.volatilities(GBP_LIBOR_3M, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  public void test_marketDataView() {
    SwaptionMarketDataLookup test = SwaptionMarketDataLookup.of(USD_LIBOR_3M, VOL_ID1);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    SwaptionScenarioMarketData multiScenario = test.marketDataView(md);
    assertEquals(multiScenario.getLookup(), test);
    assertEquals(multiScenario.getMarketData(), md);
    assertEquals(multiScenario.getScenarioCount(), 1);
    SwaptionMarketData scenario = multiScenario.scenario(0);
    assertEquals(scenario.getLookup(), test);
    assertEquals(scenario.getMarketData(), md.scenario(0));
    assertEquals(scenario.getValuationDate(), valDate);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultSwaptionMarketDataLookup test =
        DefaultSwaptionMarketDataLookup.of(ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1));
    coverImmutableBean(test);
    DefaultSwaptionMarketDataLookup test2 = DefaultSwaptionMarketDataLookup.of(USD_LIBOR_3M, VOL_ID1);
    coverBeanEquals(test, test2);

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
  }

  public void test_serialization() {
    DefaultSwaptionMarketDataLookup test =
        DefaultSwaptionMarketDataLookup.of(ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1));
    assertSerialization(test);
  }

}
