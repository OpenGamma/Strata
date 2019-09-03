/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class DecompositionFactoryTest {

  @Test
  public void testBadName() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DecompositionFactory.getDecomposition("X"));
  }

  @Test
  public void testNullDecomposition() {
    DecompositionFactory.getDecompositionName(null);
  }

  @Test
  public void test() {
    assertThat(DecompositionFactory.LU_COMMONS_NAME).isEqualTo(
        DecompositionFactory.getDecompositionName(DecompositionFactory.getDecomposition(DecompositionFactory.LU_COMMONS_NAME)));
    assertThat(DecompositionFactory.QR_COMMONS_NAME).isEqualTo(
        DecompositionFactory.getDecompositionName(DecompositionFactory.getDecomposition(DecompositionFactory.QR_COMMONS_NAME)));
    assertThat(DecompositionFactory.SV_COMMONS_NAME).isEqualTo(
        DecompositionFactory.getDecompositionName(DecompositionFactory.getDecomposition(DecompositionFactory.SV_COMMONS_NAME)));
  }

}
