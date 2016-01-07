/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.OvernightIndexCurveId;
import com.opengamma.strata.market.key.OvernightIndexCurveKey;

/**
 * Test {@link OvernightIndexCurveMapping}.
 */
@Test
public class OvernightIndexCurveMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIndexCurveMapping test = OvernightIndexCurveMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), OvernightIndexCurveKey.class);
    assertEquals(test.getIdForKey(OvernightIndexCurveKey.of(GBP_SONIA)), OvernightIndexCurveId.of(GBP_SONIA, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndexCurveMapping test = OvernightIndexCurveMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    OvernightIndexCurveMapping test2 = OvernightIndexCurveMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
