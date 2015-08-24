/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.IborIndexRatesId;
import com.opengamma.strata.market.key.IborIndexRatesKey;

/**
 * Test {@link IborIndexRatesMapping}.
 */
@Test
public class IborIndexRatesMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    IborIndexRatesMapping test = IborIndexRatesMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), IborIndexRatesKey.class);
    assertEquals(test.getIdForKey(IborIndexRatesKey.of(GBP_LIBOR_3M)), IborIndexRatesId.of(GBP_LIBOR_3M, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndexRatesMapping test = IborIndexRatesMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    IborIndexRatesMapping test2 = IborIndexRatesMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
