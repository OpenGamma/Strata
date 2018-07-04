/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link BillYieldConvention}.
 */
@Test
public class BillYieldConventionTest {

  public static final double PRICE = 0.99;
  public static final double YIELD = 0.03;
  public static final double ACCRUAL_FACTOR = 0.123;
  public static final double TOLERANCE = 1.0E-10;;

  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {BillYieldConvention.DISCOUNT, "Discount"},
        {BillYieldConvention.FRANCE_CD, "France-CD"},
        {BillYieldConvention.INTEREST_AT_MATURITY, "Interest-At-Maturity"},
        {BillYieldConvention.JAPAN_BILLS, "Japan-Bills"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(BillYieldConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(BillYieldConvention convention, String name) {
    assertEquals(BillYieldConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(BillYieldConvention convention, String name) {
    assertEquals(BillYieldConvention.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(BillYieldConvention convention, String name) {
    assertEquals(BillYieldConvention.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupStandard(BillYieldConvention convention, String name) {
    assertEquals(BillYieldConvention.of(convention.name()), convention);
  }

  public void test_price_yield_discount() {
    assertEquals(
        BillYieldConvention.DISCOUNT.priceFromYield(YIELD, ACCRUAL_FACTOR),
        1.0d - ACCRUAL_FACTOR * YIELD, TOLERANCE);
  }

  public void test_price_yield_france() {
    assertEquals(
        BillYieldConvention.FRANCE_CD.priceFromYield(YIELD, ACCRUAL_FACTOR),
        1.0d / (1.0d + ACCRUAL_FACTOR * YIELD), TOLERANCE);
  }

  public void test_price_yield_intatmaturity() {
    assertEquals(
        BillYieldConvention.INTEREST_AT_MATURITY.priceFromYield(YIELD, ACCRUAL_FACTOR),
        1.0d / (1.0d + ACCRUAL_FACTOR * YIELD), TOLERANCE);
  }

  public void test_price_yield_japan() {
    assertEquals(
        BillYieldConvention.JAPAN_BILLS.priceFromYield(YIELD, ACCRUAL_FACTOR),
        1.0d / (1.0d + ACCRUAL_FACTOR * YIELD), TOLERANCE);
  }

  public void test_yield_price_discount() {
    assertEquals(
        BillYieldConvention.DISCOUNT.yieldFromPrice(PRICE, ACCRUAL_FACTOR),
        (1.0d - PRICE) / ACCRUAL_FACTOR, TOLERANCE);
  }

  public void test_yield_price_france() {
    assertEquals(
        BillYieldConvention.FRANCE_CD.yieldFromPrice(PRICE, ACCRUAL_FACTOR),
        (1.0d / PRICE - 1.0d) / ACCRUAL_FACTOR, TOLERANCE);
  }

  public void test_yield_price_intatmaturity() {
    assertEquals(
        BillYieldConvention.INTEREST_AT_MATURITY.yieldFromPrice(PRICE, ACCRUAL_FACTOR),
        (1.0d / PRICE - 1.0d) / ACCRUAL_FACTOR, TOLERANCE);
  }

  public void test_yield_price_japan() {
    assertEquals(
        BillYieldConvention.JAPAN_BILLS.yieldFromPrice(PRICE, ACCRUAL_FACTOR),
        (1.0d / PRICE - 1.0d) / ACCRUAL_FACTOR, TOLERANCE);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> BillYieldConvention.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> BillYieldConvention.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(BillYieldConvention.class);
  }

  public void test_serialization() {
    assertSerialization(BillYieldConvention.class);
  }

  public void test_jodaConvert() {
    assertJodaConvert(BillYieldConvention.class, BillYieldConvention.DISCOUNT);
  }

}
