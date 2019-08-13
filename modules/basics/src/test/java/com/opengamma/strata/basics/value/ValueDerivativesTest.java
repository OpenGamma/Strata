/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link ValueDerivatives}.
 */
public class ValueDerivativesTest {

  private static final double VALUE = 123.4;
  private static final DoubleArray DERIVATIVES = DoubleArray.of(1.0, 2.0, 3.0);

  @Test
  public void test_of() {
    ValueDerivatives test = ValueDerivatives.of(VALUE, DERIVATIVES);
    assertThat(test.getValue()).isEqualTo(VALUE);
    assertThat(test.getDerivatives()).isEqualTo(DERIVATIVES);
    assertThat(test.getDerivative(0)).isEqualTo(DERIVATIVES.get(0));
    assertThat(test.getDerivative(1)).isEqualTo(DERIVATIVES.get(1));
    assertThat(test.getDerivative(2)).isEqualTo(DERIVATIVES.get(2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ValueDerivatives test = ValueDerivatives.of(VALUE, DERIVATIVES);
    coverImmutableBean(test);
    assertThat(ValueDerivatives.meta()).isNotNull();
    ValueDerivatives test2 = ValueDerivatives.of(123.4, DoubleArray.of(1.0, 2.0, 3.0));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ValueDerivatives test = ValueDerivatives.of(VALUE, DERIVATIVES);
    assertSerialization(test);
  }

}
