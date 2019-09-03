/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FxOptionVolatilitiesName}.
 */
public class FxOptionVolatilitiesNameTest {

  @Test
  public void test_of() {
    FxOptionVolatilitiesName test = FxOptionVolatilitiesName.of("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(FxOptionVolatilities.class);
    assertThat(test.toString()).isEqualTo("Foo");
    assertThat(test.compareTo(FxOptionVolatilitiesName.of("Goo")) < 0).isTrue();
  }

}
