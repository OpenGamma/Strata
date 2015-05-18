/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link Currency}.
 */
@Test
public class CurrencyTest {

  //-----------------------------------------------------------------------
  public void test_constants() {
    assertEquals(Currency.of("USD"), Currency.USD);
    assertEquals(Currency.of("EUR"), Currency.EUR);
    assertEquals(Currency.of("JPY"), Currency.JPY);
    assertEquals(Currency.of("GBP"), Currency.GBP);
    assertEquals(Currency.of("CHF"), Currency.CHF);
    assertEquals(Currency.of("AUD"), Currency.AUD);
    assertEquals(Currency.of("CAD"), Currency.CAD);
  }

  //-----------------------------------------------------------------------
  public void test_getAvailable() {
    Set<Currency> available = Currency.getAvailableCurrencies();
    assertTrue(available.contains(Currency.USD));
    assertTrue(available.contains(Currency.EUR));
    assertTrue(available.contains(Currency.JPY));
    assertTrue(available.contains(Currency.GBP));
    assertTrue(available.contains(Currency.CHF));
    assertTrue(available.contains(Currency.AUD));
    assertTrue(available.contains(Currency.CAD));
  }

  //-----------------------------------------------------------------------
  public void test_of_String() {
    Currency test = Currency.of("SEK");
    assertEquals(test.getCode(), "SEK");
    assertSame(test, Currency.of("SEK"));
  }

  public void test_of_String_historicCurrency() {
    Currency test = Currency.of("BEF");
    assertEquals(test.getCode(), "BEF");
    assertEquals(test.getMinorUnitDigits(), 2);
    assertEquals(test.getTriangulationCurrency(), Currency.EUR);
    assertSame(test, Currency.of("BEF"));
  }

  public void test_of_String_unknownCurrencyCreated() {
    Currency test = Currency.of("AAA");
    assertEquals(test.getCode(), "AAA");
    assertEquals(test.getMinorUnitDigits(), 0);
    assertSame(test, Currency.of("AAA"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_String_lowerCase() {
    Currency.of("gbp");
  }

  @DataProvider(name = "ofBad")
  Object[][] data_ofBad() {
    return new Object[][] {
        {""},
        {"AB"},
        {"gbp"},
        {"ABCD"},
        {"123"},
        {" GBP"},
        {null},
    };
  }

  @Test(dataProvider = "ofBad", expectedExceptions = IllegalArgumentException.class)
  public void test_of_String_bad(String input) {
    Currency.of(input);
  }

  //-----------------------------------------------------------------------
  public void test_parse_String() {
    Currency test = Currency.parse("GBP");
    assertEquals(test.getCode(), "GBP");
    assertSame(test, Currency.GBP);
  }

  public void test_parse_String_unknownCurrencyCreated() {
    Currency test = Currency.parse("zyx");
    assertEquals(test.getCode(), "ZYX");
    assertEquals(test.getMinorUnitDigits(), 0);
    assertSame(test, Currency.of("ZYX"));
  }

  public void test_parse_String_lowerCase() {
    Currency test = Currency.parse("gbp");
    assertEquals(test.getCode(), "GBP");
    assertSame(test, Currency.GBP);
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"AB"},
        {"ABCD"},
        {"123"},
        {" GBP"},
        {null},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_bad(String input) {
    Currency.parse(input);
  }

  //-----------------------------------------------------------------------
  public void test_minorUnits() {
    assertEquals(Currency.of("USD").getMinorUnitDigits(), 2);
    assertEquals(Currency.of("EUR").getMinorUnitDigits(), 2);
    assertEquals(Currency.of("JPY").getMinorUnitDigits(), 0);
    assertEquals(Currency.of("GBP").getMinorUnitDigits(), 2);
    assertEquals(Currency.of("CHF").getMinorUnitDigits(), 2);
    assertEquals(Currency.of("AUD").getMinorUnitDigits(), 2);
    assertEquals(Currency.of("CAD").getMinorUnitDigits(), 2);
  }

  public void test_triangulatonCurrency() {
    assertEquals(Currency.of("USD").getTriangulationCurrency(), Currency.USD);
    assertEquals(Currency.of("EUR").getTriangulationCurrency(), Currency.USD);
    assertEquals(Currency.of("JPY").getTriangulationCurrency(), Currency.USD);
    assertEquals(Currency.of("GBP").getTriangulationCurrency(), Currency.USD);
    assertEquals(Currency.of("CHF").getTriangulationCurrency(), Currency.USD);
    assertEquals(Currency.of("AUD").getTriangulationCurrency(), Currency.USD);
    assertEquals(Currency.of("CAD").getTriangulationCurrency(), Currency.USD);
  }

  //-----------------------------------------------------------------------
  public void test_compareTo() {
    Currency a = Currency.EUR;
    Currency b = Currency.GBP;
    Currency c = Currency.JPY;
    assertEquals(0, a.compareTo(a));
    assertEquals(0, b.compareTo(b));
    assertEquals(0, c.compareTo(c));

    assertTrue(a.compareTo(b) < 0);
    assertTrue(b.compareTo(a) > 0);

    assertTrue(a.compareTo(c) < 0);
    assertTrue(c.compareTo(a) > 0);

    assertTrue(b.compareTo(c) < 0);
    assertTrue(c.compareTo(b) > 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_compareTo_null() {
    Currency.EUR.compareTo(null);
  }

  //-----------------------------------------------------------------------
  public void test_equals_hashCode() {
    Object a1 = Currency.GBP;
    Object a2 = Currency.of("GBP");
    Object b = Currency.EUR;
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(a2), true);

    assertEquals(a2.equals(a1), true);
    assertEquals(a2.equals(a2), true);
    assertEquals(a2.equals(b), false);

    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(a2), false);
    assertEquals(b.equals(b), true);

    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_equals_bad() {
    Object a = Currency.GBP;
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("String"), false);
    assertEquals(a.equals(new Object()), false);
  }

  //-----------------------------------------------------------------------
  public void test_toString() {
    Currency test = Currency.GBP;
    assertEquals("GBP", test.toString());
  }

  //-----------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(CurrencyDataLoader.class);
  }

  public void test_serialization() {
    assertSerialization(Currency.GBP);
  }

  public void test_jodaConvert() {
    assertJodaConvert(Currency.class, Currency.GBP);
  }

}
