/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link PaymentOnDefault}.
 */
@Test
public class PaymentOnDefaultTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {PaymentOnDefault.NONE, "None"},
        {PaymentOnDefault.ACCRUED_PREMIUM, "AccruedPremium"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(PaymentOnDefault convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PaymentOnDefault convention, String name) {
    assertEquals(PaymentOnDefault.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> PaymentOnDefault.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> PaymentOnDefault.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(PaymentOnDefault.class);
  }

  public void test_serialization() {
    assertSerialization(PaymentOnDefault.ACCRUED_PREMIUM);
  }

  public void test_jodaConvert() {
    assertJodaConvert(PaymentOnDefault.class, PaymentOnDefault.ACCRUED_PREMIUM);
  }

}
