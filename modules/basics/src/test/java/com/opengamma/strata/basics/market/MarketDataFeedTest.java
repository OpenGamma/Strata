/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link MarketDataFeed}.
 */
@Test
public class MarketDataFeedTest {

  //-----------------------------------------------------------------------
  public void coverage() {
    MarketDataFeed test = MarketDataFeed.of("Foo");
    assertEquals(test.toString(), "Foo");
    assertSerialization(test);
    assertJodaConvert(MarketDataFeed.class, test);
  }

}
