/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Test {@link RateIndexCurveId}.
 */
@Test
public class RateIndexCurveIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_of_2args() {
    RateIndexCurveId test = RateIndexCurveId.of(GBP_SONIA, GROUP1);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getCurrency(), GBP_SONIA.getCurrency());
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  public void test_of_3args() {
    RateIndexCurveId test = RateIndexCurveId.of(GBP_SONIA, GROUP1, FEED);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getCurrency(), GBP_SONIA.getCurrency());
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateIndexCurveId test = RateIndexCurveId.of(GBP_SONIA, GROUP1);
    coverImmutableBean(test);
    RateIndexCurveId test2 = RateIndexCurveId.of(USD_FED_FUND, GROUP2, FEED);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RateIndexCurveId test = RateIndexCurveId.of(GBP_SONIA, GROUP1);
    assertSerialization(test);
  }

}
