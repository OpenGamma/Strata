/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link TypedString}.
 */
@Test
public class TypedStringTest {

  public void test_of() {
    SampleType test = SampleType.of("A");
    assertEquals(test.toString(), "A");
  }

  public void test_of_invalid() {
    assertThrowsIllegalArg(() -> SampleType.of(null));
    assertThrowsIllegalArg(() -> SampleType.of(""));
  }

  public void test_of_validated() {
    SampleValidatedType test = SampleValidatedType.of("ABC");
    assertEquals(test.toString(), "ABC");
  }

  public void test_of_validated_invalid() {
    assertThrowsIllegalArg(() -> SampleValidatedType.of(null));
    assertThrowsIllegalArg(() -> SampleValidatedType.of("ABc"));
  }

  public void test_equalsHashCode() {
    SampleType a1 = SampleType.of("A");
    SampleType a2 = SampleType.of("A");
    SampleType b = SampleType.of("B");

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals("A"), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_compareTo() {
    SampleType a = SampleType.of("A");
    SampleType b = SampleType.of("B");
    SampleType c = SampleType.of("C");

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
    assertSerialization(SampleType.of("A"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(SampleType.class, SampleType.of("A"));
  }

}
