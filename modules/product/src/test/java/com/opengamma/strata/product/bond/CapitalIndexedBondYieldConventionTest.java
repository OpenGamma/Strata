/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

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
 * Test {@link CapitalIndexedBondYieldConvention}.
 */
public class CapitalIndexedBondYieldConventionTest {

  public static Object[][] data_name() {
    return new Object[][] {
        {CapitalIndexedBondYieldConvention.US_IL_REAL, "US-I/L-Real"},
        {CapitalIndexedBondYieldConvention.GB_IL_FLOAT, "GB-I/L-Float"},
        {CapitalIndexedBondYieldConvention.GB_IL_BOND, "GB-I/L-Bond"},
        {CapitalIndexedBondYieldConvention.JP_IL_SIMPLE, "JP-I/L-Simple"},
        {CapitalIndexedBondYieldConvention.JP_IL_COMPOUND, "JP-I/L-Compound"}
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CapitalIndexedBondYieldConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CapitalIndexedBondYieldConvention convention, String name) {
    assertThat(CapitalIndexedBondYieldConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(CapitalIndexedBondYieldConvention convention, String name) {
    assertThat(CapitalIndexedBondYieldConvention.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(CapitalIndexedBondYieldConvention convention, String name) {
    assertThat(CapitalIndexedBondYieldConvention.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupStandard(CapitalIndexedBondYieldConvention convention, String name) {
    assertThat(CapitalIndexedBondYieldConvention.of(convention.name())).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondYieldConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondYieldConvention.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(CapitalIndexedBondYieldConvention.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(CapitalIndexedBondYieldConvention.US_IL_REAL);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CapitalIndexedBondYieldConvention.class, CapitalIndexedBondYieldConvention.GB_IL_BOND);
  }

}
