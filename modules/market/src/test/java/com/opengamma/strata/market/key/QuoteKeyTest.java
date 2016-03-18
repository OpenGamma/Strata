/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.id.QuoteId;

/**
 * Test {@link QuoteKey}.
 */
@Test
public class QuoteKeyTest {

  private static final StandardId ID = StandardId.of("Vendor", "Id");
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_of_noField() {
    QuoteKey test = QuoteKey.of(ID);
    assertEquals(test.getStandardId(), ID);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.getStandardId(), ID.getStandardId());
  }

  public void test_of_field() {
    QuoteKey test = QuoteKey.of(ID, FieldName.of("Foo"));
    assertEquals(test.getStandardId(), ID);
    assertEquals(test.getFieldName(), FieldName.of("Foo"));
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.getStandardId(), ID.getStandardId());
  }

  public void test_toObservableId() {
    QuoteKey test = QuoteKey.of(ID, FieldName.of("Foo"));
    QuoteId feed = test.toMarketDataId(FEED);
    assertEquals(feed.getStandardId(), ID);
    assertEquals(feed.getFieldName(), FieldName.of("Foo"));
    assertEquals(feed.getMarketDataType(), Double.class);
    assertEquals(feed.getStandardId(), ID.getStandardId());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    QuoteKey test = QuoteKey.of(ID);
    coverImmutableBean(test);
    QuoteKey test2 = QuoteKey.of(StandardId.of("A", "B"), FieldName.of("Foo"));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    QuoteKey test = QuoteKey.of(ID);
    assertSerialization(test);
  }

}
