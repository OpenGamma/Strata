/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.observable;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

@Test
public class QuoteTest {
  private static final QuoteId quoteId1 = QuoteId.of(StandardId.of("og", "id1"));

  public void test_of_QuoteId() throws Exception {
    Quote test = Quote.of(quoteId1, 1.234);
    assertEquals(test.getQuoteId(), quoteId1);
    assertEquals(test.getValue(), 1.234);
    assertEquals(test.toString(), "Quote{quoteId=QuoteId{standardId=og~id1, fieldName=MarketValue, observableSource=None}, value=1.234}");
  }


  public void test_of_Strings() throws Exception {
    Quote test = Quote.of("og", "id1", 1.2345);
    assertEquals(test.getQuoteId(), quoteId1);
    assertEquals(test.getValue(), 1.2345);
    assertEquals(test.toString(), "Quote{quoteId=QuoteId{standardId=og~id1, fieldName=MarketValue, observableSource=None}, value=1.2345}");
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Argument 'scheme' with value '' must match pattern:.+")
  public void test_of_EmptySchemeId() throws Exception {
    Quote test = Quote.of("", "notEmpty", 1.2345);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Argument 'value' with value '' must match pattern:.+")
  public void test_of_EmptyId() throws Exception {
    Quote test = Quote.of("notEmpty", "", 1.2345);
  }
}