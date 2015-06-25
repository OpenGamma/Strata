/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import org.testng.annotations.Test;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

/**
 * Test.
 */
@Test
public class FeeLegTest {

  public void test_of() {
    FeeLeg expected = FeeLeg.builder()
        .upfrontFee(SinglePaymentTest.sut())
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
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  public static FeeLeg sut() {
    return FeeLeg.of(SinglePaymentTest.sut(), PeriodicPaymentsTest.sut());
  }
}
