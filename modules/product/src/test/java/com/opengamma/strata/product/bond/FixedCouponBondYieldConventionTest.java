/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link FixedCouponBondYieldConvention}.
 */
public class FixedCouponBondYieldConventionTest {

  public static Object[][] data_name() {
    return new Object[][] {
        {FixedCouponBondYieldConvention.GB_BUMP_DMO, "GB-Bump-DMO"},
        {FixedCouponBondYieldConvention.US_STREET, "US-Street"},
        {FixedCouponBondYieldConvention.DE_BONDS, "DE-Bonds"},
        {FixedCouponBondYieldConvention.JP_SIMPLE, "JP-Simple"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FixedCouponBondYieldConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FixedCouponBondYieldConvention convention, String name) {
    assertThat(FixedCouponBondYieldConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(FixedCouponBondYieldConvention convention, String name) {
    assertThat(FixedCouponBondYieldConvention.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(FixedCouponBondYieldConvention convention, String name) {
    assertThat(FixedCouponBondYieldConvention.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupStandard(FixedCouponBondYieldConvention convention, String name) {
    assertThat(FixedCouponBondYieldConvention.of(convention.name())).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondYieldConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondYieldConvention.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FixedCouponBondYieldConvention.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FixedCouponBondYieldConvention.GB_BUMP_DMO);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FixedCouponBondYieldConvention.class, FixedCouponBondYieldConvention.GB_BUMP_DMO);
  }

}
