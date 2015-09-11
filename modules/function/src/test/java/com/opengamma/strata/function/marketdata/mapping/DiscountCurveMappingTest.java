/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.key.DiscountCurveKey;

/**
 * Test {@link DiscountCurveMapping}.
 */
@Test
public class DiscountCurveMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountCurveMapping test = DiscountCurveMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), DiscountCurveKey.class);
    assertEquals(test.getIdForKey(DiscountCurveKey.of(GBP)), DiscountCurveId.of(GBP, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountCurveMapping test = DiscountCurveMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    DiscountCurveMapping test2 = DiscountCurveMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
