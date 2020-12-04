/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SabrSwaptionVolatilitiesId}.
 */
public class SabrSwaptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SabrSwaptionVolatilitiesId test = SabrSwaptionVolatilitiesId.of("Foo");
    assertThat(test.getName()).isEqualTo(SabrSwaptionVolatilitiesName.of("Foo"));
    assertThat(test.getMarketDataType()).isEqualTo(SabrSwaptionVolatilities.class);
    assertThat(test.getMarketDataName()).isEqualTo(SabrSwaptionVolatilitiesName.of("Foo"));
    assertThat(test.toString()).isEqualTo("SabrSwaptionVolatilitiesId:Foo");
  }

  @Test
  public void test_of_object() {
    SabrSwaptionVolatilitiesId test = SabrSwaptionVolatilitiesId.of(SabrSwaptionVolatilitiesName.of("Foo"));
    assertThat(test.getName()).isEqualTo(SabrSwaptionVolatilitiesName.of("Foo"));
    assertThat(test.getMarketDataType()).isEqualTo(SabrSwaptionVolatilities.class);
    assertThat(test.getMarketDataName()).isEqualTo(SabrSwaptionVolatilitiesName.of("Foo"));
    assertThat(test.toString()).isEqualTo("SabrSwaptionVolatilitiesId:Foo");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SabrSwaptionVolatilitiesId test = SabrSwaptionVolatilitiesId.of("Foo");
    coverImmutableBean(test);
    SabrSwaptionVolatilitiesId test2 = SabrSwaptionVolatilitiesId.of("Bar");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SabrSwaptionVolatilitiesId test = SabrSwaptionVolatilitiesId.of("Foo");
    assertSerialization(test);
  }

}
