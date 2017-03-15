/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.NamedMarketDataId;

/**
 * Test {@link MarketDataFilter}.
 */
@Test
public class MarketDataFilterTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_ofIdType() {
    MarketDataFilter<String, MarketDataId<String>> test = MarketDataFilter.ofIdType(TestId.class);
    assertEquals(test.getMarketDataIdType(), TestId.class);
    assertTrue(test.matches(new TestId("a"), null, REF_DATA));
  }

  public void test_ofId() {
    MarketDataFilter<String, MarketDataId<String>> test = MarketDataFilter.ofId(new TestId("a"));
    assertEquals(test.getMarketDataIdType(), TestId.class);
    assertTrue(test.matches(new TestId("a"), null, REF_DATA));
    assertFalse(test.matches(new TestId("b"), null, REF_DATA));
  }

  public void test_ofName() {
    MarketDataFilter<String, NamedMarketDataId<String>> test = MarketDataFilter.ofName(new TestingName("a"));
    assertEquals(test.getMarketDataIdType(), NamedMarketDataId.class);
    assertTrue(test.matches(new TestingNamedId("a"), null, REF_DATA));
    assertFalse(test.matches(new TestingNamedId("b"), null, REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    MarketDataFilter<String, MarketDataId<String>> test1 = MarketDataFilter.ofIdType(TestId.class);
    coverImmutableBean((ImmutableBean) test1);
    MarketDataFilter<String, MarketDataId<String>> test2 = MarketDataFilter.ofId(new TestId("a"));
    coverImmutableBean((ImmutableBean) test2);
    MarketDataFilter<String, NamedMarketDataId<String>> test3 = MarketDataFilter.ofName(new TestingName("a"));
    coverImmutableBean((ImmutableBean) test3);
  }

}
