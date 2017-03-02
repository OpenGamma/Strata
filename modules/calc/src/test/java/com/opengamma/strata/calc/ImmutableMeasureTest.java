/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.assertThrows;

import org.testng.annotations.Test;

@Test
public class ImmutableMeasureTest {

  /**
   * Tests that measure names are validated
   */
  public void namePattern() {
    assertThrows(() -> ImmutableMeasure.of(null), IllegalArgumentException.class);
    assertThrows(() -> ImmutableMeasure.of(""), IllegalArgumentException.class);
    assertThrows(() -> ImmutableMeasure.of("Foo Bar"), IllegalArgumentException.class, ".*must only contain the characters.*");
    assertThrows(() -> ImmutableMeasure.of("Foo_Bar"), IllegalArgumentException.class, ".*must only contain the characters.*");
    assertThrows(() -> ImmutableMeasure.of("FooBar!"), IllegalArgumentException.class, ".*must only contain the characters.*");

    // These should execute without throwing an exception
    ImmutableMeasure.of("FooBar");
    ImmutableMeasure.of("Foo-Bar");
    ImmutableMeasure.of("123");
    ImmutableMeasure.of("FooBar123");
  }
}
