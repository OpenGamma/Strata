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

import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Test {@link QuoteId}.
 */
@Test
public class QuoteIdTest {

  private static final StandardId ID1 = StandardId.of("OG-Ticker", "1");
  private static final StandardId ID2 = StandardId.of("OG-Ticker", "2");
  private static final MarketDataFeed FEED2 = MarketDataFeed.of("Feed2");
  private static final FieldName FIELD2 = FieldName.of("Field2");

  //-------------------------------------------------------------------------
  public void test_of_1arg() {
    QuoteId test = QuoteId.of(ID1);
    assertEquals(test.getStandardId(), ID1);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.toObservableKey(), QuoteKey.of(ID1, FieldName.MARKET_VALUE));
  }

  public void test_of_2args() {
    QuoteId test = QuoteId.of(ID1, FEED2);
    assertEquals(test.getStandardId(), ID1);
    assertEquals(test.getMarketDataFeed(), FEED2);
    assertEquals(test.getFieldName(), FieldName.MARKET_VALUE);
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.toObservableKey(), QuoteKey.of(ID1, FieldName.MARKET_VALUE));
  }

  public void test_of_3args() {
    QuoteId test = QuoteId.of(ID1, FEED2, FIELD2);
    assertEquals(test.getStandardId(), ID1);
    assertEquals(test.getMarketDataFeed(), FEED2);
    assertEquals(test.getFieldName(), FIELD2);
    assertEquals(test.getMarketDataType(), Double.class);
    assertEquals(test.toObservableKey(), QuoteKey.of(ID1, FIELD2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    QuoteId test = QuoteId.of(ID1);
    coverImmutableBean(test);
    QuoteId test2 = QuoteId.of(ID2, FEED2, FIELD2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    QuoteId test = QuoteId.of(ID1);
    assertSerialization(test);
  }

}
