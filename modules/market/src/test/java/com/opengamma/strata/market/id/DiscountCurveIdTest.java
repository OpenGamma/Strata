/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Test {@link DiscountCurveId}.
 */
@Test
public class DiscountCurveIdTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountCurveId test = DiscountCurveId.of(GBP, GROUP, FEED);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  public void test_of_noFeed() {
    DiscountCurveId test = DiscountCurveId.of(GBP, GROUP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountCurveId test = DiscountCurveId.of(GBP, GROUP, FEED);
    coverImmutableBean(test);
    DiscountCurveId test2 = DiscountCurveId.of(USD, GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    DiscountCurveId test = DiscountCurveId.of(GBP, GROUP, FEED);
    assertSerialization(test);
  }

}
