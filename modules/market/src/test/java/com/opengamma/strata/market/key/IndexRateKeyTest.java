/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_12M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.market.id.IndexRateId;

/**
 * Test {@link IndexRateKey}.
 */
@Test
public class IndexRateKeyTest {

  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_of_noField() {
    IndexRateKey test = IndexRateKey.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.getStandardId(), GBP_LIBOR_3M.getStandardId());
  }

  public void test_of_field() {
    IndexRateKey test = IndexRateKey.of(GBP_LIBOR_3M, FieldName.of("Foo"));
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getFieldName(), FieldName.of("Foo"));
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.getStandardId(), GBP_LIBOR_3M.getStandardId());
  }

  public void test_toObservableId() {
    IndexRateKey test = IndexRateKey.of(GBP_LIBOR_3M, FieldName.of("Foo"));
    IndexRateId feed = test.toMarketDataId(FEED);
    assertEquals(feed.getIndex(), GBP_LIBOR_3M);
    assertEquals(feed.getFieldName(), FieldName.of("Foo"));
    assertEquals(feed.getMarketDataType(), Double.class);
    assertEquals(feed.getStandardId(), GBP_LIBOR_3M.getStandardId());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IndexRateKey test = IndexRateKey.of(GBP_LIBOR_3M);
    coverImmutableBean(test);
    IndexRateKey test2 = IndexRateKey.of(CHF_LIBOR_12M, FieldName.of("Foo"));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IndexRateKey test = IndexRateKey.of(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
