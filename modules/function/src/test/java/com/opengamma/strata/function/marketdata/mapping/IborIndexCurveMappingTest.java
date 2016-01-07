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
import com.opengamma.strata.market.id.IborIndexCurveId;
import com.opengamma.strata.market.key.IborIndexCurveKey;

/**
 * Test {@link IborIndexCurveMapping}.
 */
@Test
public class IborIndexCurveMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    IborIndexCurveMapping test = IborIndexCurveMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), IborIndexCurveKey.class);
    assertEquals(test.getIdForKey(IborIndexCurveKey.of(GBP_LIBOR_3M)), IborIndexCurveId.of(GBP_LIBOR_3M, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndexCurveMapping test = IborIndexCurveMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    IborIndexCurveMapping test2 = IborIndexCurveMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
