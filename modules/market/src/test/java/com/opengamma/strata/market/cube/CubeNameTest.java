/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CubeName}.
 */
public class CubeNameTest {

  @Test
  public void test_of() {
    CubeName test = CubeName.of("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(Cube.class);
    assertThat(test.toString()).isEqualTo("Foo");
    assertThat(test.compareTo(CubeName.of("Goo")) < 0).isTrue();
  }

}
