/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics;

import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link PayReceive}.
 */
@Test
public class PayReceiveTest {

  //-------------------------------------------------------------------------
  public void test_ofPay() {
    assertEquals(PayReceive.ofPay(true), PayReceive.PAY);
    assertEquals(PayReceive.ofPay(false), PayReceive.RECEIVE);
  }

  public void test_adjustAmount_pay() {
    assertEquals(PayReceive.PAY.adjustAmount(1d), -1d, 0d);
    assertEquals(PayReceive.PAY.adjustAmount(0d), -0d, 0d);
    assertEquals(PayReceive.PAY.adjustAmount(-1d), 1d, 0d);
  }

  public void test_adjustAmount_receive() {
    assertEquals(PayReceive.RECEIVE.adjustAmount(1d), 1d, 0d);
    assertEquals(PayReceive.RECEIVE.adjustAmount(0d), 0d, 0d);
    assertEquals(PayReceive.RECEIVE.adjustAmount(-1d), -1d, 0d);
  }

  public void test_isPay() {
    assertEquals(PayReceive.PAY.isPay(), true);
    assertEquals(PayReceive.RECEIVE.isPay(), false);
  }

  public void test_isReceive() {
    assertEquals(PayReceive.PAY.isReceive(), false);
    assertEquals(PayReceive.RECEIVE.isReceive(), true);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
      return new Object[][] {
          {PayReceive.PAY, "Pay"},
          {PayReceive.RECEIVE, "Receive"},
      };
  }

  @Test(dataProvider = "name")
  public void test_toString(PayReceive convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PayReceive convention, String name) {
    assertEquals(PayReceive.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> PayReceive.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> PayReceive.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(PayReceive.class);
  }

  public void test_serialization() {
    assertSerialization(PayReceive.PAY);
  }

  public void test_jodaConvert() {
    assertJodaConvert(PayReceive.class, PayReceive.PAY);
  }

}
