/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link CreditCouponPaymentPeriod}.
 */
public class CreditCouponPaymentPeriodTest {
  private static final double COUPON = 0.05;
  private static final double NOTIONAL = 1.0e9;
  private static final LocalDate START_DATE = LocalDate.of(2013, 12, 20);
  private static final LocalDate END_DATE = LocalDate.of(2014, 3, 20);
  private static final LocalDate EFF_START_DATE = LocalDate.of(2013, 12, 19);
  private static final LocalDate EFF_END_DATE = LocalDate.of(2014, 3, 19);
  private static final double YEAR_FRACTION = 0.256;

  @Test
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
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getEffectiveStartDate()).isEqualTo(EFF_START_DATE);
    assertThat(test.getEffectiveEndDate()).isEqualTo(EFF_END_DATE);
    assertThat(test.getStartDate()).isEqualTo(START_DATE);
    assertThat(test.getEndDate()).isEqualTo(END_DATE);
    assertThat(test.getFixedRate()).isEqualTo(COUPON);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getPaymentDate()).isEqualTo(END_DATE);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
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
    assertThat(test.contains(START_DATE)).isTrue();
    assertThat(test.contains(START_DATE.plusMonths(1))).isTrue();
    assertThat(test.contains(END_DATE)).isFalse();
    assertThat(test.contains(START_DATE.minusDays(1))).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
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
