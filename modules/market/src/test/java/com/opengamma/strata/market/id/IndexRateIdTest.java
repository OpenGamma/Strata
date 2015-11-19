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

import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.key.IndexRateKey;

/**
 * Test {@link IndexRateId}.
 */
@Test
public class IndexRateIdTest {

  private static final FieldName FIELD = FieldName.of("Field");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_of_1arg() {
    IndexRateId test = IndexRateId.of(GBP_SONIA);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getStandardId(), GBP_SONIA.getStandardId());
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.toMarketDataKey(), IndexRateKey.of(GBP_SONIA, FieldName.MARKET_VALUE));
  }

  public void test_of_2args() {
    IndexRateId test = IndexRateId.of(GBP_SONIA, FEED);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getStandardId(), GBP_SONIA.getStandardId());
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.toMarketDataKey(), IndexRateKey.of(GBP_SONIA, FieldName.MARKET_VALUE));
  }

  public void test_of_3args() {
    IndexRateId test = IndexRateId.of(GBP_SONIA, FEED, FIELD);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getFieldName(), FIELD);
    assertEquals(test.getStandardId(), GBP_SONIA.getStandardId());
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.toMarketDataKey(), IndexRateKey.of(GBP_SONIA, FIELD));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IndexRateId test = IndexRateId.of(GBP_SONIA);
    coverImmutableBean(test);
    IndexRateId test2 = IndexRateId.of(USD_FED_FUND, FEED, FIELD);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IndexRateId test = IndexRateId.of(GBP_SONIA);
    assertSerialization(test);
  }

}
