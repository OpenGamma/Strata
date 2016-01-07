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
    assertThrows(() -> Measure.of(null, Measure.singleType, Measure.scenarioType), IllegalArgumentException.class);
    assertThrows(() -> Measure.of("", Measure.singleType, Measure.scenarioType), IllegalArgumentException.class);
    assertThrows(() -> Measure.of("Foo Bar", Measure.singleType, Measure.scenarioType), IllegalArgumentException.class, ".*must only contain the characters.*");
    assertThrows(() -> Measure.of("Foo_Bar", Measure.singleType, Measure.scenarioType), IllegalArgumentException.class, ".*must only contain the characters.*");
    assertThrows(() -> Measure.of("FooBar!", Measure.singleType, Measure.scenarioType), IllegalArgumentException.class, ".*must only contain the characters.*");

    // These should execute without throwing an exception
    Measure.of("FooBar", Measure.singleType, Measure.scenarioType);
    Measure.of("Foo-Bar", Measure.singleType, Measure.scenarioType);
    Measure.of("123", Measure.singleType, Measure.scenarioType);
    Measure.of("FooBar123", Measure.singleType, Measure.scenarioType);
  }
}
