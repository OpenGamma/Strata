/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link CreditCouponPaymentPeriod}.
 */
@Test
public class CreditCouponPaymentPeriodTest {
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2014, 3, 20);
  private static final LocalDate EFF_START_DATE = LocalDate.of(2013, 12, 19);
  private static final LocalDate EFF_END_DATE = LocalDate.of(2014, 3, 19);
  private static final double YEAR_FRACTION = 0.256;

  public void test_builder() {
    CreditCouponPaymentPeriod test = CreditCouponPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .effectiveStartDate(EFF_START_DATE)
        .effectiveEndDate(EFF_END_DATE)
        .paymentDate(END_DATE)
        .fixedRate(COUPON)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getEffectiveStartDate(), EFF_START_DATE);
    assertEquals(test.getEffectiveEndDate(), EFF_END_DATE);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getFixedRate(), COUPON);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getPaymentDate(), END_DATE);
    assertEquals(test.getYearFraction(), YEAR_FRACTION);
  }

  public void test_contains() {
    CreditCouponPaymentPeriod test = CreditCouponPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .effectiveStartDate(EFF_START_DATE)
        .effectiveEndDate(EFF_END_DATE)
        .paymentDate(END_DATE)
        .fixedRate(COUPON)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertTrue(test.contains(START_DATE));
    assertTrue(test.contains(START_DATE.plusMonths(1)));
    assertFalse(test.contains(END_DATE));
    assertFalse(test.contains(START_DATE.minusDays(1)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CreditCouponPaymentPeriod test1 = CreditCouponPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .effectiveStartDate(EFF_START_DATE)
        .effectiveEndDate(EFF_END_DATE)
        .paymentDate(END_DATE)
        .fixedRate(COUPON)
        .yearFraction(YEAR_FRACTION)
        .build();
    coverImmutableBean(test1);
    CreditCouponPaymentPeriod test2 = CreditCouponPaymentPeriod.builder()
        .currency(Currency.JPY)
        .notional(5.0e6)
        .startDate(START_DATE.minusDays(7))
        .endDate(END_DATE.minusDays(7))
        .effectiveStartDate(EFF_START_DATE.minusDays(7))
        .effectiveEndDate(EFF_END_DATE.minusDays(7))
        .paymentDate(END_DATE.minusDays(7))
        .fixedRate(0.01)
        .yearFraction(0.25)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CreditCouponPaymentPeriod test = CreditCouponPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .effectiveStartDate(EFF_START_DATE)
        .effectiveEndDate(EFF_END_DATE)
        .paymentDate(END_DATE)
        .fixedRate(COUPON)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertSerialization(test);
  }

}
