/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test.
 */
public class NegativeRateMethodTest {

  //-------------------------------------------------------------------------
  @Test
  public void adjust_allowNegative() {
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(1d)).isCloseTo(1d, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(0d)).isCloseTo(0d, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(-0d)).isCloseTo(-0d, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(-1d)).isCloseTo(-1d, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.MAX_VALUE)).isCloseTo(Double.MAX_VALUE, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.MIN_VALUE)).isCloseTo(Double.MIN_VALUE, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.POSITIVE_INFINITY))
        .isCloseTo(Double.POSITIVE_INFINITY, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.NEGATIVE_INFINITY))
        .isCloseTo(Double.NEGATIVE_INFINITY, offset(0d));
    assertThat(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.NaN)).isNaN();
  }

  @Test
  public void adjust_notNegative() {
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(1d)).isCloseTo(1d, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(0d)).isCloseTo(0d, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(-0d)).isCloseTo(0d, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(-1d)).isCloseTo(0d, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.MAX_VALUE)).isCloseTo(Double.MAX_VALUE, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.MIN_VALUE)).isCloseTo(Double.MIN_VALUE, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.POSITIVE_INFINITY))
        .isCloseTo(Double.POSITIVE_INFINITY, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.NEGATIVE_INFINITY)).isCloseTo(0d, offset(0d));
    assertThat(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.NaN)).isNaN();
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {NegativeRateMethod.ALLOW_NEGATIVE, "AllowNegative"},
        {NegativeRateMethod.NOT_NEGATIVE, "NotNegative"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(NegativeRateMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(NegativeRateMethod convention, String name) {
    assertThat(NegativeRateMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NegativeRateMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NegativeRateMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(NegativeRateMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(NegativeRateMethod.ALLOW_NEGATIVE);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(NegativeRateMethod.class, NegativeRateMethod.ALLOW_NEGATIVE);
  }

}
