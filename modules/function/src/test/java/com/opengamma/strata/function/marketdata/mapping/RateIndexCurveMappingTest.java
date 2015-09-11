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
import com.opengamma.strata.market.id.RateIndexCurveId;
import com.opengamma.strata.market.key.RateIndexCurveKey;

/**
 * Test {@link RateIndexCurveMapping}.
 */
@Test
public class RateIndexCurveMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    RateIndexCurveMapping test = RateIndexCurveMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), RateIndexCurveKey.class);
    assertEquals(test.getIdForKey(RateIndexCurveKey.of(GBP_LIBOR_3M)), RateIndexCurveId.of(GBP_LIBOR_3M, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateIndexCurveMapping test = RateIndexCurveMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    RateIndexCurveMapping test2 = RateIndexCurveMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
