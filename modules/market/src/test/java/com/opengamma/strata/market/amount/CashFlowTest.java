/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.basics.BasicProjectAssertions.assertThat;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link CashFlow}.
 */
@Test
public class CashFlowTest {

  private static final Offset<Double> TOLERANCE = Assertions.offset(1e-8);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 5, 21);
  private static final double FORECAST_VALUE = 31245.65;
  private static final double DISCOUNT_FACTOR = 0.95;
  private static final double PRESENT_VALUE = FORECAST_VALUE * DISCOUNT_FACTOR;
  private static final CurrencyAmount FUTURE_AMOUNT = CurrencyAmount.of(GBP, FORECAST_VALUE);
  private static final CurrencyAmount PRESENT_AMOUNT = CurrencyAmount.of(GBP, PRESENT_VALUE);

  //-------------------------------------------------------------------------
  public void test_ofPresentValue_CurrencyAmount() {
    CashFlow test = CashFlow.ofPresentValue(PAYMENT_DATE, PRESENT_AMOUNT, DISCOUNT_FACTOR);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getPresentValue()).hasCurrency(GBP);
    assertThat(test.getPresentValue()).hasAmount(PRESENT_VALUE, TOLERANCE);
    assertThat(test.getForecastValue()).hasCurrency(GBP);
    assertThat(test.getForecastValue()).hasAmount(FORECAST_VALUE, TOLERANCE);
    assertThat(test.getDiscountFactor()).isCloseTo(DISCOUNT_FACTOR, TOLERANCE);
  }

  public void test_ofPresentValue_Currency() {
    CashFlow test = CashFlow.ofPresentValue(PAYMENT_DATE, GBP, PRESENT_VALUE, DISCOUNT_FACTOR);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getPresentValue()).hasCurrency(GBP);
    assertThat(test.getPresentValue()).hasAmount(PRESENT_VALUE, TOLERANCE);
    assertThat(test.getForecastValue()).hasCurrency(GBP);
    assertThat(test.getForecastValue()).hasAmount(FORECAST_VALUE, TOLERANCE);
    assertThat(test.getDiscountFactor()).isCloseTo(DISCOUNT_FACTOR, TOLERANCE);
  }

  public void test_ofForecastValue_CurrencyAmount() {
    CashFlow test = CashFlow.ofForecastValue(PAYMENT_DATE, FUTURE_AMOUNT, DISCOUNT_FACTOR);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getPresentValue()).hasCurrency(GBP);
    assertThat(test.getPresentValue()).hasAmount(PRESENT_VALUE, TOLERANCE);
    assertThat(test.getForecastValue()).hasCurrency(GBP);
    assertThat(test.getForecastValue()).hasAmount(FORECAST_VALUE, TOLERANCE);
    assertThat(test.getDiscountFactor()).isCloseTo(DISCOUNT_FACTOR, TOLERANCE);
  }

  public void test_ofForecastValue_Currency() {
    CashFlow test = CashFlow.ofForecastValue(PAYMENT_DATE, GBP, FORECAST_VALUE, DISCOUNT_FACTOR);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getPresentValue()).hasCurrency(GBP);
    assertThat(test.getPresentValue()).hasAmount(PRESENT_VALUE, TOLERANCE);
    assertThat(test.getForecastValue()).hasCurrency(GBP);
    assertThat(test.getForecastValue()).hasAmount(FORECAST_VALUE, TOLERANCE);
    assertThat(test.getDiscountFactor()).isCloseTo(DISCOUNT_FACTOR, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CashFlow test1 = CashFlow.ofForecastValue(PAYMENT_DATE, USD, FORECAST_VALUE, DISCOUNT_FACTOR);
    coverImmutableBean(test1);
    CashFlow test2 = CashFlow.ofForecastValue(LocalDate.of(2015, 7, 11), GBP, 0.24, 0.987);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CashFlow test = CashFlow.ofForecastValue(PAYMENT_DATE, USD, FORECAST_VALUE, DISCOUNT_FACTOR);
    assertSerialization(test);
  }

}
