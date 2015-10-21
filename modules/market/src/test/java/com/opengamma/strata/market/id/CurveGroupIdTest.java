/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Test {@link CurveGroupId}.
 */
@Test
public class CurveGroupIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of_1arg() {
    CurveGroupId test = CurveGroupId.of(GROUP1);
    assertEquals(test.getName(), GROUP1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), CurveGroup.class);
  }

  public void test_of_2args() {
    CurveGroupId test = CurveGroupId.of(GROUP1, MarketDataFeed.NONE);
    assertEquals(test.getName(), GROUP1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), CurveGroup.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupId test = CurveGroupId.of(GROUP1);
    coverImmutableBean(test);
    CurveGroupId test2 = CurveGroupId.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupId test = CurveGroupId.of(GROUP1);
    assertSerialization(test);
  }

}
