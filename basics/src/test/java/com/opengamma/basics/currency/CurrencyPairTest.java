/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.currency;

import static com.opengamma.basics.currency.Currency.AUD;
import static com.opengamma.basics.currency.Currency.CAD;
import static com.opengamma.basics.currency.Currency.EUR;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CurrencyPair}.
 */
@Test
public class CurrencyPairTest {

  //-------------------------------------------------------------------------
  public void test_of_CurrencyCurrency() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertEquals(test.getBase(), GBP);
    assertEquals(test.getCounter(), USD);
    assertEquals(test.toString(), "GBP/USD");
  }

  public void test_of_CurrencyCurrency_reverseStandardOrder() {
    CurrencyPair test = CurrencyPair.of(USD, GBP);
    assertEquals(test.getBase(), USD);
    assertEquals(test.getCounter(), GBP);
    assertEquals(test.toString(), "USD/GBP");
  }

  public void test_of_CurrencyCurrency_same() {
    CurrencyPair test = CurrencyPair.of(USD, USD);
    assertEquals(test.getBase(), USD);
    assertEquals(test.getCounter(), USD);
    assertEquals(test.toString(), "USD/USD");
  }

  public void test_of_CurrencyCurrency_null() {
    assertThrows(() -> CurrencyPair.of((Currency) null, USD), IllegalArgumentException.class);
    assertThrows(() -> CurrencyPair.of(USD, (Currency) null), IllegalArgumentException.class);
    assertThrows(() -> CurrencyPair.of((Currency) null, (Currency) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"USD/EUR", USD, EUR},
        {"EUR/USD", EUR, USD},
        {"EUR/EUR", EUR, EUR},
        {"cAd/GbP", CAD, GBP},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good(String input, Currency base, Currency counter) {
    assertEquals(CurrencyPair.parse(input), CurrencyPair.of(base, counter));
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
      {"AUD"},
      {"AUD/GB"},
      {"AUD GBP"},
      {"AUD:GBP"},
      {"123/456"},
      {""},
      {null},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_bad(String input) {
    CurrencyPair.parse(input);
  }

  //-------------------------------------------------------------------------
  public void test_inverse() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertEquals(test.inverse(), CurrencyPair.of(USD, GBP));
  }

  public void test_inverse_same() {
    CurrencyPair test = CurrencyPair.of(GBP, GBP);
    assertEquals(test.inverse(), CurrencyPair.of(GBP, GBP));
  }

  //-------------------------------------------------------------------------
  public void test_contains_Currency() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertEquals(test.contains(GBP), true);
    assertEquals(test.contains(USD), true);
    assertEquals(test.contains(EUR), false);
    assertEquals(test.contains(null), false);
  }

  public void test_contains_Currency_same() {
    CurrencyPair test = CurrencyPair.of(GBP, GBP);
    assertEquals(test.contains(GBP), true);
    assertEquals(test.contains(USD), false);
    assertEquals(test.contains(EUR), false);
  }

  //-------------------------------------------------------------------------
  public void test_equals_hashCode() {
    CurrencyPair a1 = CurrencyPair.of(AUD, GBP);
    CurrencyPair a2 = CurrencyPair.of(AUD, GBP);
    CurrencyPair b = CurrencyPair.of(USD, GBP);
    CurrencyPair c = CurrencyPair.of(USD, EUR);
    
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    
    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(a2), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);
    
    assertEquals(c.equals(a1), false);
    assertEquals(c.equals(a2), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);
    
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_equals_bad() {
    CurrencyPair test = CurrencyPair.of(AUD, GBP);
    assertFalse(test.equals(""));
    assertFalse(test.equals(null));
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(CurrencyPair.of(GBP, USD));
    assertSerialization(CurrencyPair.of(GBP, GBP));
  }

}
