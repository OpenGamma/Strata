/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class WeightingFunctionsTest {

  @Test
  public void testBadName() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> WeightingFunction.of("Random"));
  }

  @Test
  public void test() {
    assertThat(WeightingFunction.of("Linear")).isEqualTo(WeightingFunctions.LINEAR);
    assertThat(WeightingFunction.of("Sine")).isEqualTo(WeightingFunctions.SINE);
  }

  @Test
  public void coverage() {
    coverPrivateConstructor(WeightingFunctions.class);
  }

}
