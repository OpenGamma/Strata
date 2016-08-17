/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link StandardId}.
 */
@Test
public class StandardIdTest {

  private static final String SCHEME = "Scheme";
  private static final String OTHER_SCHEME = "Other";

  //-------------------------------------------------------------------------
  public void test_factory_String_String() {
    StandardId test = StandardId.of("scheme:/+foo", "value");
    assertEquals(test.getScheme(), "scheme:/+foo");
    assertEquals(test.getValue(), "value");
    assertEquals(test.toString(), "scheme:/+foo~value");
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

  @DataProvider(name = "factoryValid")
  Object[][] data_factoryValid() {
    return new Object[][] {
        {"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "123"},
        {"abcdefghijklmnopqrstuvwxyz", "123"},
        {"0123456789:/+.=_-", "123"},
        {"ABC", "! !\"$%%^&*()123abcxyzABCXYZ"},
    };
  }

  @Test(dataProvider = "factoryValid")
  public void test_factory_String_String_valid(String scheme, String value) {
    StandardId.of(scheme, value);
  }

  @DataProvider(name = "factoryInvalid")
  Object[][] data_factoryInvalid() {
    return new Object[][] {
        {"", ""},
        {" ", "123"},
        {"{", "123"},
        {"ABC", " 123"},
        {"ABC", "12}3"},
        {"ABC", "12\u00003"},
    };
  }

  @Test(dataProvider = "factoryInvalid", expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_invalid(String scheme, String value) {
    StandardId.of(scheme, value);
  }

  //-------------------------------------------------------------------------
  public void test_encodeScheme() {
    String test = StandardId.encodeScheme("http://www.opengamma.com/foo/../~bar#test");
    assertEquals(test, "http://www.opengamma.com/foo/../%7Ebar%23test");
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
  public void test_equals() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");
    StandardId d2 = StandardId.of(SCHEME, "d2");
    StandardId d3 = StandardId.of("Different", "d1");
    assertEquals((Object) d1a.equals(d1a), true);
    assertEquals((Object) d1a.equals(d1b), true);
    assertEquals((Object) d1a.equals(d2), false);
    assertEquals((Object) d1b.equals(d1a), true);
    assertEquals((Object) d1b.equals(d1b), true);
    assertEquals((Object) d1b.equals(d2), false);
    assertEquals((Object) d2.equals(d1a), false);
    assertEquals((Object) d2.equals(d1b), false);
    assertEquals((Object) d2.equals(d2), true);
    assertEquals((Object) d3.equals(d1a), false);
    assertEquals((Object) d3.equals(d2), false);
    assertEquals((Object) d3.equals(d3), true);
    assertEquals((Object) d1b.equals("d1"), false);
    assertEquals((Object) d1b.equals(null), false);
  }

  public void test_hashCode() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");
    assertEquals((Object) d1b.hashCode(), d1a.hashCode());
  }

  public void test_comparisonByScheme() {
    StandardId id1 = StandardId.of(SCHEME, "123");
    StandardId id2 = StandardId.of(OTHER_SCHEME, "234");
    // as schemes are different, will compare by scheme
    assertThat(id1).isGreaterThan(id2);
  }

  public void test_comparisonWithSchemeSame() {
    StandardId id1 = StandardId.of(SCHEME, "123");
    StandardId id2 = StandardId.of(SCHEME, "234");
    // as schemes are same, will compare by id
    assertThat(id1).isLessThan(id2);
  }

  public void coverage() {
    coverImmutableBean(StandardId.of(SCHEME, "123"));
  }
}
