/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link ValueAdjustmentType}.
 */
public class ValueAdjustmentTypeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_adjust() {
    assertThat(ValueAdjustmentType.DELTA_AMOUNT.adjust(2d, 3d)).isEqualTo(5d);
    assertThat(ValueAdjustmentType.DELTA_MULTIPLIER.adjust(2d, 1.5d)).isEqualTo(5d);
    assertThat(ValueAdjustmentType.MULTIPLIER.adjust(2d, 1.5d)).isEqualTo(3d);
    assertThat(ValueAdjustmentType.REPLACE.adjust(2d, 1.5d)).isEqualTo(1.5d);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {ValueAdjustmentType.DELTA_AMOUNT, "DeltaAmount"},
        {ValueAdjustmentType.DELTA_MULTIPLIER, "DeltaMultiplier"},
        {ValueAdjustmentType.MULTIPLIER, "Multiplier"},
        {ValueAdjustmentType.REPLACE, "Replace"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(ValueAdjustmentType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(ValueAdjustmentType convention, String name) {
    assertThat(ValueAdjustmentType.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(ValueAdjustmentType convention, String name) {
    assertThat(ValueAdjustmentType.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(ValueAdjustmentType convention, String name) {
    assertThat(ValueAdjustmentType.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> ValueAdjustmentType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> ValueAdjustmentType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(ValueAdjustmentType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(ValueAdjustmentType.DELTA_AMOUNT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(ValueAdjustmentType.class, ValueAdjustmentType.DELTA_AMOUNT);
  }

}
