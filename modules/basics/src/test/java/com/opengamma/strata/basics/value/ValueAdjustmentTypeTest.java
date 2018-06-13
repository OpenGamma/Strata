/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link ValueAdjustmentType}.
 */
@Test
public class ValueAdjustmentTypeTest {

  //-------------------------------------------------------------------------
  public void test_adjust() {
    assertEquals(ValueAdjustmentType.DELTA_AMOUNT.adjust(2d, 3d), 5d, 1e-12);
    assertEquals(ValueAdjustmentType.DELTA_MULTIPLIER.adjust(2d, 1.5d), 5d, 1e-12);
    assertEquals(ValueAdjustmentType.MULTIPLIER.adjust(2d, 1.5d), 3d, 1e-12);
    assertEquals(ValueAdjustmentType.REPLACE.adjust(2d, 1.5d), 1.5d, 1e-12);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {ValueAdjustmentType.DELTA_AMOUNT, "DeltaAmount"},
        {ValueAdjustmentType.DELTA_MULTIPLIER, "DeltaMultiplier"},
        {ValueAdjustmentType.MULTIPLIER, "Multiplier"},
        {ValueAdjustmentType.REPLACE, "Replace"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(ValueAdjustmentType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(ValueAdjustmentType convention, String name) {
    assertEquals(ValueAdjustmentType.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(ValueAdjustmentType convention, String name) {
    assertEquals(ValueAdjustmentType.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(ValueAdjustmentType convention, String name) {
    assertEquals(ValueAdjustmentType.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> ValueAdjustmentType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> ValueAdjustmentType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(ValueAdjustmentType.class);
  }

  public void test_serialization() {
    assertSerialization(ValueAdjustmentType.DELTA_AMOUNT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(ValueAdjustmentType.class, ValueAdjustmentType.DELTA_AMOUNT);
  }

}
