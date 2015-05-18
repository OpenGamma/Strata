/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.location;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Country.
 */
@Test
public class CountryTest {

  //-----------------------------------------------------------------------
  public void test_constants() {
    assertEquals(Country.of("EU"), Country.EU);
    assertEquals(Country.of("BE"), Country.BE);
    assertEquals(Country.of("CH"), Country.CH);
    assertEquals(Country.of("CZ"), Country.CZ);
    assertEquals(Country.of("DE"), Country.DE);
    assertEquals(Country.of("DK"), Country.DK);
    assertEquals(Country.of("ES"), Country.ES);
    assertEquals(Country.of("FI"), Country.FI);
    assertEquals(Country.of("FR"), Country.FR);
    assertEquals(Country.of("GB"), Country.GB);
    assertEquals(Country.of("GR"), Country.GR);
    assertEquals(Country.of("HU"), Country.HU);
    assertEquals(Country.of("IS"), Country.IS);
    assertEquals(Country.of("IT"), Country.IT);
    assertEquals(Country.of("LU"), Country.LU);
    assertEquals(Country.of("NL"), Country.NL);
    assertEquals(Country.of("NO"), Country.NO);
    assertEquals(Country.of("PL"), Country.PL);
    assertEquals(Country.of("PT"), Country.PT);
    assertEquals(Country.of("SE"), Country.SE);
    assertEquals(Country.of("SK"), Country.SK);
    assertEquals(Country.of("TR"), Country.TR);

    assertEquals(Country.of("AR"), Country.AR);
    assertEquals(Country.of("BR"), Country.BR);
    assertEquals(Country.of("CA"), Country.CA);
    assertEquals(Country.of("CL"), Country.CL);
    assertEquals(Country.of("MX"), Country.MX);
    assertEquals(Country.of("US"), Country.US);

    assertEquals(Country.of("AU"), Country.AU);
    assertEquals(Country.of("CN"), Country.CN);
    assertEquals(Country.of("EG"), Country.EG);
    assertEquals(Country.of("HK"), Country.HK);
    assertEquals(Country.of("ID"), Country.ID);
    assertEquals(Country.of("IL"), Country.IL);
    assertEquals(Country.of("IN"), Country.IN);
    assertEquals(Country.of("JP"), Country.JP);
    assertEquals(Country.of("KR"), Country.KR);
    assertEquals(Country.of("MY"), Country.MY);
    assertEquals(Country.of("NZ"), Country.NZ);
    assertEquals(Country.of("RU"), Country.RU);
    assertEquals(Country.of("SA"), Country.SA);
    assertEquals(Country.of("SG"), Country.SG);
    assertEquals(Country.of("TH"), Country.TH);
    assertEquals(Country.of("ZA"), Country.ZA);
  }

  //-----------------------------------------------------------------------
  public void test_getAvailable() {
    Set<Country> available = Country.getAvailableCountries();
    assertTrue(available.contains(Country.US));
    assertTrue(available.contains(Country.EU));
    assertTrue(available.contains(Country.JP));
    assertTrue(available.contains(Country.GB));
    assertTrue(available.contains(Country.CH));
    assertTrue(available.contains(Country.AU));
    assertTrue(available.contains(Country.CA));
  }

  //-----------------------------------------------------------------------
  public void test_of_String() {
    Country test = Country.of("SE");
    assertEquals(test.getCode(), "SE");
    assertSame(test, Country.of("SE"));
  }

  public void test_of_String_unknownCountryCreated() {
    Country test = Country.of("AA");
    assertEquals(test.getCode(), "AA");
    assertSame(test, Country.of("AA"));
  }

  @DataProvider(name = "ofBad")
  Object[][] data_ofBad() {
    return new Object[][] {
        {""},
        {"A"},
        {"gb"},
        {"ABC"},
        {"123"},
        {" GB"},
        {null},
    };
  }

  @Test(dataProvider = "ofBad", expectedExceptions = IllegalArgumentException.class)
  public void test_of_String_bad(String input) {
    Country.of(input);
  }

  //-----------------------------------------------------------------------
  public void test_parse_String() {
    Country test = Country.parse("GB");
    assertEquals(test.getCode(), "GB");
    assertSame(test, Country.GB);
  }

  public void test_parse_String_unknownCountryCreated() {
    Country test = Country.parse("zy");
    assertEquals(test.getCode(), "ZY");
    assertSame(test, Country.of("ZY"));
  }

  public void test_parse_String_lowerCase() {
    Country test = Country.parse("gb");
    assertEquals(test.getCode(), "GB");
    assertSame(test, Country.GB);
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"A"},
        {"ABC"},
        {"123"},
        {" GB"},
        {null},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_bad(String input) {
    Country.parse(input);
  }

  //-----------------------------------------------------------------------
  public void test_compareTo() {
    Country a = Country.EU;
    Country b = Country.GB;
    Country c = Country.JP;
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
    Country.EU.compareTo(null);
  }

  //-----------------------------------------------------------------------
  public void test_equals_hashCode() {
    Country a1 = Country.GB;
    Country a2 = Country.of("GB");
    Country b = Country.EU;
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
    Country a = Country.GB;
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("String"), false);
    assertEquals(a.equals(new Object()), false);
  }

  //-----------------------------------------------------------------------
  public void test_toString() {
    Country test = Country.GB;
    assertEquals("GB", test.toString());
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(Country.GB);
    assertSerialization(Country.of("US"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(Country.class, Country.GB);
    assertJodaConvert(Country.class, Country.of("US"));
  }

}
