/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

public class ImmutableMeasureTest {

  /**
   * Tests that measure names are validated
   */
  @Test
  public void namePattern() {
    assertThatIllegalArgumentException().isThrownBy(() -> ImmutableMeasure.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> ImmutableMeasure.of(""));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableMeasure.of("Foo Bar"))
        .withMessageMatching(".*must only contain the characters.*");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableMeasure.of("Foo_Bar"))
        .withMessageMatching(".*must only contain the characters.*");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableMeasure.of("FooBar!"))
        .withMessageMatching(".*must only contain the characters.*");

    // These should execute without throwing an exception
    ImmutableMeasure.of("FooBar");
    ImmutableMeasure.of("Foo-Bar");
    ImmutableMeasure.of("123");
    ImmutableMeasure.of("FooBar123");
  }
}
