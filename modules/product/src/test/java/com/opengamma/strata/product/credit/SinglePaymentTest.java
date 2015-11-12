/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test.
 */
@Test
public class SinglePaymentTest {

  public void test_of() {
    SinglePayment expected = SinglePayment.builder()
        .fixedAmount(CurrencyAmount.of(USD, 1_000_000d))
        .paymentDate(date(2014, 3, 23))
        .build();
    assertEquals(sut(), expected);
  }

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> SinglePayment.builder().build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static SinglePayment sut() {
    return SinglePayment.of(USD, 1_000_000d, date(2014, 3, 23));
  }

}
