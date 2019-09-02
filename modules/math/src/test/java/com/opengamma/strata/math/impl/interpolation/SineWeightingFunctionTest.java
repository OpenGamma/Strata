/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class SineWeightingFunctionTest extends WeightingFunctionTestCase {

  @Override
  protected WeightingFunction getInstance() {
    return SineWeightingFunction.INSTANCE;
  }

  @Test
  public void testName() {
    assertThat(getInstance().getName()).isEqualTo("Sine");
  }

}
