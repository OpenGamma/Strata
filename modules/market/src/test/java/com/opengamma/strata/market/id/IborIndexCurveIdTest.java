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
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Test {@link IborIndexCurveId}.
 */
@Test
public class IborIndexCurveIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_of_2args() {
    IborIndexCurveId test = IborIndexCurveId.of(GBP_LIBOR_3M, GROUP1);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getCurrency(), GBP_LIBOR_3M.getCurrency());
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  public void test_of_3args() {
    IborIndexCurveId test = IborIndexCurveId.of(GBP_LIBOR_3M, GROUP1, FEED);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getCurrency(), GBP_LIBOR_3M.getCurrency());
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndexCurveId test = IborIndexCurveId.of(GBP_LIBOR_3M, GROUP1);
    coverImmutableBean(test);
    IborIndexCurveId test2 = IborIndexCurveId.of(USD_LIBOR_3M, GROUP2, FEED);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborIndexCurveId test = IborIndexCurveId.of(GBP_LIBOR_3M, GROUP1);
    assertSerialization(test);
  }

}
