/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link FloatingRateType}.
 */
@Test
public class FloatingRateTypeTest {

  //-------------------------------------------------------------------------
  public void test_isIbor() {
    assertEquals(FloatingRateType.IBOR.isIbor(), true);
    assertEquals(FloatingRateType.OVERNIGHT_AVERAGED.isIbor(), false);
    assertEquals(FloatingRateType.OVERNIGHT_COMPOUNDED.isIbor(), false);
    assertEquals(FloatingRateType.PRICE.isIbor(), false);
    assertEquals(FloatingRateType.OTHER.isIbor(), false);
  }

  public void test_isOvernight() {
    assertEquals(FloatingRateType.IBOR.isOvernight(), false);
    assertEquals(FloatingRateType.OVERNIGHT_AVERAGED.isOvernight(), true);
    assertEquals(FloatingRateType.OVERNIGHT_COMPOUNDED.isOvernight(), true);
    assertEquals(FloatingRateType.PRICE.isOvernight(), false);
    assertEquals(FloatingRateType.OTHER.isOvernight(), false);
  }

  public void test_isPrice() {
    assertEquals(FloatingRateType.IBOR.isPrice(), false);
    assertEquals(FloatingRateType.OVERNIGHT_AVERAGED.isPrice(), false);
    assertEquals(FloatingRateType.OVERNIGHT_COMPOUNDED.isPrice(), false);
    assertEquals(FloatingRateType.PRICE.isPrice(), true);
    assertEquals(FloatingRateType.OTHER.isPrice(), false);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FloatingRateType.IBOR, "Ibor"},
        {FloatingRateType.OVERNIGHT_AVERAGED, "OvernightAveraged"},
        {FloatingRateType.OVERNIGHT_COMPOUNDED, "OvernightCompounded"},
        {FloatingRateType.PRICE, "Price"},
        {FloatingRateType.OTHER, "Other"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FloatingRateType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FloatingRateType convention, String name) {
    assertEquals(FloatingRateType.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(FloatingRateType convention, String name) {
    assertEquals(FloatingRateType.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(FloatingRateType convention, String name) {
    assertEquals(FloatingRateType.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FloatingRateType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FloatingRateType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FloatingRateType.class);
  }

  public void test_serialization() {
    assertSerialization(FloatingRateType.IBOR);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FloatingRateType.class, FloatingRateType.IBOR);
  }

}
