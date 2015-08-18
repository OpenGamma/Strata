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
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.value.OvernightIndexRates;

/**
 * Test {@link OvernightIndexRatesId}.
 */
@Test
public class OvernightIndexRatesIdTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIndexRatesId test = OvernightIndexRatesId.of(GBP_SONIA, GROUP, FEED);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataType(), OvernightIndexRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndexRatesId test = OvernightIndexRatesId.of(GBP_SONIA, GROUP, FEED);
    coverImmutableBean(test);
    OvernightIndexRatesId test2 = OvernightIndexRatesId.of(USD_FED_FUND, GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightIndexRatesId test = OvernightIndexRatesId.of(GBP_SONIA, GROUP, FEED);
    assertSerialization(test);
  }

}
