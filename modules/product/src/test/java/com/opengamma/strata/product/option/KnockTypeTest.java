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
 * Test {@link KnockType}.
 */
@Test
public class KnockTypeTest {

  //-------------------------------------------------------------------------
  public void test_isKnockIn() {
    assertEquals(KnockType.KNOCK_IN.isKnockIn(), true);
    assertEquals(KnockType.KNOCK_OUT.isKnockIn(), false);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {KnockType.KNOCK_IN, "KnockIn"},
        {KnockType.KNOCK_OUT, "KnockOut"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(KnockType type, String name) {
    assertEquals(type.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(KnockType type, String name) {
    assertEquals(KnockType.of(name), type);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> KnockType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> KnockType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(KnockType.class);
  }

  public void test_serialization() {
    assertSerialization(KnockType.KNOCK_OUT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(KnockType.class, KnockType.KNOCK_IN);
  }

}
