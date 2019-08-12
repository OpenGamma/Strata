/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Payment}.
 */
public class PaymentTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_600);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_3args() {
    Payment test = Payment.of(GBP, 1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30);
  }

  @Test
  public void test_of_2args() {
    Payment test = Payment.of(GBP_P1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30);
  }

  @Test
  public void test_ofPay() {
    Payment test = Payment.ofPay(GBP_P1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_M1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(-1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30);
  }

  @Test
  public void test_ofReceive() {
    Payment test = Payment.ofReceive(GBP_P1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30);
  }

  @Test
  public void test_builder() {
    Payment test = Payment.builder()
        .value(GBP_P1000)
        .date(DATE_2015_06_30)
        .build();
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_adjustDate() {
    Payment test = Payment.ofReceive(GBP_P1000, DATE_2015_06_29);
    Payment expected = Payment.of(GBP_P1000, DATE_2015_06_29.plusDays(1));
    assertThat(test.adjustDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(1)))).isEqualTo(expected);
  }

  @Test
  public void test_adjustDate_noChange() {
    Payment test = Payment.ofReceive(GBP_P1000, DATE_2015_06_29);
    assertThat(test.adjustDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(1).minusDays(1)))).isSameAs(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negated() {
    Payment test = Payment.ofReceive(GBP_P1000, DATE_2015_06_30);
    assertThat(test.negated()).isEqualTo(Payment.of(GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo_rateProvider() {
    Payment test = Payment.ofReceive(GBP_P1000, DATE_2015_06_30);
    FxRateProvider provider = (ccy1, ccy2) -> 1.6d;
    assertThat(test.convertedTo(EUR, provider)).isEqualTo(Payment.ofReceive(EUR_P1600, DATE_2015_06_30));
    assertThat(test.convertedTo(GBP, provider)).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Payment test = Payment.of(GBP_P1000, DATE_2015_06_30);
    coverImmutableBean(test);
    Payment test2 = Payment.of(EUR_P1600, DATE_2015_06_29);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    Payment test = Payment.of(GBP_P1000, DATE_2015_06_30);
    assertSerialization(test);
  }

}
