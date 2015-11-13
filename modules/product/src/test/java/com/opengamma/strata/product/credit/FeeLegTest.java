/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

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

  static FeeLeg sut() {
    return FeeLeg.of(SinglePaymentTest.sut(), PeriodicPaymentsTest.sut());
  }

}
