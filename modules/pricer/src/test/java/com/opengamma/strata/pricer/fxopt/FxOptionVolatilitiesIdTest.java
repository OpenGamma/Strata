/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FxOptionVolatilitiesId}.
 */
public class FxOptionVolatilitiesIdTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of("Foo");
    assertThat(test.getName()).isEqualTo(FxOptionVolatilitiesName.of("Foo"));
    assertThat(test.getMarketDataType()).isEqualTo(FxOptionVolatilities.class);
    assertThat(test.getMarketDataName()).isEqualTo(FxOptionVolatilitiesName.of("Foo"));
  }

  @Test
  public void test_of_object() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of(FxOptionVolatilitiesName.of("Foo"));
    assertThat(test.getName()).isEqualTo(FxOptionVolatilitiesName.of("Foo"));
    assertThat(test.getMarketDataType()).isEqualTo(FxOptionVolatilities.class);
    assertThat(test.getMarketDataName()).isEqualTo(FxOptionVolatilitiesName.of("Foo"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of("Foo");
    coverImmutableBean(test);
    FxOptionVolatilitiesId test2 = FxOptionVolatilitiesId.of("Bar");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FxOptionVolatilitiesId test = FxOptionVolatilitiesId.of("Foo");
    assertSerialization(test);
  }

}
