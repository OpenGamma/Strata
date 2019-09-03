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
 * Test {@link SwaptionVolatilitiesId}.
 */
public class SwaptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    assertThat(test.getName()).isEqualTo(SwaptionVolatilitiesName.of("Foo"));
    assertThat(test.getMarketDataType()).isEqualTo(SwaptionVolatilities.class);
    assertThat(test.getMarketDataName()).isEqualTo(SwaptionVolatilitiesName.of("Foo"));
    assertThat(test.toString()).isEqualTo("SwaptionVolatilitiesId:Foo");
  }

  @Test
  public void test_of_object() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of(SwaptionVolatilitiesName.of("Foo"));
    assertThat(test.getName()).isEqualTo(SwaptionVolatilitiesName.of("Foo"));
    assertThat(test.getMarketDataType()).isEqualTo(SwaptionVolatilities.class);
    assertThat(test.getMarketDataName()).isEqualTo(SwaptionVolatilitiesName.of("Foo"));
    assertThat(test.toString()).isEqualTo("SwaptionVolatilitiesId:Foo");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    coverImmutableBean(test);
    SwaptionVolatilitiesId test2 = SwaptionVolatilitiesId.of("Bar");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SwaptionVolatilitiesId test = SwaptionVolatilitiesId.of("Foo");
    assertSerialization(test);
  }

}
