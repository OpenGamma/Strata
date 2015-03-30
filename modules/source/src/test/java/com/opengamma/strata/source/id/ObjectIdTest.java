/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ObjectIdTest {

  public void test_factory_String_String() {
    ObjectId test = ObjectId.of("Scheme", "value");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.toString(), "Scheme~value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullScheme() {
    ObjectId.of((String) null, "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_emptyScheme() {
    ObjectId.of("", "value");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_nullValue() {
    ObjectId.of("Scheme", (String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_emptyValue() {
    ObjectId.of("Scheme", "");
  }

  //-------------------------------------------------------------------------
  public void test_parse() {
    ObjectId test = ObjectId.parse("Scheme~value");
    assertEquals(test.getScheme(), "Scheme");
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
        {"Scheme~value~other"},
    };
  }

  @Test(dataProvider = "parseInvalidFormat", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_invalidFormat(String text) {
    ObjectId.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_withScheme() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(test.withScheme("newScheme"), ObjectId.of("newScheme", "value1"));
  }

  public void test_withScheme_toSame() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertSame(test.withScheme("scheme1"), test);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withScheme_empty() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    test.withScheme("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withScheme_null() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    test.withScheme(null);
  }

  //-------------------------------------------------------------------------
  public void test_withValue() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(test.withValue("newValue"), ObjectId.of("scheme1", "newValue"));
  }

  public void test_withValue_toSame() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertSame(test.withValue("value1"), test);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withValue_empty() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    test.withValue("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withValue_null() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    test.withValue(null);
  }

  //-------------------------------------------------------------------------
  public void test_atVersion() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(test.atVersion("32"), UniqueId.of("scheme1", "value1", "32"));
  }

  public void test_atVersion_empty() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(test.atVersion(""), UniqueId.of("scheme1", "value1", ""));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_atVersion_null() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(test.atVersion(null), UniqueId.of("scheme1", "value1", ""));
  }

  //-------------------------------------------------------------------------
  public void test_getObjectId() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertSame(test.getObjectId(), test);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    ObjectId a = ObjectId.of("A", "1");
    ObjectId b = ObjectId.of("A", "2");
    ObjectId c = ObjectId.of("B", "2");

    assertTrue(a.compareTo(a) == 0);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(a.compareTo(c) < 0);

    assertTrue(b.compareTo(a) > 0);
    assertTrue(b.compareTo(b) == 0);
    assertTrue(b.compareTo(c) < 0);

    assertTrue(c.compareTo(a) > 0);
    assertTrue(c.compareTo(b) > 0);
    assertTrue(c.compareTo(c) == 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_compareTo_null() {
    ObjectId test = ObjectId.of("A", "1");
    test.compareTo(null);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ObjectId sd1a = ObjectId.of("Scheme", "d1");
    ObjectId sd1b = ObjectId.of("Scheme", "d1");
    ObjectId sd2 = ObjectId.of("Scheme", "d2");
    ObjectId td2 = ObjectId.of("Temp", "d2");

    assertEquals((Object) sd1a.equals(sd1a), true);
    assertEquals((Object) sd1a.equals(sd1b), true);
    assertEquals((Object) sd1a.equals(sd2), false);
    assertEquals((Object) sd1a.equals(td2), false);

    assertEquals((Object) sd1b.equals(sd1a), true);
    assertEquals((Object) sd1b.equals(sd1b), true);
    assertEquals((Object) sd1b.equals(sd2), false);
    assertEquals((Object) sd1b.equals(td2), false);

    assertEquals((Object) sd2.equals(sd1a), false);
    assertEquals((Object) sd2.equals(sd1b), false);
    assertEquals((Object) sd2.equals(sd2), true);
    assertEquals((Object) sd2.equals(td2), false);

    assertEquals((Object) td2.equals(sd1a), false);
    assertEquals((Object) td2.equals(sd1b), false);
    assertEquals((Object) td2.equals(sd2), false);
    assertEquals((Object) td2.equals(td2), true);

    assertEquals((Object) sd1b.equals("d1"), false);
    assertEquals((Object) sd1b.equals(null), false);
  }

  public void test_hashCode() {
    ObjectId d1a = ObjectId.of("Scheme", "d1");
    ObjectId d1b = ObjectId.of("Scheme", "d1");

    assertEquals((Object) d1b.hashCode(), d1a.hashCode());
  }

}
