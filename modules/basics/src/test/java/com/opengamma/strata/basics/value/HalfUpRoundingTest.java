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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link HalfUpRounding}.
 */
public class HalfUpRoundingTest {

  @Test
  public void test_of_Currency() {
    Rounding test = Rounding.of(Currency.USD);
    assertThat(test.round(63.455d)).isEqualTo(63.46d);
    assertThat(test.round(63.454d)).isEqualTo(63.45d);
  }

  @Test
  public void test_ofDecimalPlaces() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(4);
    assertThat(test.getDecimalPlaces()).isEqualTo(4);
    assertThat(test.getFraction()).isEqualTo(0);
    assertThat(test.toString()).isEqualTo("Round to 4dp");
    assertThat(Rounding.ofDecimalPlaces(4)).isEqualTo(test);
  }

  @Test
  public void test_ofDecimalPlaces_big() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(40);
    assertThat(test.getDecimalPlaces()).isEqualTo(40);
    assertThat(test.getFraction()).isEqualTo(0);
    assertThat(test.toString()).isEqualTo("Round to 40dp");
    assertThat(Rounding.ofDecimalPlaces(40)).isEqualTo(test);
  }

  @Test
  public void test_ofDecimalPlaces_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.ofDecimalPlaces(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.ofDecimalPlaces(257));
  }

  @Test
  public void test_ofFractionalDecimalPlaces() {
    HalfUpRounding test = HalfUpRounding.ofFractionalDecimalPlaces(4, 32);
    assertThat(test.getDecimalPlaces()).isEqualTo(4);
    assertThat(test.getFraction()).isEqualTo(32);
    assertThat(test.toString()).isEqualTo("Round to 1/32 of 4dp");
    assertThat(Rounding.ofFractionalDecimalPlaces(4, 32)).isEqualTo(test);
  }

  @Test
  public void test_ofFractionalDecimalPlaces_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.ofFractionalDecimalPlaces(-1, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.ofFractionalDecimalPlaces(257, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.ofFractionalDecimalPlaces(0, -1));
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.ofFractionalDecimalPlaces(0, 257));
  }

  @Test
  public void test_builder() {
    HalfUpRounding test = HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 4)
        .set(HalfUpRounding.meta().fraction(), 1)
        .build();
    assertThat(test.getDecimalPlaces()).isEqualTo(4);
    assertThat(test.getFraction()).isEqualTo(0);
    assertThat(test.toString()).isEqualTo("Round to 4dp");
  }

  @Test
  public void test_builder_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), -1)
        .build());
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 257)
        .build());
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 4)
        .set(HalfUpRounding.meta().fraction(), -1)
        .build());
    assertThatIllegalArgumentException().isThrownBy(() -> HalfUpRounding.meta().builder()
        .set(HalfUpRounding.meta().decimalPlaces(), 4)
        .set(HalfUpRounding.meta().fraction(), 257)
        .build());
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_round() {
    return new Object[][] {
        {HalfUpRounding.ofDecimalPlaces(2), 12.3449, 12.34},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3450, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3451, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3500, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3549, 12.35},
        {HalfUpRounding.ofDecimalPlaces(2), 12.3550, 12.36},

        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3424, 12.340},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3425, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3426, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3449, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3450, 12.345},
        {HalfUpRounding.ofFractionalDecimalPlaces(2, 2), 12.3451, 12.345},
    };
  }

  @ParameterizedTest
  @MethodSource("data_round")
  public void round_double_NONE(HalfUpRounding rounding, double input, double expected) {
    assertThat(rounding.round(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("data_round")
  public void round_BigDecimal_NONE(HalfUpRounding rounding, double input, double expected) {
    assertThat(rounding.round(BigDecimal.valueOf(input))).isEqualTo(BigDecimal.valueOf(expected));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(4);
    coverImmutableBean(test);
    HalfUpRounding test2 = HalfUpRounding.ofFractionalDecimalPlaces(4, 32);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    HalfUpRounding test = HalfUpRounding.ofDecimalPlaces(4);
    assertSerialization(test);
  }

}
