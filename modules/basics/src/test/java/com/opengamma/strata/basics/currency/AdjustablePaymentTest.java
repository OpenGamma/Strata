/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test {@link AdjustablePayment}.
 */
public class AdjustablePaymentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_600);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final AdjustableDate DATE_2015_06_28_ADJ =
      AdjustableDate.of(date(2015, 6, 28), BusinessDayAdjustment.of(FOLLOWING, GBLO));
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final AdjustableDate DATE_2015_06_30_FIX = AdjustableDate.of(date(2015, 6, 30));

  //-------------------------------------------------------------------------
  @Test
  public void test_of_3argsFixed() {
    AdjustablePayment test = AdjustablePayment.of(GBP, 1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30_FIX);
  }

  @Test
  public void test_of_3argsAdjustable() {
    AdjustablePayment test = AdjustablePayment.of(GBP, 1000, DATE_2015_06_28_ADJ);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_28_ADJ);
  }

  @Test
  public void test_of_2argsFixed() {
    AdjustablePayment test = AdjustablePayment.of(GBP_P1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30_FIX);
  }

  @Test
  public void test_of_2argsAdjustable() {
    AdjustablePayment test = AdjustablePayment.of(GBP_P1000, DATE_2015_06_28_ADJ);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_28_ADJ);
  }

  @Test
  public void test_ofPayFixed() {
    AdjustablePayment test = AdjustablePayment.ofPay(GBP_P1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_M1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(-1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30_FIX);
  }

  @Test
  public void test_ofPayAdjustable() {
    AdjustablePayment test = AdjustablePayment.ofPay(GBP_P1000, DATE_2015_06_28_ADJ);
    assertThat(test.getValue()).isEqualTo(GBP_M1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(-1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_28_ADJ);
  }

  @Test
  public void test_ofReceiveFixed() {
    AdjustablePayment test = AdjustablePayment.ofReceive(GBP_P1000, DATE_2015_06_30);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_30_FIX);
  }

  @Test
  public void test_ofReceiveAdjustable() {
    AdjustablePayment test = AdjustablePayment.ofReceive(GBP_P1000, DATE_2015_06_28_ADJ);
    assertThat(test.getValue()).isEqualTo(GBP_P1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(1_000);
    assertThat(test.getDate()).isEqualTo(DATE_2015_06_28_ADJ);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    AdjustablePayment test = AdjustablePayment.ofReceive(GBP_P1000, DATE_2015_06_28_ADJ);
    assertThat(test.resolve(REF_DATA)).isEqualTo(Payment.of(GBP_P1000, DATE_2015_06_29));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negated() {
    AdjustablePayment test = AdjustablePayment.ofReceive(GBP_P1000, DATE_2015_06_30);
    assertThat(test.negated()).isEqualTo(AdjustablePayment.of(GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    AdjustablePayment test = AdjustablePayment.of(GBP_P1000, DATE_2015_06_30);
    coverImmutableBean(test);
    AdjustablePayment test2 = AdjustablePayment.of(EUR_P1600, DATE_2015_06_28_ADJ);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    AdjustablePayment test = AdjustablePayment.of(GBP_P1000, DATE_2015_06_30);
    assertSerialization(test);
  }

}
