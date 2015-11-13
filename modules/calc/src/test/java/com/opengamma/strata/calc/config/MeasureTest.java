/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.collect.TestHelper.assertThrows;

import org.testng.annotations.Test;

@Test
public class MeasureTest {

  /**
   * Tests that measure names are validated
   */
  public void namePattern() {
    assertThrows(() -> Measure.of(null), IllegalArgumentException.class);
    assertThrows(() -> Measure.of(""), IllegalArgumentException.class);
    assertThrows(() -> Measure.of("Foo Bar"), IllegalArgumentException.class, ".*must only contain the characters.*");
    assertThrows(() -> Measure.of("Foo_Bar"), IllegalArgumentException.class, ".*must only contain the characters.*");
    assertThrows(() -> Measure.of("FooBar!"), IllegalArgumentException.class, ".*must only contain the characters.*");

    // These should execute without throwing an exception
    Measure.of("FooBar");
    Measure.of("Foo-Bar");
    Measure.of("123");
    Measure.of("FooBar123");
  }
}
