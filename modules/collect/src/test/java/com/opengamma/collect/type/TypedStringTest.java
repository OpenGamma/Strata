/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.type;

import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link TypedString}.
 */
@Test
public class TypedStringTest {

  public void test_mock_of() {
    MockType test = MockType.of("A");
    assertEquals(test.toString(), "A");
  }

  public void test_mock_of_invalid() {
    assertThrows(() -> MockType.of(null), IllegalArgumentException.class);
    assertThrows(() -> MockType.of(""), IllegalArgumentException.class);
  }

  public void test_mock_equalsHashCode() {
    MockType a1 = MockType.of("A");
    MockType a2 = MockType.of("A");
    MockType b = MockType.of("B");
    
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals("A"), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_mock_compareTo() {
    MockType a = MockType.of("A");
    MockType b = MockType.of("B");
    MockType c = MockType.of("C");
    
    assertEquals(a.compareTo(a) == 0, true);
    assertEquals(a.compareTo(b) < 0, true);
    assertEquals(a.compareTo(c) < 0, true);
    
    assertEquals(b.compareTo(a) > 0, true);
    assertEquals(b.compareTo(b) == 0, true);
    assertEquals(b.compareTo(c) < 0, true);
    
    assertEquals(c.compareTo(a) > 0, true);
    assertEquals(c.compareTo(b) > 0, true);
    assertEquals(c.compareTo(c) == 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(MockType.of("A"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(MockType.class, MockType.of("A"));
  }

}
