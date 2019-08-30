/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test.
 */
public class NotionalExchangeTest {

  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final CurrencyAmount GBP_1000 = CurrencyAmount.of(GBP, 1000d);

  @Test
  public void test_of() {
    NotionalExchange test = NotionalExchange.of(GBP_1000, DATE_2014_06_30);
    assertThat(test.getPayment()).isEqualTo(Payment.of(GBP_1000, DATE_2014_06_30));
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_06_30);
    assertThat(test.getPaymentAmount()).isEqualTo(GBP_1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_of_Payment() {
    NotionalExchange test = NotionalExchange.of(Payment.of(GBP_1000, DATE_2014_06_30));
    assertThat(test.getPayment()).isEqualTo(Payment.of(GBP_1000, DATE_2014_06_30));
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_06_30);
    assertThat(test.getPaymentAmount()).isEqualTo(GBP_1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_of_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalExchange.of(GBP_1000, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalExchange.of(null, DATE_2014_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalExchange.of(null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_adjustPaymentDate() {
    NotionalExchange test = NotionalExchange.of(GBP_1000, DATE_2014_06_30);
    NotionalExchange expected = NotionalExchange.of(GBP_1000, DATE_2014_06_30.plusDays(2));
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0)))).isEqualTo(test);
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2)))).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    NotionalExchange test = NotionalExchange.of(GBP_1000, DATE_2014_06_30);
    coverImmutableBean(test);
    NotionalExchange test2 = NotionalExchange.of(CurrencyAmount.of(GBP, 200d), date(2014, 1, 15));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    NotionalExchange test = NotionalExchange.of(GBP_1000, DATE_2014_06_30);
    assertSerialization(test);
  }

}
