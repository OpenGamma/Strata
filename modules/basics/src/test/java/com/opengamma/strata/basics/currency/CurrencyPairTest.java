/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.BHD;
import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CurrencyPair}.
 */
@Test
public class CurrencyPairTest {

  //-----------------------------------------------------------------------
  public void test_getAvailable() {
    Set<CurrencyPair> available = CurrencyPair.getAvailablePairs();
    assertTrue(available.contains(CurrencyPair.of(EUR, USD)));
    assertTrue(available.contains(CurrencyPair.of(EUR, GBP)));
    assertTrue(available.contains(CurrencyPair.of(GBP, USD)));
  }

  //-------------------------------------------------------------------------
  public void test_of_CurrencyCurrency() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertEquals(test.getBase(), GBP);
    assertEquals(test.getCounter(), USD);
    assertEquals(test.isIdentity(), false);
    assertEquals(test.toString(), "GBP/USD");
  }

  public void test_of_CurrencyCurrency_reverseStandardOrder() {
    CurrencyPair test = CurrencyPair.of(USD, GBP);
    assertEquals(test.getBase(), USD);
    assertEquals(test.getCounter(), GBP);
    assertEquals(test.isIdentity(), false);
    assertEquals(test.toString(), "USD/GBP");
  }

  public void test_of_CurrencyCurrency_same() {
    CurrencyPair test = CurrencyPair.of(USD, USD);
    assertEquals(test.getBase(), USD);
    assertEquals(test.getCounter(), USD);
    assertEquals(test.isIdentity(), true);
    assertEquals(test.toString(), "USD/USD");
  }

  public void test_of_CurrencyCurrency_null() {
    assertThrowsIllegalArg(() -> CurrencyPair.of(null, USD));
    assertThrowsIllegalArg(() -> CurrencyPair.of(USD, null));
    assertThrowsIllegalArg(() -> CurrencyPair.of(null, null));
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
  }

  public void test_contains_Currency_same() {
    CurrencyPair test = CurrencyPair.of(GBP, GBP);
    assertEquals(test.contains(GBP), true);
    assertEquals(test.contains(USD), false);
    assertEquals(test.contains(EUR), false);
  }

  public void test_contains_Currency_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThrowsIllegalArg(() -> test.contains(null));
  }

  //-------------------------------------------------------------------------
  public void test_isInverse_CurrencyPair() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertEquals(test.isInverse(test), false);
    assertEquals(test.isInverse(CurrencyPair.of(GBP, USD)), false);
    assertEquals(test.isInverse(CurrencyPair.of(USD, GBP)), true);
    assertEquals(test.isInverse(CurrencyPair.of(GBP, EUR)), false);
    assertEquals(test.isInverse(CurrencyPair.of(EUR, GBP)), false);
    assertEquals(test.isInverse(CurrencyPair.of(USD, EUR)), false);
    assertEquals(test.isInverse(CurrencyPair.of(EUR, USD)), false);
  }

  public void test_isInverse_CurrencyPair_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThrowsIllegalArg(() -> test.isInverse(null));
  }

  //-------------------------------------------------------------------------
  public void test_cross_CurrencyPair() {
    CurrencyPair gbpGbp = CurrencyPair.of(GBP, GBP);
    CurrencyPair gbpUsd = CurrencyPair.of(GBP, USD);
    CurrencyPair usdGbp = CurrencyPair.of(USD, GBP);
    CurrencyPair eurGbp = CurrencyPair.of(EUR, GBP);
    CurrencyPair eurUsd = CurrencyPair.of(EUR, USD);
    CurrencyPair usdEur = CurrencyPair.of(USD, EUR);

    assertEquals(gbpUsd.cross(gbpUsd), Optional.empty());
    assertEquals(gbpUsd.cross(usdGbp), Optional.empty());
    assertEquals(gbpGbp.cross(gbpUsd), Optional.empty());
    assertEquals(gbpUsd.cross(gbpGbp), Optional.empty());

    assertEquals(gbpUsd.cross(usdEur), Optional.of(eurGbp));
    assertEquals(gbpUsd.cross(eurUsd), Optional.of(eurGbp));
    assertEquals(usdGbp.cross(usdEur), Optional.of(eurGbp));
    assertEquals(usdGbp.cross(eurUsd), Optional.of(eurGbp));

    assertEquals(usdEur.cross(gbpUsd), Optional.of(eurGbp));
    assertEquals(usdEur.cross(usdGbp), Optional.of(eurGbp));
    assertEquals(eurUsd.cross(gbpUsd), Optional.of(eurGbp));
    assertEquals(eurUsd.cross(usdGbp), Optional.of(eurGbp));
  }

  public void test_cross_CurrencyPair_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThrowsIllegalArg(() -> test.cross(null));
  }

  //-----------------------------------------------------------------------
  public void test_isConventional() {
    assertEquals(CurrencyPair.of(GBP, USD).isConventional(), true);
    assertEquals(CurrencyPair.of(USD, GBP).isConventional(), false);
    // There is no configuration for GBP/BRL or BRL/GBP so the ordering list is used to choose a convention pair
    // GBP is in the currency order list and BRL isn't so GBP is the base
    assertEquals(CurrencyPair.of(GBP, BRL).isConventional(), true);
    assertEquals(CurrencyPair.of(BRL, GBP).isConventional(), false);
    // There is no configuration for BHD/BRL or BRL/BHD and neither are in the list specifying currency priority order.
    // Lexicographical ordering is used
    assertEquals(CurrencyPair.of(BHD, BRL).isConventional(), true);
    assertEquals(CurrencyPair.of(BRL, BHD).isConventional(), false);
    assertEquals(CurrencyPair.of(GBP, GBP).isConventional(), true);
  }

  public void test_toConventional() {
    assertEquals(CurrencyPair.of(GBP, USD).toConventional(), CurrencyPair.of(GBP, USD));
    assertEquals(CurrencyPair.of(USD, GBP).toConventional(), CurrencyPair.of(GBP, USD));

    assertEquals(CurrencyPair.of(GBP, BRL).toConventional(), CurrencyPair.of(GBP, BRL));
    assertEquals(CurrencyPair.of(BRL, GBP).toConventional(), CurrencyPair.of(GBP, BRL));

    assertEquals(CurrencyPair.of(BHD, BRL).toConventional(), CurrencyPair.of(BHD, BRL));
    assertEquals(CurrencyPair.of(BRL, BHD).toConventional(), CurrencyPair.of(BHD, BRL));
  }

  public void test_rateDigits() {
    assertEquals(CurrencyPair.of(GBP, USD).getRateDigits(), 4);
    assertEquals(CurrencyPair.of(USD, GBP).getRateDigits(), 4);
    assertEquals(CurrencyPair.of(BRL, GBP).getRateDigits(), 4);
    assertEquals(CurrencyPair.of(GBP, BRL).getRateDigits(), 4);
    assertEquals(CurrencyPair.of(BRL, BHD).getRateDigits(), 5);
    assertEquals(CurrencyPair.of(BHD, BRL).getRateDigits(), 5);
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

  public void test_jodaConvert() {
    assertJodaConvert(CurrencyPair.class, CurrencyPair.of(GBP, USD));
    assertJodaConvert(CurrencyPair.class, CurrencyPair.of(GBP, GBP));
  }

}
