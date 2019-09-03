/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test {@link VolatilityAndBucketedSensitivities}.
 */
public class VolatilityAndBucketedSensitivitiesTest {

  private static final double VOL = 0.34;
  private static final DoubleMatrix SENSITIVITIES = DoubleMatrix.of(2, 3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
  private static final DoubleMatrix SENSITIVITIES2 = DoubleMatrix.of(1, 3, 0.1, 0.2, 0.3);

  @Test
  public void testNullSensitivities() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> VolatilityAndBucketedSensitivities.of(VOL, null));
  }

  @Test
  public void test() {
    VolatilityAndBucketedSensitivities object = VolatilityAndBucketedSensitivities.of(VOL, SENSITIVITIES);
    assertThat(VOL).isEqualTo(object.getVolatility());
    assertThat(SENSITIVITIES).isEqualTo(object.getSensitivities());
    VolatilityAndBucketedSensitivities other = VolatilityAndBucketedSensitivities.of(
        VOL, DoubleMatrix.of(2, 3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6));
    assertThat(object).isEqualTo(other);
    assertThat(object.hashCode()).isEqualTo(other.hashCode());
    other = VolatilityAndBucketedSensitivities.of(VOL + 0.01, SENSITIVITIES);
    assertThat(other.equals(object)).isFalse();
    other = VolatilityAndBucketedSensitivities.of(VOL, SENSITIVITIES2);
    assertThat(other.equals(object)).isFalse();
  }

}
