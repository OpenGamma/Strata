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
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.ParRates;

/**
 * Test {@link ParRatesId}.
 */
@Test
public class ParRatesIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final CurveName NAME1 = CurveName.of("Name1");
  private static final CurveName NAME2 = CurveName.of("Name2");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    ParRatesId test = ParRatesId.of(GROUP1, NAME1, MarketDataFeed.NONE);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getCurveName(), NAME1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), ParRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParRatesId test = ParRatesId.of(GROUP1, NAME1, MarketDataFeed.NONE);
    coverImmutableBean(test);
    ParRatesId test2 = ParRatesId.of(GROUP2, NAME2, FEED2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ParRatesId test = ParRatesId.of(GROUP1, NAME1, MarketDataFeed.NONE);
    assertSerialization(test);
  }

}
