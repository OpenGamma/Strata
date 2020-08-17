/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ParameterizedData}.
 */
public class ParameterizedDataTest {

  private static final LabelParameterMetadata LABEL_METADATA = LabelParameterMetadata.of("LABEL");
  private static final ParameterizedData CURVE = new TestingParameterizedData(1d);

  @Test
  public void test_withPerturbation() {
    assertThat(CURVE.withPerturbation((i, v, m) -> v)).isSameAs(CURVE);
    assertThat(CURVE.withPerturbation((i, v, m) -> v + 2d).getParameter(0)).isEqualTo(3d);
    assertThat(CURVE.findParameterIndex(ParameterMetadata.empty())).isEmpty();
    assertThat(CURVE.findParameterIndex(LABEL_METADATA)).isEmpty();
  }

}
