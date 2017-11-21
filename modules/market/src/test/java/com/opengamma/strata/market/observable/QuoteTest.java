/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.observable;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link Quote}.
 */
@Test
public class QuoteTest {
  private static final QuoteId QUOTE_ID_1 = QuoteId.of(StandardId.of("og", "id1"));

  public void test_of_QuoteId() throws Exception {
    Quote test = Quote.of(QUOTE_ID_1, 1.234);
    assertEquals(test.getQuoteId(), QUOTE_ID_1);
    assertEquals(test.getValue(), 1.234);
  }

  public void test_of_nullQuoteId() throws Exception {
    assertThrowsIllegalArg(() -> Quote.of(null, 1.2345));
  }

  public void coverage() {
    Quote test = Quote.of(QUOTE_ID_1, 1.234);
    coverImmutableBean(test);
    Quote test2 = Quote.of(QuoteId.of(StandardId.of("a", "b")), 4.321);
    coverBeanEquals(test, test2);
  }

}
