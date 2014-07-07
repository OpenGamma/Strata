/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ObjectIdTest {

  public void test_factory_String_String() {
    ObjectId test = ObjectId.of("Scheme", "value");
    assertEquals("Scheme", test.getScheme());
    assertEquals("value", test.getValue());
    assertEquals("Scheme~value", test.toString());
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
    assertEquals("Scheme", test.getScheme());
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
    assertEquals(ObjectId.of("newScheme", "value1"), test.withScheme("newScheme"));
  }

  public void test_withScheme_toSame() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertSame(test, test.withScheme("scheme1"));
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
    assertEquals(ObjectId.of("scheme1", "newValue"), test.withValue("newValue"));
  }

  public void test_withValue_toSame() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertSame(test, test.withValue("value1"));
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
    assertEquals(UniqueId.of("scheme1", "value1", "32"), test.atVersion("32"));
  }

  public void test_atVersion_empty() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(UniqueId.of("scheme1", "value1", ""), test.atVersion(""));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_atVersion_null() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertEquals(UniqueId.of("scheme1", "value1", ""), test.atVersion(null));
  }

  //-------------------------------------------------------------------------
  public void test_getObjectId() {
    ObjectId test = ObjectId.of("scheme1", "value1");
    assertSame(test, test.getObjectId());
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    ObjectId a = ObjectId.of("A", "1");
    ObjectId b = ObjectId.of("A", "2");
    ObjectId c = ObjectId.of("B", "2");
    
    assertEquals(true, a.compareTo(a) == 0);
    assertEquals(true, a.compareTo(b) < 0);
    assertEquals(true, a.compareTo(c) < 0);
    
    assertEquals(true, b.compareTo(a) > 0);
    assertEquals(true, b.compareTo(b) == 0);
    assertEquals(true, b.compareTo(c) < 0);
    
    assertEquals(true, c.compareTo(a) > 0);
    assertEquals(true, c.compareTo(b) > 0);
    assertEquals(true, c.compareTo(c) == 0);
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
    
    assertEquals(true, sd1a.equals(sd1a));
    assertEquals(true, sd1a.equals(sd1b));
    assertEquals(false, sd1a.equals(sd2));
    assertEquals(false, sd1a.equals(td2));
    
    assertEquals(true, sd1b.equals(sd1a));
    assertEquals(true, sd1b.equals(sd1b));
    assertEquals(false, sd1b.equals(sd2));
    assertEquals(false, sd1b.equals(td2));
    
    assertEquals(false, sd2.equals(sd1a));
    assertEquals(false, sd2.equals(sd1b));
    assertEquals(true, sd2.equals(sd2));
    assertEquals(false, sd2.equals(td2));
    
    assertEquals(false, td2.equals(sd1a));
    assertEquals(false, td2.equals(sd1b));
    assertEquals(false, td2.equals(sd2));
    assertEquals(true, td2.equals(td2));
    
    assertEquals(false, sd1b.equals("d1"));
    assertEquals(false, sd1b.equals(null));
  }

  public void test_hashCode() {
    ObjectId d1a = ObjectId.of("Scheme", "d1");
    ObjectId d1b = ObjectId.of("Scheme", "d1");
    
    assertEquals(d1a.hashCode(), d1b.hashCode());
  }

}
