/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.amount.CashFlow;

/**
 * Test {@link CashFlow}.
 */
@Test
public class CashFlowTest {

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 5, 21);
  private static final double FUTURE_VALUE = 0.0232;
  private static final double DISCOUNT_FACTOR = 0.95;

  //-------------------------------------------------------------------------
  public void test_of_CurrencyAmount() {
    CurrencyAmount currencyAmount = CurrencyAmount.of(GBP, FUTURE_VALUE);
    CashFlow test = CashFlow.of(PAYMENT_DATE, currencyAmount, DISCOUNT_FACTOR);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getFutureValue(), currencyAmount);
    assertEquals(test.getDiscountFactor(), DISCOUNT_FACTOR);
  }

  public void test_of_Currency() {
    CashFlow test = CashFlow.of(PAYMENT_DATE, USD, FUTURE_VALUE, DISCOUNT_FACTOR);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getFutureValue(), CurrencyAmount.of(USD, FUTURE_VALUE));
    assertEquals(test.getDiscountFactor(), DISCOUNT_FACTOR);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CashFlow test1 = CashFlow.of(PAYMENT_DATE, USD, FUTURE_VALUE, DISCOUNT_FACTOR);
    coverImmutableBean(test1);
    CashFlow test2 = CashFlow.of(LocalDate.of(2015, 7, 11), GBP, 0.24, 0.987);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CashFlow test = CashFlow.of(PAYMENT_DATE, USD, FUTURE_VALUE, DISCOUNT_FACTOR);
    assertSerialization(test);
  }

}
