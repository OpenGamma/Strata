/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SurfaceName}.
 */
public class SurfaceNameTest {

  @Test
  public void test_of() {
    SurfaceName test = SurfaceName.of("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(Surface.class);
    assertThat(test.toString()).isEqualTo("Foo");
    assertThat(test.compareTo(SurfaceName.of("Goo")) < 0).isTrue();
  }

}
