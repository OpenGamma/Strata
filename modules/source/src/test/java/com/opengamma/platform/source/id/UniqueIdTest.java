/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class UniqueIdTest {

  //-------------------------------------------------------------------------
  public void test_factory_String_String_String() {
    UniqueId test = UniqueId.of("Scheme", "value", "version");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.getVersion(), "version");
    assertEquals(test.toString(), "Scheme~value~version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_String_nullScheme() {
    UniqueId.of((String) null, "value", "version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_String_emptyScheme() {
    UniqueId.of("", "value", "version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_String_nullValue() {
    UniqueId.of("Scheme", (String) null, "version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_String_emptyValue() {
    UniqueId.of("Scheme", "", "version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_String_String_String_nullVersion() {
    UniqueId.of("Scheme", "value", (String) null);
  }

  public void test_factory_String_String_String_emptyVersion() {
    UniqueId test = UniqueId.of("Scheme", "value", "");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.getVersion(), "");
    assertEquals(test.toString(), "Scheme~value~");
  }

  //-------------------------------------------------------------------------
  public void test_factory_ObjectId_String() {
    UniqueId test = UniqueId.of(ObjectId.of("Scheme", "value"), "version");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.getVersion(), "version");
    assertEquals(test.toString(), "Scheme~value~version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ObjectId_String_nullObjectId() {
    UniqueId.of((ObjectId) null, "version");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_factory_ObjectId_String_nullVersion() {
    UniqueId.of(ObjectId.of("Scheme", "value"), null);
  }

  public void test_factory_ObjectId_String_emptyVersion() {
    UniqueId test = UniqueId.of(ObjectId.of("Scheme", "value"), "");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.getVersion(), "");
    assertEquals(test.toString(), "Scheme~value~");
  }

  //-------------------------------------------------------------------------
  public void test_parse_version() {
    UniqueId test = UniqueId.parse("Scheme~value~version");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.getVersion(), "version");
    assertEquals(test.toString(), "Scheme~value~version");
  }

  public void test_parse_emptyVersion() {
    UniqueId test = UniqueId.parse("Scheme~value~");
    assertEquals(test.getScheme(), "Scheme");
    assertEquals(test.getValue(), "value");
    assertEquals(test.getVersion(), "");
    assertEquals(test.toString(), "Scheme~value~");
  }

  @DataProvider(name = "parseInvalidFormat")
  Object[][] data_parseInvalidFormat() {
    return new Object[][] {
        {"Scheme"},
        {"Scheme~~1"},
        {"~value~1"},
        {"Scheme:value~1"},
        {"Scheme~value~version~other"},
    };
  }

  @Test(dataProvider = "parseInvalidFormat", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_invalidFormat(String text) {
    UniqueId.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_withScheme() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    assertEquals(test.withScheme("newScheme"), UniqueId.of("newScheme", "value1", "32"));
  }

  public void test_withScheme_toSame() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    assertSame(test.withScheme("scheme1"), test);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withScheme_null() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    test.withScheme(null);
  }

  //-------------------------------------------------------------------------
  public void test_withValue() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    assertEquals(test.withValue("newValue"), UniqueId.of("scheme1", "newValue", "32"));
  }

  public void test_withValue_toSame() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    assertSame(test.withValue("value1"), test);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withValue_null() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    test.withValue(null);
  }

  //-------------------------------------------------------------------------
  public void test_withVersion_added() {
    UniqueId test = UniqueId.of("scheme1", "value1", "");
    assertEquals(test.withVersion("32"), UniqueId.of("scheme1", "value1", "32"));
  }

  public void test_withVersion_replaced() {
    UniqueId test = UniqueId.of("scheme1", "value1", "12");
    assertEquals(test.withVersion("32"), UniqueId.of("scheme1", "value1", "32"));
  }

  public void test_withVersion_replacedToEmpty() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    assertEquals(test.withVersion(""), UniqueId.of("scheme1", "value1", ""));
  }

  public void test_withVersion_replacedToSame() {
    UniqueId test = UniqueId.of("scheme1", "value1", "32");
    assertSame(test.withVersion("32"), test);
  }

  //-------------------------------------------------------------------------
  public void test_getObjectId() {
    UniqueId test = UniqueId.of("scheme1", "value1", "version");
    assertEquals(test.getObjectId(), ObjectId.of("scheme1", "value1"));
  }

  //-------------------------------------------------------------------------
  public void test_getUniqueId() {
    UniqueId test = UniqueId.of("scheme1", "value1", "version");
    assertSame(test.getUniqueId(), test);
  }

  //-------------------------------------------------------------------------
  public void test_equalObjectId_emptyVersion() {
    UniqueId d1a = UniqueId.of("Scheme", "d1", "");
    UniqueId d1b = UniqueId.of("Scheme", "d1", "");
    UniqueId d2 = UniqueId.of("Scheme", "d2", "");

    assertEquals((Object) d1a.equalObjectId(d1a), true);
    assertEquals((Object) d1a.equalObjectId(d1b), true);
    assertEquals((Object) d1a.equalObjectId(d2), false);

    assertEquals((Object) d1b.equalObjectId(d1a), true);
    assertEquals((Object) d1b.equalObjectId(d1b), true);
    assertEquals((Object) d1b.equalObjectId(d2), false);

    assertEquals((Object) d2.equalObjectId(d1a), false);
    assertEquals((Object) d2.equalObjectId(d1b), false);
    assertEquals((Object) d2.equalObjectId(d2), true);
  }

  public void test_equalObjectId_version() {
    UniqueId d1 = UniqueId.of("Scheme", "d1", "1");
    UniqueId d2 = UniqueId.of("Scheme", "d1", "2");

    assertEquals((Object) d1.equalObjectId(d2), true);
  }

  public void test_equalObjectId_scheme() {
    UniqueId d1 = UniqueId.of("Scheme", "d1", "1");
    UniqueId d2 = UniqueId.of("Other", "d1", "2");

    assertEquals((Object) d1.equalObjectId(d2), false);
  }

  public void test_equalObjectId_null() {
    UniqueId d1 = UniqueId.of("Scheme", "d1", "1");

    assertEquals((Object) d1.equalObjectId(null), false);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo_emptyVersion() {
    UniqueId a = UniqueId.of("A", "1", "");
    UniqueId b = UniqueId.of("A", "2", "");
    UniqueId c = UniqueId.of("B", "2", "");
    
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

  public void test_compareTo_versionOnly() {
    UniqueId a = UniqueId.of("A", "1", "");
    UniqueId b = UniqueId.of("A", "1", "4");
    UniqueId c = UniqueId.of("A", "1", "5");
    
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

  public void test_compareTo_valueBeatsVersion() {
    UniqueId a = UniqueId.of("A", "1", "5");
    UniqueId b = UniqueId.of("A", "2", "4");
    
    assertTrue(a.compareTo(a) == 0);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(b.compareTo(a) > 0);
    assertTrue(b.compareTo(b) == 0);
  }

  public void test_compareTo_schemeBeatsValue() {
    UniqueId a = UniqueId.of("A", "2", "1");
    UniqueId b = UniqueId.of("B", "1", "1");
    
    assertTrue(a.compareTo(a) == 0);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(b.compareTo(a) > 0);
    assertTrue(b.compareTo(b) == 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_compareTo_null() {
    UniqueId test = UniqueId.of("A", "1", "1");
    test.compareTo(null);
  }

  //-------------------------------------------------------------------------
  public void test_equals_emptyVersion() {
    UniqueId d1a = UniqueId.of("Scheme", "d1", "");
    UniqueId d1b = UniqueId.of("Scheme", "d1", "");
    UniqueId d2 = UniqueId.of("Scheme", "d2", "");

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

  public void test_equals_version() {
    UniqueId d1a = UniqueId.of("Scheme", "d1", "1");
    UniqueId d1b = UniqueId.of("Scheme", "d1", "1");
    UniqueId d2 = UniqueId.of("Scheme", "d2", "1");

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

  public void test_equals_differentScheme() {
    UniqueId d1 = UniqueId.of("Scheme", "d1", "1");
    UniqueId d2 = UniqueId.of("Other", "d1", "1");

    assertEquals((Object) d1.equals(d1), true);
    assertEquals((Object) d1.equals(d2), false);
    assertEquals((Object) d2.equals(d1), false);
    assertEquals((Object) d2.equals(d2), true);
  }

  public void test_hashCode_emptyVersion() {
    UniqueId d1a = UniqueId.of("Scheme", "d1", "");
    UniqueId d1b = UniqueId.of("Scheme", "d1", "");

    assertEquals((Object) d1b.hashCode(), d1a.hashCode());
  }

  public void test_hashCode_version() {
    UniqueId d1a = UniqueId.of("Scheme", "d1", "1");
    UniqueId d1b = UniqueId.of("Scheme", "d1", "1");

    assertEquals((Object) d1b.hashCode(), d1a.hashCode());
  }

}
