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
public class ExternalIdTest {

  private static final ExternalScheme SCHEME = ExternalScheme.of("Scheme");
  private static final ExternalScheme OTHER_SCHEME = ExternalScheme.of("Other");

  //-------------------------------------------------------------------------
  public void test_factory_ExternalScheme_String() {
    ExternalId test = ExternalId.of(SCHEME, "value");
    assertEquals("Scheme", test.getScheme().getName());
    assertEquals("value", test.getValue());
    assertEquals("Scheme~value", test.toString());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_nullScheme() {
    ExternalId.of((ExternalScheme) null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_nullValue() {
    ExternalId.of(SCHEME, (String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ExternalScheme_String_emptyValue() {
    ExternalId.of(SCHEME, "");
  }

  //-------------------------------------------------------------------------
  public void test_factory_String_String() {
    ExternalId test = ExternalId.of("Scheme", "value");
    assertEquals("Scheme", test.getScheme().getName());
    assertEquals("value", test.getValue());
    assertEquals("Scheme~value", test.toString());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullScheme() {
    ExternalId.of((String) null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullValue() {
    ExternalId.of("Scheme", (String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_emptyValue() {
    ExternalId.of("Scheme", "");
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
    ExternalId test = ExternalId.of("A", value);
    assertEquals(expected, test.toString());
  }

  @Test(dataProvider = "formats")
  public void test_formats_parse(String value, String text) {
    ExternalId test = ExternalId.parse(text);
    assertEquals("A", test.getScheme().getName());
    assertEquals(value, test.getValue());
  }

  //-------------------------------------------------------------------------
  public void test_parse() {
    ExternalId test = ExternalId.parse("Scheme~value");
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
    ExternalId.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_isScheme_ExternalScheme() {
    ExternalId test = ExternalId.of(SCHEME, "value");
    assertEquals(true, test.isScheme(SCHEME));
    assertEquals(false, test.isScheme(OTHER_SCHEME));
    assertEquals(false, test.isScheme((ExternalScheme) null));
  }

  public void test_isNotScheme_ExternalScheme() {
    ExternalId test = ExternalId.of(SCHEME, "value");
    assertEquals(false, test.isNotScheme(SCHEME));
    assertEquals(true, test.isNotScheme(OTHER_SCHEME));
    assertEquals(true, test.isNotScheme((ExternalScheme) null));
  }

  //-------------------------------------------------------------------------
  public void test_getIdentityKey() {
    ExternalId test = ExternalId.of(SCHEME, "value");
    assertEquals(test, test.getExternalId());
  }

  //-------------------------------------------------------------------------
  public void test_toBundle() {
    ExternalId test = ExternalId.of(SCHEME, "value");
    assertEquals(ExternalIdBundle.of(test), test.toBundle());
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ExternalId d1a = ExternalId.of(SCHEME, "d1");
    ExternalId d1b = ExternalId.of(SCHEME, "d1");
    ExternalId d2 = ExternalId.of(SCHEME, "d2");

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
    ExternalId d1a = ExternalId.of(SCHEME, "d1");
    ExternalId d1b = ExternalId.of(SCHEME, "d1");

    assertEquals(d1a.hashCode(), d1b.hashCode());
  }

}
