/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

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
 * Test {@link FailureReason}.
 */
public class FailureReasonTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FailureReason.CALCULATION_FAILED, "CALCULATION_FAILED"},
        {FailureReason.CURRENCY_CONVERSION, "CURRENCY_CONVERSION"},
        {FailureReason.ERROR, "ERROR"},
        {FailureReason.INVALID, "INVALID"},
        {FailureReason.MISSING_DATA, "MISSING_DATA"},
        {FailureReason.MULTIPLE, "MULTIPLE"},
        {FailureReason.NOT_APPLICABLE, "NOT_APPLICABLE"},
        {FailureReason.OTHER, "OTHER"},
        {FailureReason.PARSING, "PARSING"},
        {FailureReason.UNSUPPORTED, "UNSUPPORTED"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FailureReason convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FailureReason convention, String name) {
    assertThat(FailureReason.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(FailureReason convention, String name) {
    assertThat(FailureReason.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(FailureReason convention, String name) {
    assertThat(FailureReason.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FailureReason.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FailureReason.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FailureReason.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FailureReason.CALCULATION_FAILED);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FailureReason.class, FailureReason.CURRENCY_CONVERSION);
  }

}
