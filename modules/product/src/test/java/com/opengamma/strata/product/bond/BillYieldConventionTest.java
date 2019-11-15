/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link BillYieldConvention}.
 */
public class BillYieldConventionTest {

  public static final double PRICE = 0.99;
  public static final double YIELD = 0.03;
  public static final double ACCRUAL_FACTOR = 0.123;
  public static final double TOLERANCE = 1.0E-10;

  public static Object[][] data_name() {
    return new Object[][] {
        {BillYieldConvention.DISCOUNT, "Discount"},
        {BillYieldConvention.FRANCE_CD, "France-CD"},
        {BillYieldConvention.INTEREST_AT_MATURITY, "Interest-At-Maturity"},
        {BillYieldConvention.JAPAN_BILLS, "Japan-Bills"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(BillYieldConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(BillYieldConvention convention, String name) {
    assertThat(BillYieldConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(BillYieldConvention convention, String name) {
    assertThat(BillYieldConvention.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(BillYieldConvention convention, String name) {
    assertThat(BillYieldConvention.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupStandard(BillYieldConvention convention, String name) {
    assertThat(BillYieldConvention.of(convention.name())).isEqualTo(convention);
  }

  @Test
  public void test_price_yield_discount() {
    assertThat(BillYieldConvention.DISCOUNT.priceFromYield(YIELD, ACCRUAL_FACTOR))
        .isCloseTo(1.0d - ACCRUAL_FACTOR * YIELD, offset(TOLERANCE));
  }

  @Test
  public void test_price_yield_france() {
    assertThat(BillYieldConvention.FRANCE_CD.priceFromYield(YIELD, ACCRUAL_FACTOR))
        .isCloseTo(1.0d / (1.0d + ACCRUAL_FACTOR * YIELD), offset(TOLERANCE));
  }

  @Test
  public void test_price_yield_intatmaturity() {
    assertThat(BillYieldConvention.INTEREST_AT_MATURITY.priceFromYield(YIELD, ACCRUAL_FACTOR))
        .isCloseTo(1.0d / (1.0d + ACCRUAL_FACTOR * YIELD), offset(TOLERANCE));
  }

  @Test
  public void test_price_yield_japan() {
    assertThat(BillYieldConvention.JAPAN_BILLS.priceFromYield(YIELD, ACCRUAL_FACTOR))
        .isCloseTo(1.0d / (1.0d + ACCRUAL_FACTOR * YIELD), offset(TOLERANCE));
  }

  @Test
  public void test_yield_price_discount() {
    assertThat(BillYieldConvention.DISCOUNT.yieldFromPrice(PRICE, ACCRUAL_FACTOR))
        .isCloseTo((1.0d - PRICE) / ACCRUAL_FACTOR, offset(TOLERANCE));
  }

  @Test
  public void test_yield_price_france() {
    assertThat(BillYieldConvention.FRANCE_CD.yieldFromPrice(PRICE, ACCRUAL_FACTOR))
        .isCloseTo((1.0d / PRICE - 1.0d) / ACCRUAL_FACTOR, offset(TOLERANCE));
  }

  @Test
  public void test_yield_price_intatmaturity() {
    assertThat(BillYieldConvention.INTEREST_AT_MATURITY.yieldFromPrice(PRICE, ACCRUAL_FACTOR))
        .isCloseTo((1.0d / PRICE - 1.0d) / ACCRUAL_FACTOR, offset(TOLERANCE));
  }

  @Test
  public void test_yield_price_japan() {
    assertThat(BillYieldConvention.JAPAN_BILLS.yieldFromPrice(PRICE, ACCRUAL_FACTOR))
        .isCloseTo((1.0d / PRICE - 1.0d) / ACCRUAL_FACTOR, offset(TOLERANCE));
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillYieldConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BillYieldConvention.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(BillYieldConvention.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(BillYieldConvention.class);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(BillYieldConvention.class, BillYieldConvention.DISCOUNT);
  }

}
