/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

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

  public void test_ofSignedAmount() {
    assertEquals(PayReceive.ofSignedAmount(-1d), PayReceive.PAY);
    assertEquals(PayReceive.ofSignedAmount(-0d), PayReceive.PAY);
    assertEquals(PayReceive.ofSignedAmount(0d), PayReceive.RECEIVE);
    assertEquals(PayReceive.ofSignedAmount(+0d), PayReceive.RECEIVE);
    assertEquals(PayReceive.ofSignedAmount(1d), PayReceive.RECEIVE);
  }

  //-------------------------------------------------------------------------
  public void test_normalize_pay_double() {
    assertEquals(PayReceive.PAY.normalize(1d), -1d, 0d);
    assertEquals(PayReceive.PAY.normalize(0d), 0d, 0d);
    assertEquals(PayReceive.PAY.normalize(-0d), 0d, 0d);
    assertEquals(PayReceive.PAY.normalize(-1d), -1d, 0d);
  }

  public void test_normalize_pay_amount() {
    assertEquals(PayReceive.PAY.normalize(CurrencyAmount.of(GBP, 1d)), CurrencyAmount.of(GBP, -1d));
    assertEquals(PayReceive.PAY.normalize(CurrencyAmount.of(GBP, 0d)), CurrencyAmount.of(GBP, 0d));
    assertEquals(PayReceive.PAY.normalize(CurrencyAmount.of(GBP, -1d)), CurrencyAmount.of(GBP, -1d));
  }

  public void test_normalize_receive_double() {
    assertEquals(PayReceive.RECEIVE.normalize(1d), 1d, 0d);
    assertEquals(PayReceive.RECEIVE.normalize(0d), 0d, 0d);
    assertEquals(PayReceive.RECEIVE.normalize(-0d), 0d, 0d);
    assertEquals(PayReceive.RECEIVE.normalize(-1d), 1d, 0d);
  }

  public void test_normalize_receive_amount() {
    assertEquals(PayReceive.RECEIVE.normalize(CurrencyAmount.of(GBP, 1d)), CurrencyAmount.of(GBP, 1d));
    assertEquals(PayReceive.RECEIVE.normalize(CurrencyAmount.of(GBP, 0d)), CurrencyAmount.of(GBP, 0d));
    assertEquals(PayReceive.RECEIVE.normalize(CurrencyAmount.of(GBP, -1d)), CurrencyAmount.of(GBP, 1d));
  }

  public void test_isPay() {
    assertEquals(PayReceive.PAY.isPay(), true);
    assertEquals(PayReceive.RECEIVE.isPay(), false);
  }

  public void test_isReceive() {
    assertEquals(PayReceive.PAY.isReceive(), false);
    assertEquals(PayReceive.RECEIVE.isReceive(), true);
  }

  public void test_opposite() {
    assertEquals(PayReceive.PAY.opposite(), PayReceive.RECEIVE);
    assertEquals(PayReceive.RECEIVE.opposite(), PayReceive.PAY);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
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

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(PayReceive convention, String name) {
    assertEquals(PayReceive.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(PayReceive convention, String name) {
    assertEquals(PayReceive.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PayReceive.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PayReceive.of(null));
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
