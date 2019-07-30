/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link FixedCouponBondYieldConvention}.
 */
@Test
public class FixedCouponBondYieldConventionTest {

  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FixedCouponBondYieldConvention.GB_BUMP_DMO, "GB-Bump-DMO"},
        {FixedCouponBondYieldConvention.US_STREET, "US-Street"},
        {FixedCouponBondYieldConvention.DE_BONDS, "DE-Bonds"},
        {FixedCouponBondYieldConvention.JP_SIMPLE, "JP-Simple"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FixedCouponBondYieldConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FixedCouponBondYieldConvention convention, String name) {
    assertEquals(FixedCouponBondYieldConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(FixedCouponBondYieldConvention convention, String name) {
    assertEquals(FixedCouponBondYieldConvention.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(FixedCouponBondYieldConvention convention, String name) {
    assertEquals(FixedCouponBondYieldConvention.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupStandard(FixedCouponBondYieldConvention convention, String name) {
    assertEquals(FixedCouponBondYieldConvention.of(convention.name()), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondYieldConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondYieldConvention.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FixedCouponBondYieldConvention.class);
  }

  public void test_serialization() {
    assertSerialization(FixedCouponBondYieldConvention.GB_BUMP_DMO);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FixedCouponBondYieldConvention.class, FixedCouponBondYieldConvention.GB_BUMP_DMO);
  }

}
