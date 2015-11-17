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
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurveInputsId}.
 */
@Test
public class CurveInputsIdTest {

  private static final CurveGroupName GROUP1 = CurveGroupName.of("Group1");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final CurveName NAME1 = CurveName.of("Name1");
  private static final CurveName NAME2 = CurveName.of("Name2");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveInputsId test = CurveInputsId.of(GROUP1, NAME1, MarketDataFeed.NONE);
    assertEquals(test.getCurveGroupName(), GROUP1);
    assertEquals(test.getCurveName(), NAME1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), CurveInputs.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveInputsId test = CurveInputsId.of(GROUP1, NAME1, MarketDataFeed.NONE);
    coverImmutableBean(test);
    CurveInputsId test2 = CurveInputsId.of(GROUP2, NAME2, FEED2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveInputsId test = CurveInputsId.of(GROUP1, NAME1, MarketDataFeed.NONE);
    assertSerialization(test);
  }

}
