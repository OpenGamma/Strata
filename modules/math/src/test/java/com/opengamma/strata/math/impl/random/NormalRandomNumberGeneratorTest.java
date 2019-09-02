/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.cern.MersenneTwister64;

/**
 * Test {@link NormalRandomNumberGenerator}.
 */
public class NormalRandomNumberGeneratorTest {

  private static final NormalRandomNumberGenerator GENERATOR = new NormalRandomNumberGenerator(0, 1);

  @Test
  public void test_array() {
    double[] result = GENERATOR.getVector(10);
    assertThat(result.length).isEqualTo(10);
  }

  @Test
  public void test_list() {
    List<double[]> result = GENERATOR.getVectors(10, 50);
    assertThat(result).hasSize(50);
    for (double[] d : result) {
      assertThat(d.length).isEqualTo(10);
    }
  }

  @Test
  public void test_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new NormalRandomNumberGenerator(0, -1));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new NormalRandomNumberGenerator(0, -1, new MersenneTwister64()));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new NormalRandomNumberGenerator(0, 1, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GENERATOR.getVectors(-1, 4));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GENERATOR.getVectors(1, -5));
  }

}
