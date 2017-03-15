/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.payment;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test {@link ResolvedBulletPayment}.
 */
@Test
public class ResolvedBulletPaymentTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final Payment PAYMENT1 = Payment.of(GBP_P1000, DATE_2015_06_30);
  private static final Payment PAYMENT2 = Payment.of(GBP_M1000, DATE_2015_06_30);

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedBulletPayment test = ResolvedBulletPayment.of(PAYMENT1);
    assertEquals(test.getPayment(), PAYMENT1);
    assertEquals(test.getCurrency(), PAYMENT1.getCurrency());
  }

  public void test_builder() {
    ResolvedBulletPayment test = ResolvedBulletPayment.builder().payment(PAYMENT1).build();
    assertEquals(test.getPayment(), PAYMENT1);
    assertEquals(test.getCurrency(), PAYMENT1.getCurrency());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedBulletPayment test = ResolvedBulletPayment.of(PAYMENT1);
    coverImmutableBean(test);
    ResolvedBulletPayment test2 = ResolvedBulletPayment.of(PAYMENT2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedBulletPayment test = ResolvedBulletPayment.of(PAYMENT1);
    assertSerialization(test);
  }

}
