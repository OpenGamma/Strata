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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Test.
 */
@Test
public class PeriodicPaymentsTest {

  public void test_of() {
    PeriodicPayments expected = PeriodicPayments.builder()
        .notional(CurrencyAmount.of(Currency.USD, 100_000_000d))
        .coupon(0.00100)
        .dayCount(DayCounts.ACT_360)
        .paymentFrequency(Frequency.P3M)
        .stubConvention(StubConvention.SHORT_INITIAL)
        .rollConvention(RollConventions.DAY_20)
        .build();
    assertEquals(sut(), expected);
  }

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> PeriodicPayments.builder().build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static PeriodicPayments sut() {
    return PeriodicPayments.of(
        CurrencyAmount.of(Currency.USD, 100_000_000d),
        0.00100,
        DayCounts.ACT_360,
        Frequency.P3M,
        StubConvention.SHORT_INITIAL,
        RollConventions.DAY_20);
  }

}
