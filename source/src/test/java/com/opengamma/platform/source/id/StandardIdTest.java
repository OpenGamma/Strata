/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

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
    assertEquals(test.getScheme().getName(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.toString(), "Scheme~value");
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
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.toString(), "Scheme~value");
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
    assertEquals(test.toString(), expected);
  }

  @Test(dataProvider = "formats")
  public void test_formats_parse(String value, String text) {
    StandardId test = StandardId.parse(text);
    assertEquals(test.getScheme(), "A");
    assertEquals(test.getValue(), value);
  }

  //-------------------------------------------------------------------------
  public void test_parse() {
    StandardId test = StandardId.parse("Scheme~value");
    assertEquals(test.getScheme(), SCHEME);
    assertEquals(test.getValue(), "value");
    assertEquals(test.toString(), "Scheme~value");
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
    assertEquals((Object) test.isScheme(SCHEME), true);
    assertEquals((Object) test.isScheme(OTHER_SCHEME), false);
    assertEquals((Object) test.isScheme(null), false);
  }

  public void test_isNotScheme_ExternalScheme() {
    StandardId test = StandardId.of(SCHEME, "value");
    assertEquals((Object) test.isNotScheme(SCHEME), false);
    assertEquals((Object) test.isNotScheme(OTHER_SCHEME), true);
    assertEquals((Object) test.isNotScheme(null), true);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");
    StandardId d2 = StandardId.of(SCHEME, "d2");

    assertEquals((Object) d1a.equals(d1a), true);
    assertEquals((Object) d1a.equals(d1b), true);
    assertEquals((Object) d1a.equals(d2), false);

    assertEquals((Object) d1b.equals(d1a), true);
    assertEquals((Object) d1b.equals(d1b), true);
    assertEquals((Object) d1b.equals(d2), false);

    assertEquals((Object) d2.equals(d1a), false);
    assertEquals((Object) d2.equals(d1b), false);
    assertEquals((Object) d2.equals(d2), true);

    assertEquals((Object) d1b.equals("d1"), false);
    assertEquals((Object) d1b.equals(null), false);
  }

  public void test_hashCode() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");

    assertEquals((Object) d1b.hashCode(), d1a.hashCode());
  }

  public void test_getIdentityKey() {
    StandardId test = StandardId.of(SCHEME, "value");
    assertEquals(test.getStandardId(), test);
  }

  public void test_comparisonByScheme() {
    StandardId id1 = StandardId.of(SCHEME, "123");
    StandardId id2 = StandardId.of(OTHER_SCHEME, "234");

    // As schemes are different, will compare by scheme
    assertThat(id1).isGreaterThan(id2);
  }

  public void test_comparisonWithSchemeSame() {
    StandardId id1 = StandardId.of(SCHEME, "123");
    StandardId id2 = StandardId.of(SCHEME, "234");

    // As schemes are same, will compare by id
    assertThat(id1).isLessThan(id2);
  }

}
