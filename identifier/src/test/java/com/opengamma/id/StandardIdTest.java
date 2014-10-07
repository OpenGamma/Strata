/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class StandardIdTest {

  private static final String SCHEME = "Scheme";
  private static final String OTHER_SCHEME = "Other";

  //-------------------------------------------------------------------------
  public void test_factory_ExternalScheme_String() {
    ExternalId test = ExternalId.of(SCHEME, "value");
    assertEquals("Scheme", test.getScheme().getName());
    assertEquals("value", test.getValue());
    assertEquals("Scheme~value", test.toString());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_nullScheme() {
    StandardId.of(null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_nullValue() {
    StandardId.of(SCHEME, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_emptyValue() {
    StandardId.of(SCHEME, "");
  }

  //-------------------------------------------------------------------------
  public void test_factory_String_String() {
    StandardId test = StandardId.of("Scheme", "value");
    assertEquals("Scheme", test.getScheme());
    assertEquals("value", test.getValue());
    assertEquals("Scheme~value", test.toString());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullScheme() {
    StandardId.of(null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullValue() {
    StandardId.of("Scheme", null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_emptyValue() {
    StandardId.of("Scheme", "");
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "formats")
  Object[][] data_formats() {
    return new Object[][] {
        {"Value", "A~Value"},
        {"a+b", "A~a+b"},
    };
  }

  @Test(dataProvider = "formats")
  public void test_formats_toString(String value, String expected) {
    StandardId test = StandardId.of("A", value);
    assertEquals(expected, test.toString());
  }

  @Test(dataProvider = "formats")
  public void test_formats_parse(String value, String text) {
    StandardId test = StandardId.parse(text);
    assertEquals("A", test.getScheme());
    assertEquals(value, test.getValue());
  }

  //-------------------------------------------------------------------------
  public void test_parse() {
    StandardId test = StandardId.parse("Scheme~value");
    assertEquals(SCHEME, test.getScheme());
    assertEquals("value", test.getValue());
    assertEquals("Scheme~value", test.toString());
  }

  @DataProvider(name = "parseInvalidFormat")
  Object[][] data_parseInvalidFormat() {
    return new Object[][] {
        {"Scheme"},
        {"Scheme~"},
        {"~value"},
        {"Scheme:value"},
        {"a~b~c"},
    };
  }

  @Test(dataProvider = "parseInvalidFormat", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_invalidFormat(String text) {
    StandardId.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_isScheme_ExternalScheme() {
    StandardId test = StandardId.of(SCHEME, "value");
    assertEquals(true, test.isScheme(SCHEME));
    assertEquals(false, test.isScheme(OTHER_SCHEME));
    assertEquals(false, test.isScheme(null));
  }

  public void test_isNotScheme_ExternalScheme() {
    StandardId test = StandardId.of(SCHEME, "value");
    assertEquals(false, test.isNotScheme(SCHEME));
    assertEquals(true, test.isNotScheme(OTHER_SCHEME));
    assertEquals(true, test.isNotScheme(null));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");
    StandardId d2 = StandardId.of(SCHEME, "d2");

    assertEquals(true, d1a.equals(d1a));
    assertEquals(true, d1a.equals(d1b));
    assertEquals(false, d1a.equals(d2));

    assertEquals(true, d1b.equals(d1a));
    assertEquals(true, d1b.equals(d1b));
    assertEquals(false, d1b.equals(d2));

    assertEquals(false, d2.equals(d1a));
    assertEquals(false, d2.equals(d1b));
    assertEquals(true, d2.equals(d2));

    assertEquals(false, d1b.equals("d1"));
    assertEquals(false, d1b.equals(null));
  }

  public void test_hashCode() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");

    assertEquals(d1a.hashCode(), d1b.hashCode());
  }

}
