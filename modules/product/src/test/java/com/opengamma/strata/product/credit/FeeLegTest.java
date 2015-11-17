/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Payment;

/**
 * Test.
 */
@Test
public class FeeLegTest {

  public void test_of() {
    FeeLeg expected = FeeLeg.builder()
        .upfrontFee(Payment.of(USD, 1_000_000d, date(2014, 3, 23)))
        .periodicPayments(PeriodicPaymentsTest.sut())
        .build();
    assertEquals(sut(), expected);
  }

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> FeeLeg.builder().build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static FeeLeg sut() {
    return FeeLeg.of(Payment.of(USD, 1_000_000d, date(2014, 3, 23)), PeriodicPaymentsTest.sut());
  }

  static FeeLeg sut2() {
    return FeeLeg.of(Payment.of(USD, 2_000_000d, date(2014, 3, 23)), PeriodicPaymentsTest.sut2());
  }

}
