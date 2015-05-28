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
import com.opengamma.strata.market.id.ZeroRateDiscountFactorsId;
import com.opengamma.strata.market.key.DiscountFactorsKey;

/**
 * Test {@link ZeroRateDiscountFactorsMapping}.
 */
@Test
public class ZeroRateDiscountFactorsMappingTest {

  private static final CurveGroupName GROUP = CurveGroupName.of("Group");
  private static final CurveGroupName GROUP2 = CurveGroupName.of("Group2");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");

  //-------------------------------------------------------------------------
  public void test_of() {
    ZeroRateDiscountFactorsMapping test = ZeroRateDiscountFactorsMapping.of(GROUP, FEED);
    assertEquals(test.getCurveGroupName(), GROUP);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataKeyType(), DiscountFactorsKey.class);
    assertEquals(test.getIdForKey(DiscountFactorsKey.of(GBP)), ZeroRateDiscountFactorsId.of(GBP, GROUP, FEED));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ZeroRateDiscountFactorsMapping test = ZeroRateDiscountFactorsMapping.of(GROUP, FEED);
    coverImmutableBean(test);
    ZeroRateDiscountFactorsMapping test2 = ZeroRateDiscountFactorsMapping.of(GROUP2, FEED2);
    coverBeanEquals(test, test2);
  }

}
