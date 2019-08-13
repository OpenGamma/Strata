/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * Test {@link NoRounding}.
 */
public class NoRoundingTest {

  @Test
  public void test_none() {
    Rounding test = Rounding.none();
    assertThat(test.toString()).isEqualTo("No rounding");
    assertThat(Rounding.none()).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void round_double_NONE() {
    assertThat(Rounding.none().round(1.23d)).isEqualTo(1.23d);
  }

  @Test
  public void round_BigDecimal_NONE() {
    assertThat(Rounding.none().round(BigDecimal.valueOf(1.23d))).isEqualTo(BigDecimal.valueOf(1.23d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(NoRounding.INSTANCE);
  }

  @Test
  public void test_serialization() {
    Rounding test = Rounding.none();
    assertSerialization(test);
  }

}
