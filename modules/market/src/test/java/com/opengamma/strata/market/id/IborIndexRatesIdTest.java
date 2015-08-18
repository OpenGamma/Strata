/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.value.IborIndexRates;

/**
 * Test {@link IborIndexRatesId}.
 */
@Test
public class IborIndexRatesIdTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    IborIndexRatesId test = IborIndexRatesId.of(GBP_LIBOR_3M, GROUP, FEED);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataType(), IborIndexRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndexRatesId test = IborIndexRatesId.of(GBP_LIBOR_3M, GROUP, FEED);
    coverImmutableBean(test);
    IborIndexRatesId test2 = IborIndexRatesId.of(USD_LIBOR_3M, GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborIndexRatesId test = IborIndexRatesId.of(GBP_LIBOR_3M, GROUP, FEED);
    assertSerialization(test);
  }

}
