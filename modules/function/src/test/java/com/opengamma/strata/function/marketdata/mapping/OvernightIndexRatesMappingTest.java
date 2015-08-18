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
import com.opengamma.strata.market.id.OvernightIndexRatesId;
import com.opengamma.strata.market.key.OvernightIndexRatesKey;

/**
 * Test {@link OvernightIndexRatesMapping}.
 */
@Test
public class OvernightIndexRatesMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIndexRatesMapping test = OvernightIndexRatesMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), OvernightIndexRatesKey.class);
    assertEquals(test.getIdForKey(OvernightIndexRatesKey.of(GBP_SONIA)), OvernightIndexRatesId.of(GBP_SONIA, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndexRatesMapping test = OvernightIndexRatesMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    OvernightIndexRatesMapping test2 = OvernightIndexRatesMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
