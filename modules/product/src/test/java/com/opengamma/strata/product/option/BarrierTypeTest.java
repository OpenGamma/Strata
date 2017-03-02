/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link BarrierType}.
 */
@Test
public class BarrierTypeTest {

  //-------------------------------------------------------------------------
  public void test_isDown() {
    assertEquals(BarrierType.UP.isDown(), false);
    assertEquals(BarrierType.DOWN.isDown(), true);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {BarrierType.UP, "Up"},
        {BarrierType.DOWN, "Down"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(BarrierType type, String name) {
    assertEquals(type.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(BarrierType type, String name) {
    assertEquals(BarrierType.of(name), type);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> BarrierType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> BarrierType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(BarrierType.class);
  }

  public void test_serialization() {
    assertSerialization(BarrierType.DOWN);
  }

  public void test_jodaConvert() {
    assertJodaConvert(BarrierType.class, BarrierType.UP);
  }

}
