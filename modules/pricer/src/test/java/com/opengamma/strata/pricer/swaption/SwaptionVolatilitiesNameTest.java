/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SwaptionVolatilitiesName}.
 */
public class SwaptionVolatilitiesNameTest {

  @Test
  public void test_of() {
    SwaptionVolatilitiesName test = SwaptionVolatilitiesName.of("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(SwaptionVolatilities.class);
    assertThat(test.toString()).isEqualTo("Foo");
    assertThat(test.compareTo(SwaptionVolatilitiesName.of("Goo")) < 0).isTrue();
  }

}
