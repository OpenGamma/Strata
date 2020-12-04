/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SabrSwaptionVolatilitiesName}.
 */
public class SabrSwaptionVolatilitiesNameTest {

  @Test
  public void test_of() {
    SabrSwaptionVolatilitiesName test = SabrSwaptionVolatilitiesName.of("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(SabrSwaptionVolatilities.class);
    assertThat(test.toString()).isEqualTo("Foo");
    assertThat(test.compareTo(SabrSwaptionVolatilitiesName.of("Goo")) < 0).isTrue();
  }

}
