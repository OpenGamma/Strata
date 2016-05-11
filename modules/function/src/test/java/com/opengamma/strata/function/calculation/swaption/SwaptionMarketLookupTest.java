/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.market.id.SwaptionVolatilitiesId;
import com.opengamma.strata.market.view.SwaptionVolatilities;

/**
 * Test {@link SwaptionMarketLookup}.
 */
@Test
public class SwaptionMarketLookupTest {

  private static final SwaptionVolatilitiesId VOL_ID1 = SwaptionVolatilitiesId.of("USD1");
  private static final SwaptionVolatilities MOCK_VOLS = mock(SwaptionVolatilities.class);
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final CalculationMarketData MOCK_CALC_MARKET_DATA = mock(CalculationMarketData.class);

  static {
    when(MOCK_MARKET_DATA.getValue(VOL_ID1)).thenReturn(MOCK_VOLS);
  }

  //-------------------------------------------------------------------------
  public void test_of_single() {
    ImmutableMap<IborIndex, SwaptionVolatilitiesId> ids = ImmutableMap.of(USD_LIBOR_3M, VOL_ID1);
    SwaptionMarketLookup test = SwaptionMarketLookup.of(ids);
    assertEquals(test.queryType(), SwaptionMarketLookup.class);
    assertEquals(test.getVolatilityIndices(), ImmutableSet.of(USD_LIBOR_3M));
    assertEquals(test.getVolatilityIds(USD_LIBOR_3M), ImmutableSet.of(VOL_ID1));
    assertThrowsIllegalArg(() -> test.getVolatilityIds(GBP_LIBOR_3M));
    assertEquals(
        test.requirements(ImmutableSet.of(USD_LIBOR_3M)),
        FunctionRequirements.builder().singleValueRequirements(VOL_ID1).build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(GBP_LIBOR_3M)));
  }

  public void test_of_map() {
    ImmutableMap<IborIndex, SwaptionVolatilitiesId> ids = ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1);
    SwaptionMarketLookup test = SwaptionMarketLookup.of(ids);
    assertEquals(test.queryType(), SwaptionMarketLookup.class);
    assertEquals(test.getVolatilityIndices(), ImmutableSet.of(USD_LIBOR_3M, USD_LIBOR_6M));
    assertEquals(test.getVolatilityIds(USD_LIBOR_3M), ImmutableSet.of(VOL_ID1));
    assertThrowsIllegalArg(() -> test.getVolatilityIds(GBP_LIBOR_3M));

    assertEquals(
        test.requirements(ImmutableSet.of(USD_LIBOR_3M)),
        FunctionRequirements.builder().singleValueRequirements(VOL_ID1).build());
    assertThrowsIllegalArg(() -> test.requirements(ImmutableSet.of(GBP_LIBOR_3M)));

    assertEquals(test.volatilities(USD_LIBOR_3M, MOCK_MARKET_DATA), MOCK_VOLS);
    assertThrowsIllegalArg(() -> test.volatilities(GBP_LIBOR_3M, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultSwaptionMarketLookup test = DefaultSwaptionMarketLookup.of(ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1));
    coverImmutableBean(test);
    DefaultSwaptionMarketLookup test2 = DefaultSwaptionMarketLookup.of(USD_LIBOR_3M, VOL_ID1);
    coverBeanEquals(test, test2);

    coverImmutableBean((ImmutableBean) test.marketView(MOCK_CALC_MARKET_DATA));
    coverImmutableBean((ImmutableBean) test.marketView(MOCK_MARKET_DATA));
  }

  public void test_serialization() {
    DefaultSwaptionMarketLookup test = DefaultSwaptionMarketLookup.of(ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1));
    assertSerialization(test);
  }

}
