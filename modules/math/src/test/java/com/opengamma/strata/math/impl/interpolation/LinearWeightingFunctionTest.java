/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class LinearWeightingFunctionTest extends WeightingFunctionTestCase {

  @Override
  protected WeightingFunction getInstance() {
    return LinearWeightingFunction.INSTANCE;
  }

  @Test
  public void testWeighting() {
    assertThat(getInstance().getWeight(STRIKES, INDEX, STRIKE)).isCloseTo(0.55, offset(EPS));
    assertThat(getInstance().getWeight(STRIKES, INDEX, STRIKES[3])).isCloseTo(1, offset(EPS));
  }

  @Test
  public void testName() {
    assertThat(getInstance().getName()).isEqualTo("Linear");
  }

}
