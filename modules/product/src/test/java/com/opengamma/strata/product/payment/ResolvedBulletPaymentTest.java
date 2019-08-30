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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test {@link ResolvedBulletPayment}.
 */
public class ResolvedBulletPaymentTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final Payment PAYMENT1 = Payment.of(GBP_P1000, DATE_2015_06_30);
  private static final Payment PAYMENT2 = Payment.of(GBP_M1000, DATE_2015_06_30);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ResolvedBulletPayment test = ResolvedBulletPayment.of(PAYMENT1);
    assertThat(test.getPayment()).isEqualTo(PAYMENT1);
    assertThat(test.getCurrency()).isEqualTo(PAYMENT1.getCurrency());
  }

  @Test
  public void test_builder() {
    ResolvedBulletPayment test = ResolvedBulletPayment.builder().payment(PAYMENT1).build();
    assertThat(test.getPayment()).isEqualTo(PAYMENT1);
    assertThat(test.getCurrency()).isEqualTo(PAYMENT1.getCurrency());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedBulletPayment test = ResolvedBulletPayment.of(PAYMENT1);
    coverImmutableBean(test);
    ResolvedBulletPayment test2 = ResolvedBulletPayment.of(PAYMENT2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedBulletPayment test = ResolvedBulletPayment.of(PAYMENT1);
    assertSerialization(test);
  }

}
