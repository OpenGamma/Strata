/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.payment;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.product.common.PayReceive;

/**
 * Test {@link BulletPayment}.
 */
@Test
public class BulletPaymentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  public void test_builder() {
    BulletPayment test = BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(GBP_P1000)
        .date(AdjustableDate.of(DATE_2015_06_30))
        .build();
    assertEquals(test.getPayReceive(), PayReceive.PAY);
    assertEquals(test.getValue(), GBP_P1000);
    assertEquals(test.getDate(), AdjustableDate.of(DATE_2015_06_30));
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_builder_notNegative() {
    assertThrowsIllegalArg(() -> BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(GBP_M1000)
        .date(AdjustableDate.of(DATE_2015_06_30))
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve_pay() {
    BulletPayment test = BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(GBP_P1000)
        .date(AdjustableDate.of(DATE_2015_06_30))
        .build();
    ResolvedBulletPayment expected = ResolvedBulletPayment.of(Payment.of(GBP_M1000, DATE_2015_06_30));
    assertEquals(test.resolve(REF_DATA), expected);
  }

  public void test_resolve_receive() {
    BulletPayment test = BulletPayment.builder()
        .payReceive(PayReceive.RECEIVE)
        .value(GBP_P1000)
        .date(AdjustableDate.of(DATE_2015_06_30))
        .build();
    ResolvedBulletPayment expected = ResolvedBulletPayment.of(Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BulletPayment test = BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(GBP_P1000)
        .date(AdjustableDate.of(DATE_2015_06_30))
        .build();
    coverImmutableBean(test);
    BulletPayment test2 = BulletPayment.builder()
        .payReceive(PayReceive.RECEIVE)
        .value(USD_P1600)
        .date(AdjustableDate.of(DATE_2015_06_29))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    BulletPayment test = BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(GBP_P1000)
        .date(AdjustableDate.of(DATE_2015_06_30))
        .build();
    assertSerialization(test);
  }

}
